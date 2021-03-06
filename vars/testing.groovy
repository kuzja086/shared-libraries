import io.libs.SqlUtils
import io.libs.ProjectHelpers
import io.libs.Utils

def call(Map buildEnv){
    pipeline {
        agent {
            label getParametrValue(buildEnv, 'agent')
        }

        post { // Выполняется после сборки
            always {
                script {
                    if (currentBuild.result == "ABORTED") {
                        return
                    }

                    dir ('build/out/allure') {
                        writeFile file:'environment.properties', text:"Build=${env.BUILD_URL}"
                    }

                    allure includeProperties: false, jdk: '', results: [[path: 'build/out/allure']]
                
                }
            }            
        }
        //     // Варианты в документации
        //     failure {
        //        sendEmailMessage("Failed", getParametrValue(buildEnv, 'emailForNotification')) // Научиться отправлять почту и добавить условие истина
        //     }
        // }
        options {
            timeout(time: 8, unit: 'HOURS') 
        }
        environment {
            // Заполнить параметры для пайплайна
            // TODO Добавить обязательные или нет с комментариями
            def CURRENT_CATALOG  = pwd()
            def server1c = getParametrValue(buildEnv, 'server1c')
            def server1cPort = getParametrValue(buildEnv, 'server1cPort')
            def agent1cPort = getParametrValue(buildEnv, 'agent1cPort') 
            def platform1c = getParametrValue(buildEnv, 'platform1C') 
            def serverSql = getParametrValue(buildEnv, 'serverSql')
            def templatebases = getParametrValue(buildEnv, 'templatebases') // *Обязательный
            def storages1cPath = getParametrValue(buildEnv, 'storages1cPath')
            def tempCatalog = getParametrValue(buildEnv, 'tempCatalog')   
            def base1CCredentialID = getParametrValue(buildEnv, 'base1CCredentialID')
            def storages1cCredentalsID = getParametrValue(buildEnv, 'storages1cCredentalsID')
            def sqlCredentialsID = getParametrValue(buildEnv, 'sqlCredentialsID')
            def serverCopyPath = getParametrValue(buildEnv, 'serverCopyPath') // * Обязательный
            def tempCatalpgOtherDisc = getParametrValue(buildEnv, 'tempCatalpgOtherDisc')
            def debugger = getParametrValue(buildEnv, 'debugger')
            def projectNameEDT = getParametrValue(buildEnv, 'projectNameEDT')
            def runTesting = getParametrValue(buildEnv, 'runTesting')
            def runSonar = getParametrValue(buildEnv, 'runSonar')
            def memoryForJava = getParametrValue(buildEnv, 'memoryForJava')
            def toolsTargetDir = getParametrValue(buildEnv, 'toolsTargetDir')
            def edtVersion      = getParametrValue(buildEnv, 'edtVersion')
            def testFeature = getParametrValue(buildEnv, 'testFeature')
            def cfPath = getParametrValue(buildEnv, 'cfPath')
            def executorPath = getParametrValue(buildEnv, 'executorPath')
        }

        stages{
            stage('Инициализация') {
                steps {
                    timestamps {
                        script {
                            def utils = new Utils()

                            utils.checkoutSCM(buildEnv)

                            templatebasesList = utils.lineToArray(templatebases.toLowerCase())
                                storages1cPathList = utils.lineToArray(storages1cPath.toLowerCase())
                                if (storages1cPathList.size() != 0) {
                                    assert storages1cPathList.size() == templatebasesList.size()
                                }
                            
                            testbase = null

                            coverageFile = "${tempCatalpgOtherDisc}\\coverage\\${projectNameEDT}\\genericCoverage.xml"
                            coverageFileOutput =  "${tempCatalpgOtherDisc}\\coverage\\${projectNameEDT}\\coveredLines.xml"
                            SRC = "./${projectNameEDT}/src"
                            
                            dir ('build') {
                                writeFile file:'dummy', text:''
                            }

                            projectName = "${CURRENT_CATALOG}\\${projectNameEDT}"

                            ib = "File=${tempCatalog}"
                        }
                    }
                }
            }
            stage("Запуск") {
                steps {
                    timestamps {
                        script {
                            utils = new Utils()
                            for (i = 0;  i < templatebasesList.size(); i++) {
                                stage("Подготовка к тестированию") {
                                    timestamps {
                                        if (runTesting.trim().equals("true")) {
                                            templateDb = templatebasesList[i]
                                            storage1cPath = storages1cPathList[i]
                                            testbase = "test_${templateDb}"
                                            templateDbConnString = utils.getConnectionString(buildEnv)
                                            testbaseConnString = utils.getConnectionString(buildEnv, testbase)
                                            backupPath = "${serverCopyPath}/temp_${templateDb}_${utils.currentDateStamp()}"

                                            // 1. Удаляем тестовую базу из кластера (если он там была) и очищаем клиентский кеш 1с
                                            dropDbTask(
                                                server1c, 
                                                server1cPort,
                                                testbase,
                                                base1CCredentialID
                                            )

                                            // 2. Обновляем Эталонную базу из хранилища 1С (если применимо)
                                            updateDbTask(
                                                platform1c,
                                                templateDb, 
                                                storage1cPath, 
                                                storages1cCredentalsID, 
                                                templateDbConnString, 
                                                base1CCredentialID
                                            )

                                            // 3. Делаем sql бекап эталонной базы, которую будем загружать в тестовую базу
                                            backupTask(
                                                serverSql, 
                                                templateDb, 
                                                backupPath,
                                                sqlCredentialsID
                                            )

                                            // 4. Загружаем sql бекап эталонной базы в тестовую
                                            restoreTask(
                                                serverSql, 
                                                testbase, 
                                                backupPath,
                                                sqlCredentialsID
                                            )

                                            // 5. Создаем тестовую базу кластере 1С
                                            createDbTask(
                                                "${server1c}:${agent1cPort}",
                                                serverSql,
                                                platform1c,
                                                testbase,
                                                sqlCredentialsID
                                            )
                                        }
                                    }
                                }
                                stage("Тестирование Vanessa"){
                                    if (runTesting.trim().equals("true")) {
                                        timestamps {
                                            // 6. Запускаем внешнюю обработку 1С, которая очищает базу от всплывающего окна с тем, что база перемещена при старте 1С
                                            runHandlers1cTask(
                                                testbase, 
                                                base1CCredentialID,
                                                testbaseConnString,
                                                coverageFile,
                                                debugger,
                                                runSonar
                                            )

                                            // 7. Тестирование Vanessa
                                            test1C(
                                                platform1c,
                                                base1CCredentialID,
                                                testbaseConnString,
                                                "${server1c}:${agent1cPort}",
                                                testbase,
                                                testFeature,
                                                runSonar
                                            )
                                        }
                                    }    
                                }       
                            }
                            // TODO Разобраться что это и доделать
                            // parallel dropDbTasks 
						    //parallel updateDbTasks
                        // parallel backupTasks
                        //                         parallel restoreTasks
                        //                         parallel createDbTasks
                        //                         parallel runHandlers1cTasks
                        }
                    }
                }
            }
        } 
    }
}


def call(){
    call([:])
}

def dropDbTask(server1c, server1cPort, infobase, base1CCredentialID) {
    timestamps {
        def projectHelpers = new ProjectHelpers()
        projectHelpers.dropDb(server1c, server1cPort, infobase, base1CCredentialID, true)
    }
}

def updateDbTask(platform1c, infobase, storage1cPath, storages1cCredentalsID, connString, base1CCredentialID) {
    // stage("Загрузка из хранилища ${infobase}") {
        timestamps {
            prHelpers = new ProjectHelpers()
            if (storage1cPath.trim().equals("null")) {
                return
            }
            
            prHelpers.loadCfgFrom1CStorage(storage1cPath, storages1cCredentalsID, connString, base1CCredentialID)
            prHelpers.updateInfobase(connString, base1CCredentialID, platform1c)
        }
    // }
}

def backupTask(serverSql, infobase, backupPath, sqlCredentialsID) {
    // stage("sql бекап ${infobase}") {
        timestamps {
            def sqlUtils = new SqlUtils()

            sqlUtils.checkDb(serverSql, infobase, sqlCredentialsID)
            sqlUtils.backupDb(serverSql, infobase, backupPath, sqlCredentialsID)
        }
    // }
}

def restoreTask(serverSql, infobase, backupPath, sqlCredentialsID) {
    timestamps {
        sqlUtils = new SqlUtils()
        utils = new Utils()

        sqlUtils.createEmptyDb(serverSql, infobase, sqlCredentialsID)
        sqlUtils.restoreDb(serverSql, infobase, backupPath, sqlCredentialsID)
        sqlUtils.clearBackups(backupPath)
    }
}

def createDbTask(server1c, serverSql, platform1c, infobase, sqlCredentialsID) {
    // stage("Создание базы ${infobase}") {
        timestamps {
            def projectHelpers = new ProjectHelpers()
            try {
                projectHelpers.createDb(platform1c, server1c, serversql, sqlCredentialsID, infobase, null, false)
            } catch (excp) {
                echo "Error happened when creating base ${infobase}. Probably base already exists in the ibases.v8i list. Skip the error"
            }
        }
    // }
}

def runHandlers1cTask(infobase, base1CCredentialID, testbaseConnString, coverageFile, debugger, runSonar) {
    // stage("Запуск 1с обработки на ${infobase}") {
        timestamps {
            // TODO Запуск начала замеров покрытия
            // coverage-cli start --infobase test_pb_test --output C:\temp\coverage.csv --debugger http://192.168.0.112:2450
            def projectHelpers = new ProjectHelpers()
            def utils = new Utils()

            if (runSonar.trim().equals("true")){
                utils.cmd("""
                 coverage-cli start --infobase \"${infobase}\" --output \"${coverageFile}\" --debugger \"${debugger}\"  
                """)
            }
            projectHelpers.unlocking1cBase(testbaseConnString, base1CCredentialID)
        }
    // }
}

def test1C(platform1c, base1CCredentialID, testbaseConnString, server1c, testbase, testFeature, runSonar){
    // stage("Тестирование Vanessa") {
        timestamps {
            def projectHelpers = new ProjectHelpers()
            def utils = new Utils()
            
            projectHelpers.test1C(platform1c, base1CCredentialID, testbaseConnString, server1c, testbase, testFeature)        
            
            if (runSonar.trim().equals("true")){
                utils.cmd("""
                  coverage-cli stop 
                  coverage-cli convert --input \"${coverageFile}\" --output \"${coverageFileOutput}\" --sources \"${SRC}\" --format EDT  
                """)
            }
            // TODO Остановка замеров и покрытия и их конвертация
            // coverage-cli stop 
            // Конвертацию сделать в Pipeline sonar
            // coverage-cli convert --input C:\temp\coverage.csv --output C:\temp\coveredLines.xml --sources D:\1c\workspace\pb_sonar\pb\src --format EDT

        }
    // }
    //TODO Сделать дымовые и другие тесты
}