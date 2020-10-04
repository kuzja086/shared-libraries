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
            def oneAgent = getParametrValue(buildEnv, 'oneAgent')
            def testFeature = getParametrValue(buildEnv, 'testFeature')
            def xmlPath = getParametrValue(buildEnv, 'xmlPath')
            def cfPath = getParametrValue(buildEnv, 'cfPath')
            def catalog1c = getParametrValue(buildEnv, 'catalog1c')
            def makeDistrib = getParametrValue(buildEnv, 'makeDistrib')
        }

        stages{
            stage('Инициализация') {
                agent {
                    label 'FirstNode'
                }
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
                            
                            if (catalog1c == ''){
                                catalog1c = platform1C
                            }

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
                                                serverSql, 
                                                testbase,
                                                base1CCredentialID,
                                                sqlCredentialsID
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
            stage("Sonar Initialization"){
                agent {
                    label 'testserver'
                }
                steps {
                    timestamps {
                        script {
                            if (runSonar.trim().equals("true")){ 
                                CURRENT_CATALOG = pwd()
                                if (oneAgent.trim().equals("true")) {
                                    RESULT_CATALOG = "${CURRENT_CATALOG}\\sonar_result"
                                }
                                else {
                                    RESULT_CATALOG = "${tempCatalpgOtherDisc}\\sonar_result"
                                    toolsTargetDir = "${toolsTargetDir}\\tools"
                                }

                                def utils = new Utils()
                                utils.checkoutSCM(buildEnv)
                            
                                // создаем/очищаем временный каталог
                                dir(RESULT_CATALOG) {
                                deleteDir()
                                writeFile file: 'acc.json', text: '{"issues": []}'
                                writeFile file: 'bsl-generic-json.json', text: '{"issues": []}'
                                writeFile file: 'edt.json', text: '{"issues": []}'
                                }

                                GENERIC_ISSUE_JSON ="${RESULT_CATALOG}/acc.json,${RESULT_CATALOG}/bsl-generic-json.json,${RESULT_CATALOG}/edt.json"
                                SRC = "./${projectNameEDT}/src"

                                EDT_VALIDATION_RESULT = "${RESULT_CATALOG}\\edt-validation.csv"

                                perf_catalog = "${tempCatalpgOtherDisc}\\coverage\\${projectNameEDT}"
                            }
                        }
                    }
                }        
            }
            stage("Sonar Cheking"){
                agent {
                    label 'testserver'
                }
                steps {
                    timestamps {
                        script {
                            if (runSonar.trim().equals("true")) {           
                                edtCheck(EDT_VALIDATION_RESULT, edtVersion, tempCatalog, projectName) 
                                //АПК                                
                            }
                        } 
                    }
                }
            }
            stage("Подготовка результатов"){
                agent {
                    label 'FirstNode'
                }
                steps {
                    timestamps {
                        script {
                            if (runSonar.trim().equals("true")) {
                                convertResult(SRC, EDT_VALIDATION_RESULT, RESULT_CATALOG)
                                transformResult(toolsTargetDir, STEBI_SETTINGS, GENERIC_ISSUE_JSON, SRC)      
                            }
                        }
                    }
                }
            }
            stage("Sonar Scanner"){
                agent {
                    label 'testserver'
                }
                steps {
                    timestamps {
                        script {
                            if (runSonar.trim().equals("true")) {  
                                sonarScaner(SRC, memoryForJava, projectNameEDT, GENERIC_ISSUE_JSON)   
                            }
                        }
                    }
                }
            }
            stage("Making Distribution"){
                agent {
                    label 'testserver'
                }
                steps {
                    timestamps {
                        script {
                            if (makeDistrib.trim().equals("true")) {  
                                // TODO Передалить на норм  параметры
                                def xmlPath = getParametrValue(buildEnv, 'xmlPath')
                                def cfPath = getParametrValue(buildEnv, 'cfPath')
                                def tempCatalog = getParametrValue(buildEnv, 'tempCatalog')
                                initDistribFiles()
                                dumpProjectEDTInFiles(memoryForJava, edtVersion, tempCatalog, projectName, xmlPath)
                                loadConfigFromFiles(catalog1c, xmlPath, ib)
                                saveCF(cfPath, catalog1c, projectName, ib)
                                // TODO Расширения списком
                            }
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

def dropDbTask(server1c, server1cPort, serverSql, infobase, base1CCredentialID, sqlCredentialsID) {
    // stage("Удаление ${infobase}") {
        timestamps {
            def projectHelpers = new ProjectHelpers()
            projectHelpers.dropDb(server1c, server1cPort, serverSql, infobase, base1CCredentialID, sqlCredentialsID)
        }
    // }
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
    // stage("Востановление ${infobase} бекапа") {
            timestamps {
            sqlUtils = new SqlUtils()
            utils = new Utils()

            sqlUtils.createEmptyDb(serverSql, infobase, sqlCredentialsID)
            sqlUtils.restoreDb(serverSql, infobase, backupPath, sqlCredentialsID)
            sqlUtils.clearBackups(backupPath)
        }
    // }
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

def edtCheck(EDT_VALIDATION_RESULT, edtVersion, tempCatalog, projectName){
    timestamps{
        def utils = new Utils()
        if (fileExists("${EDT_VALIDATION_RESULT}")) {
            utils.cmd("@DEL \"${EDT_VALIDATION_RESULT}\"")
        }
        utils.cmd("""
        @set RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru
         ring edt@${edtVersion} workspace validate --workspace-location \"${tempCatalog}\" --file \"${EDT_VALIDATION_RESULT}\" --project-list \"${projectName}\"
        """)
    }
}

def convertResult(SRC, EDT_VALIDATION_RESULT, RESULT_CATALOG){
    timestamps {
        def utils = new Utils()
        if (oneAgent.trim().equals("true")) {
            utils.checkoutSCM(buildEnv)
        }

        utils.cmd("""
        set SRC=\"${SRC}\"
        stebi convert -e \"${EDT_VALIDATION_RESULT}\" \"${RESULT_CATALOG}/edt.json\" 
        """)
    }
}

def transformResult(toolsTargetDir, STEBI_SETTINGS, GENERIC_ISSUE_JSON, SRC){
    timestamps {
        STEBI_SETTINGS =  "${toolsTargetDir}/settings.json"
                            
        def utils = new Utils()
        utils.cmd("""
        set GENERIC_ISSUE_SETTINGS_JSON=\"${STEBI_SETTINGS}\"
        set GENERIC_ISSUE_JSON=${GENERIC_ISSUE_JSON}
        set SRC=${SRC}
        stebi transform -r=0
        """)
    }
}

def sonarScaner(SRC, memoryForJava, projectNameEDT, GENERIC_ISSUE_JSON){
    withSonarQubeEnv('Sonar') {
        def scanner_properties = "-Dsonar.projectVersion=%SONAR_PROJECTVERSION% -Dsonar.projectKey=${projectNameEDT} -Dsonar.sources=\"${SRC}\" -Dsonar.externalIssuesReportPaths=${GENERIC_ISSUE_JSON} -Dsonar.sourceEncoding=UTF-8 -Dsonar.inclusions=**/*.bsl"

        // if (!perf_catalog.isEmpty()) {
        //     scanner_properties = "${scanner_properties} -Dsonar.coverageReportPaths=\"${perf_catalog}\\genericCoverage.xml\""
        //

        def scannerHome = tool 'SonarQube Scanner';

        def utils = new Utils()

        utils.cmd("""
        @set SRC=\"${SRC}\"
        @echo %SRC%
        @set SONAR_SCANNER_OPTS=-Xmx${memoryForJava}g
        ${scannerHome}\\sonar-scanner\\bin\\sonar-scanner ${scanner_properties} -Dfile.encoding=UTF-8
        """)

        PROJECT_URL = "${env.SONAR_HOST_URL}/dashboard?id=${projectNameEDT}"
    }
}

def initDistribFiles(){
    timestamps {
        dir(tempCatalog) {
            deleteDir()
        }

        dir(xmlPath){
            deleteDir()
        }
    }
}

def dumpProjectEDTInFiles(memoryForJava, edtVersion, tempCatalog, projectName, xmlPath){
    timestamps{
        def utils = new Utils()
        // TODO Переделать на норм параметры.
        projectName = pwd()"\\${projectNameEDT}"
        utils.cmd("""
            @set RING_OPTS = -Dfile.encoding=UTF-8 -Dosgi.nl=ru
            @set RING_OPTS = -Xmx${memoryForJava}g
            ring edt@${edtVersion} workspace export --workspace-location ${tempCatalog} --project ${projectName} --configuration-files ${xmlPath}
            """)
    }    
}

def loadConfigFromFiles(catalog1c, xmlPath, ib){
    timestamps{
        def utils = new Utils()

        utils.cmd("""
        cd /D C:\\Program Files (x86)\\1cv8\\${catalog1c}\\bin\\
        1cv8.exe CREATEINFOBASE ${ib}
        1cv8.exe DESIGNER /WA- /DISABLESTARTUPDIALOGS /IBConnectionString ${ib} /LoadConfigFromFiles ${xmlPath} /UpdateDBCfg
        """)
    }
}

def saveCF(cfPath, catalog1c, projectName, ib){
    timestamps{
        def utils = new Utils()

        cfPath = "${cfPath}\\${projectName}.cf" 
        // Разовое решение, так нужно было.
        catalog1c = 'reliz_8.3.17.1496'
        
        utils.cmd("""
        cd /D C:\\Program Files (x86)\\1cv8\\${catalog1c}\\bin\\
        1cv8.exe DESIGNER /WA- /DISABLESTARTUPDIALOGS /IBConnectionString ${ib} /CreateDistributionFiles -cffile ${cfPath}
        """)
    }
}