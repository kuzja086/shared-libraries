def call(Map buildEnv){
    pipeline {
        agent {
            label getParametrValue(buildEnv, 'agent')
        }

        post { // Выполняется после сборки
            // Варианты в документации
            failure {
               sendEmailMessage("Failed", buildEnv.emailForNotification) // Научиться отправлять почту и добавить условие истина
            }
        }

        environment {
            // Заполнить параметры для пайплайна
            // TODO Добавить комментарий, Обязательный или НЕТ
            def CURRENT_CATALOG  = pwd()
            def EDT_VERSION      = getParametrValue(buildEnv, 'edtVersion')
        }

        options {
            timeout(time: 8, unit: 'HOURS')
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }

        stages{
            stage('Checkout') {
                steps {
                    timestamps {
                        script {
                            def utils = new Utils()

                            utils.checkoutSCM(buildEnv)
                        }
                    }
                }
            }
            //stage('АПК') {
           // steps {
           //     timestamps {
           //         script {
           //             def cmd_properties = "\"acc.propertiesPaths=${ACC_PROPERTIES};acc.catalog=${CURRENT_CATALOG};acc.sources=${SRC};acc.result=${TEMP_CATALOG}\\acc.json;acc.projectKey=${PROJECT_KEY};acc.check=${ACC_check};acc.recreateProject=${ACC_recreateProject}\""
           //             cmd("runner run --ibconnection /F${ACC_BASE} --db-user ${ACC_USER} --command ${cmd_properties} --execute \"${BIN_CATALOG}acc-export.epf\" --ordinaryapp=1")
           //         }
           //     }
            //}
            //}
        //     stage('EDT') {
        //     steps {
        //         timestamps {
        //             script {
        //                if (fileExists("${EDT_VALIDATION_RESULT}")) {
        //                     cmd("@DEL \"${EDT_VALIDATION_RESULT}\"")
        //                 }
        //                 cmd("""
        //                 @set RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru
        //                 ring edt@${EDT_VERSION} workspace validate --workspace-location \"${TEMP_CATALOG}\" --file \"${EDT_VALIDATION_RESULT}\" --project-list \"${PROJECT_NAME_EDT}\"
        //                 """)
        //            }
        //         }
        //     }
        // }
        stage('bsl-language-server') {
            steps {
                timestamps {
                    script {
                    cmd("java -Xmx8g -jar ${BIN_CATALOG}bsl-language-server.jar -a -s \"./Repo/${SRC}\" -r generic -c \"${BSL_LS_PROPERTIES}\" -o \"${TEMP_CATALOG}\"")
                    }
                }
            }
        }
        // stage('Конвертация результатов EDT') {
        //     steps {
        //         timestamps {
        //             script {
        //             dir('Repo') {
        //                 cmd("""
        //                 set SRC=\"${SRC}\"
        //                 stebi convert -e \"${EDT_VALIDATION_RESULT}\" \"${TEMP_CATALOG}/edt.json\" 
        //                 """)
        //             }
        //             }
        //         }
        //     }
        // }
        stage('Трансформация результатов') {
            steps {
                timestamps {
                    script {
                    cmd("""
                    set GENERIC_ISSUE_SETTINGS_JSON=\"${STEBI_SETTINGS}\"
                    set GENERIC_ISSUE_JSON=${GENERIC_ISSUE_JSON}
                    set SRC=\"./Repo/${SRC}\"

                    stebi transform -r=0
                    """)
                    }
                }
            }
        }
        // stage('Получение покрытия') {
        //     steps {
        //         timestamps {
        //             script {
        //              if (!perf_catalog.isEmpty()) {
        //                 dir('Repo') {
        //                     cmd("perf-measurements-to-cover c -i=${perf_catalog} -o=\"${TEMP_CATALOG}\\genericCoverage.xml\" -s=\"${SRC}\"")
        //                 }
        //             } else{
        //                 echo "skip"
        //             }
        //             }
        //         }
        //     }
        // }
        // stage('Сканер') {
        //     steps {
        //         timestamps {
        //             script {
        //             dir('Repo') {
        //             withSonarQubeEnv('Sonar') {
        //             def scanner_properties = "-X -Dsonar.projectVersion=%SONAR_PROJECTVERSION% -Dsonar.projectKey=${PROJECT_KEY} -Dsonar.sources=\"${SRC}\" -Dsonar.externalIssuesReportPaths=${GENERIC_ISSUE_JSON} -Dsonar.sourceEncoding=UTF-8 -Dsonar.inclusions=**/*.bsl -Dsonar.bsl.languageserver.enabled=false"
        //             if (!perf_catalog.isEmpty()) {
        //                 scanner_properties = "${scanner_properties} -Dsonar.coverageReportPaths=\"${TEMP_CATALOG}\\genericCoverage.xml\""
        //             }
        //             def scannerHome = tool 'SonarQube Scanner';
        //             cmd("""
        //             @set SRC=\"${SRC}\"
        //             @echo %SRC%
        //             @call stebi g > temp_SONAR_PROJECTVERSION
        //             @set /p SONAR_PROJECTVERSION=<temp_SONAR_PROJECTVERSION
        //             @DEL temp_SONAR_PROJECTVERSION
        //             @echo %SONAR_PROJECTVERSION%
        //             @set JAVA_HOME=${sonar_catalog}\\jdk\\
        //             @set SONAR_SCANNER_OPTS=-Xmx6g
        //             ${scannerHome}\\bin\\sonar-scanner ${scanner_properties} -Dfile.encoding=UTF-8
        //             """)
        //             PROJECT_URL = "${env.SONAR_HOST_URL}/dashboard?id=${PROJECT_KEY}"
        //             }

        //             if (!rocket_channel.isEmpty() ) {
        //                 def qg = waitForQualityGate()
        //                 rocketSend channel: rocket_channel, message: "Sonar check completed: [${env.JOB_NAME} ${env.BUILD_NUMBER}](${env.JOB_URL}) STATUS: [${qg.status}](${PROJECT_URL})", rawMessage: true
        //                 }
        //             }
        //             }
        //         }
        //     }
        }
    }
}


def call(){
    call([:])
}
