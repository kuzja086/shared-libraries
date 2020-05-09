def backupTasks = [:]
def restoreTasks = [:]
def dropDbTasks = [:]
def createDbTasks = [:]
def runHandlers1cTasks = [:]
def updateDbTasks = [:]


def call(Map buildEnv){
    pipeline {
        agent {
            label getParametrValue(buildEnv, 'agent')
        }

        // post { // Выполняется после сборки
        //     // always {
        //     //     script {
        //     //         if (currentBuild.result == "ABORTED") {
        //     //             return
        //     //         }

        //     //         dir ('build/out/allure') {
        //     //             writeFile file:'environment.properties', text:"Build=${env.BUILD_URL}"
        //     //         }

        //     //         allure includeProperties: false, jdk: '', results: [[path: 'build/out/allure']]
        //     //     }
        //     // }
        //     // Варианты в документации
        //     failure {
        //        sendEmailMessage("Failed", buildEnv.emailForNotification) // Научиться отправлять почту и добавить условие истина
        //     }
        // }

        environment {
            // Заполнить параметры для пайплайна
            // TODO Добавить обязательные или нет с комментариями
            def CURRENT_CATALOG  = pwd()
            def server1c = getParametrValue(buildEnv, 'server1c')
            def server1cPort = getParametrValue(buildEnv, 'server1c')
            def agent1cPort = getParametrValue(buildEnv, 'agent1cPort') 
            def platform1c = getParametrValue(buildEnv, 'platform1C') 
            def serverSql = getParametrValue(buildEnv, 'serverSql')
            def templatebases = getParametrValue(buildEnv, 'templatebases') // *Обязательный
            def storages1cPath = getParametrValue(buildEnv, 'storages1cPath')
            def tempCatalog = getParametrValue(buildEnv, 'tempCatalog')   
            def base1CCredentialID = getParametrValue(buildEnv, 'base1CCredential_ID')
            def storages1cCredentalsID = getParametrValue(buildEnv, 'storages1cCredentalsID')
            def sqlCredentialsID = getParametrValue(buildEnv, 'sqlCredentialsID')
            def serverCopyPath = getParametrValue(buildEnv, 'serverCopyPath') // * Обязательный
        }

        stages{
            stage('Инициализация') {
                steps {
                    timestamps {
                        script {
                            templatebasesList = utils.lineToArray(templatebases.toLowerCase())
                            if (storages1cPath != null && !storages1cPath.isEmpty()){
                                storages1cPathList = utils.lineToArray(storages1cPath.toLowerCase())
                            }
                            

                            if (storages1cPathList.size() != 0) {
                                assert storages1cPathList.size() == templatebasesList.size()
                            }

                            testbase = null

                            dir ('build') {
                                writeFile file:'dummy', text:''
                            }
                        }
                    }
                }
            }
            stage("Запуск") {
                steps {
                    timestamps {
                        script {
                            for (i = 0;  i < templatebasesList.size(); i++) {
                                templateDb = templatebasesList[i]
                                storage1cPath = storages1cPathList[i]
                                testbase = "test_${templateDb}"
                                templateDbConnString = getConnectionString(buildEnv)
                                testbaseConnString = getConnectionString(buildEnv, testbase)
                                backupPath = "${serverCopyPath}/temp_${templateDb}_${utils.currentDateStamp()}"

                                // 1. Удаляем тестовую базу из кластера (если он там была) и очищаем клиентский кеш 1с
                                dropDbTasks["dropDbTask_${testbase}"] = dropDbTask(
                                    server1c, 
                                    server1cPort, 
                                    serverSql, 
                                    testbase, 
                                    base1CCredentialID, 
                                    sqlCredentialsID
                                )
// 							// 2. Обновляем Эталонную базу из хранилища 1С (если применимо)
//                             updateDbTasks["updateTask_${templateDb}"] = updateDbTask(
//                                 platform1c,
//                                 templateDb, 
//                                 storage1cPath, 
//                                 storageUser, 
//                                 storagePwd, 
//                                 templateDbConnString, 
//                                 admin1cUser, 
//                                 admin1cPwd
//                             )
//                             // 3. Делаем sql бекап эталонной базы, которую будем загружать в тестовую базу
//                             backupTasks["backupTask_${templateDb}"] = backupTask(
//                                 serverSql, 
//                                 templateDb, 
//                                 backupPath,
//                                 sqlUser,
//                                 sqlPwd
//                             )
//                             // 4. Загружаем sql бекап эталонной базы в тестовую
//                             restoreTasks["restoreTask_${testbase}"] = restoreTask(
//                                 serverSql, 
//                                 testbase, 
//                                 backupPath,
//                                 sqlUser,
//                                 sqlPwd
//                             )
//                             // 5. Создаем тестовую базу кластере 1С
//                             createDbTasks["createDbTask_${testbase}"] = createDbTask(
//                                 "${server1c}:${agent1cPort}",
//                                 serverSql,
//                                 platform1c,
//                                 testbase,
// 								sqlUser,
//                                 sqlPwd
//                             )
//                             // 6. Запускаем внешнюю обработку 1С, которая очищает базу от всплывающего окна с тем, что база перемещена при старте 1С
//                             runHandlers1cTasks["runHandlers1cTask_${testbase}"] = runHandlers1cTask(
//                                 testbase, 
//                                 admin1cUser, 
//                                 admin1cPwd,
//                                 testbaseConnString
//                             )
//                         }

                        parallel dropDbTasks
// 						   parallel updateDbTasks
//                         parallel backupTasks
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


//         stage("Тестирование ADD") {
//             steps {
//                 timestamps {
//                     script {

//                         if (templatebasesList.size() == 0) {
//                             return
//                         }

//                         platform1cLine = ""
//                         if (platform1c != null && !platform1c.isEmpty()) {
//                             platform1cLine = "--v8version ${platform1c}"
//                         }

//                         admin1cUsrLine = ""
//                         if (admin1cUser != null && !admin1cUser.isEmpty()) {
//                             admin1cUsrLine = "--db-user ${admin1cUser}"
//                         }

//                         admin1cPwdLine = ""
//                         if (admin1cPwd != null && !admin1cPwd.isEmpty()) {
//                             admin1cPwdLine = "--db-pwd ${admin1cPwd}"
//                         }
//                         // Запускаем ADD тестирование на произвольной базе, сохранившейся в переменной testbaseConnString
//                         returnCode = utils.cmd("runner vanessa --settings tools/vrunner.json ${platform1cLine} --ibconnection \"${testbaseConnString}\" ${admin1cUsrLine} ${admin1cPwdLine} --pathvanessa tools/vanessa-automation/vanessa-automation.epf")

//                         if (returnCode != 0) {
//                             utils.raiseError("Возникла ошибка при запуске ADD на сервере ${server1c} и базе ${testbase}")
//                         }
//                     }
//                 }
//             }
//         }
//     }   
//     post {
//         always {
//             script {
//                 if (currentBuild.result == "ABORTED") {
//                     return
//                 }

//                 dir ('build/out/allure') {
//                     writeFile file:'environment.properties', text:"Build=${env.BUILD_URL}"
//                 }

//                 allure includeProperties: false, jdk: '', results: [[path: 'build/out/allure']]
//             }
//         }
//     }
// }


def dropDbTask(server1c, server1cPort, serverSql, infobase, base1CCredentialID, sqlCredentialsID) {
    return {
        timestamps {
            stage("Удаление ${infobase}") {
                projectHelpers.dropDb(server1c, server1cPort, serverSql, infobase, admin1cUser, admin1cPwd, sqluser, sqlPwd)
            }
        }
    }
}

// def createDbTask(server1c, serverSql, platform1c, infobase, sqlUser, sqlPwd) {
//     return {
//         stage("Создание базы ${infobase}") {
//             timestamps {
//                 def projectHelpers = new ProjectHelpers()
//                 try {
//                     projectHelpers.createDb(platform1c, server1c, serversql, sqlUser, sqlPwd, infobase, null, false)
//                 } catch (excp) {
//                     echo "Error happened when creating base ${infobase}. Probably base already exists in the ibases.v8i list. Skip the error"
//                 }
//             }
//         }
//     }
// }

// def backupTask(serverSql, infobase, backupPath, sqlUser, sqlPwd) {
//     return {
//         stage("sql бекап ${infobase}") {
//             timestamps {
//                 def sqlUtils = new SqlUtils()

//                 sqlUtils.checkDb(serverSql, infobase, sqlUser, sqlPwd)
//                 sqlUtils.backupDb(serverSql, infobase, backupPath, sqlUser, sqlPwd)
//             }
//         }
//     }
// }

// def restoreTask(serverSql, infobase, backupPath, sqlUser, sqlPwd) {
//     return {
//         stage("Востановление ${infobase} бекапа") {
//             timestamps {
//                 sqlUtils = new SqlUtils()

//                 sqlUtils.createEmptyDb(serverSql, infobase, sqlUser, sqlPwd)
//                 sqlUtils.restoreDb(serverSql, infobase, backupPath, sqlUser, sqlPwd)
				
// 				 utils = new Utils()

// 				returnCode = utils.cmd("oscript one_script_tools/deleteFile.os -file ${backupPath}")
// 				if (returnCode != 0) {
// 					utils.raiseError("Возникла ошибка при удалении файла ${backupPath}")
// 				}
//             }
//         }
//     }
// }

// def runHandlers1cTask(infobase, admin1cUser, admin1cPwd, testbaseConnString) {
//     return {
//         stage("Запуск 1с обработки на ${infobase}") {
//             timestamps {
//                 def projectHelpers = new ProjectHelpers()
//                 projectHelpers.unlocking1cBase(testbaseConnString, admin1cUser, admin1cPwd)
//             }
//         }
//     }
// }

// def updateDbTask(platform1c, infobase, storage1cPath, storageUser, storagePwd, connString, admin1cUser, admin1cPwd) {
//     return {
//         stage("Загрузка из хранилища ${infobase}") {
//             timestamps {
//                 prHelpers = new ProjectHelpers()

//                 if (storage1cPath == null || storage1cPath.isEmpty()) {
//                     return
//                 }

//                 prHelpers.loadCfgFrom1CStorage(storage1cPath, storageUser, storagePwd, connString, admin1cUser, admin1cPwd, platform1c)
//                 prHelpers.updateInfobase(connString, admin1cUser, admin1cPwd, platform1c)
//             }
//         }
//     }
// }