import io.libs.SqlUtils
import io.libs.ProjectHelpers
import io.libs.Utils

def call(Map buildEnv){
    pipeline {
        agent {
            label getParametrValue(buildEnv, 'agent')
        }

        // post { // Выполняется после сборки
            // Варианты в документации
            // failure {
            //    sendEmailMessage("Failed", buildEnv.emailForNotification) // Научиться отправлять почту и добавить условие истина
            // }
        // }

        environment {
            // Заполнить параметры для пайплайна
            // TODO Добавить комментарий, Обязательный или НЕТ
            def CURRENT_CATALOG  = pwd()
            def MEMORY_FOR_JAVA = getParametrValue(buildEnv, 'memoryForJava')
            def toolsTargetDir = getParametrValue(buildEnv, 'toolsTargetDir')
            def EDT_VERSION      = getParametrValue(buildEnv, 'edtVersion')
            def projectNameEDT = getParametrValue(buildEnv, 'projectNameEDT')
            def tempCatalpgOtherDisc = getParametrValue(buildEnv, 'tempCatalpgOtherDisc')
            def tempCatalog = getParametrValue(buildEnv, 'tempCatalog')
            def oneAgent = getParametrValue(buildEnv, 'oneAgent')
        }

        options {
            timeout(time: 8, unit: 'HOURS')
            buildDiscarder(logRotator(numToKeepStr: '10'))
        }

        stages{
            stage('Checkout and Initialization') {
                steps {
                    timestamps {
                        script {
                            CURRENT_CATALOG = pwd()

                            RESULT_CATALOG = "${CURRENT_CATALOG}\\sonar_result"
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
                            edtValidationSonar = "sonar_result\\edt-validation.csv"
                            projectName = "${CURRENT_CATALOG}\\${projectNameEDT}"

                            perf_catalog = "${tempCatalpgOtherDisc}\\coverage\\${projectNameEDT}"
                        }
                    }
                }
            }
            // stage('bsl-language-server') {
            //     steps {
            //         timestamps {
            //             script {
            //                 BSL_LS_JAR = "${toolsTargetDir}/bsl-language-server.jar"
            //                 BSL_LS_PROPERTIES = "${toolsTargetDir}/bsl-language-server.conf"

            //                 def utils = new Utils()
            //                 utils.cmd("java -Xmx${MEMORY_FOR_JAVA}g -jar ${BSL_LS_JAR} -a -s \"${SRC}\" -r generic -c \"${BSL_LS_PROPERTIES}\" -o \"${RESULT_CATALOG}\"")
            //             }
            //         }
            //     }
            // }
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
            stage('EDT') {
                steps {
                    timestamps {
                        script {
                        def utils = new Utils()
                        if (fileExists("${EDT_VALIDATION_RESULT}")) {
                            utils.cmd("@DEL \"${EDT_VALIDATION_RESULT}\"")
                        }
                        utils.cmd("""
                            @set RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru
                            ring edt@${EDT_VERSION} workspace validate --workspace-location \"${tempCatalog}\" --file \"${EDT_VALIDATION_RESULT}\" --project-list \"${projectName}\"
                            """)
                        }

                        // TODO Сделать универсально
                        archiveArtifacts artifacts: edtValidationSonar
                        stash name: "edtValidationSonar", includes: edtValidationSonar
                    }
                }
            }
            stage('Конвертация результатов EDT') {
                agent {
                    label 'FirstNode'
                }
                steps {
                    timestamps {
                        script {
                            def utils = new Utils()

                            dir (RESULT_CATALOG)
                            unstash name: "edtValidationSonar"

                            utils.cmd("""
                            set SRC=\"${SRC}\"
                            stebi convert -e \"${EDT_VALIDATION_RESULT}\" \"${RESULT_CATALOG}/edt.json\" 
                            """)
                        }
                    }
                }
            }
            stage('Трансформация результатов') {
                agent {
                    label 'FirstNode'
                }
                steps {
                    timestamps {
                        script {
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
                }
            }
            // stage('Получение покрытия') {
            //     agent {
            //         label 'FirstNode'
            //     }
            //     steps {
            //         timestamps {
            //             script {
            //             if (!perf_catalog.isEmpty()) {
            //                     cmd("perf-measurements-to-cover c -i=${perf_catalog} -o=\"${perf_catalog}\\genericCoverage.xml\" -s=\"${SRC}\"")
            //             } else{
            //                 echo "skip"
            //             }
            //             }
            //         }
            //     }
            // }
            stage('Сканер') {
                steps {
                    timestamps {
                        script {
                        // dir('Repo') {
                            withSonarQubeEnv('Sonar') {
                                def scanner_properties = "-Dsonar.projectVersion=%SONAR_PROJECTVERSION% -Dsonar.projectKey=${projectNameEDT} -Dsonar.sources=\"${SRC}\" -Dsonar.externalIssuesReportPaths=${GENERIC_ISSUE_JSON} -Dsonar.sourceEncoding=UTF-8 -Dsonar.inclusions=**/*.bsl"

                                // if (!perf_catalog.isEmpty()) {
                                //     scanner_properties = "${scanner_properties} -Dsonar.coverageReportPaths=\"${perf_catalog}\\genericCoverage.xml\""
                                // }

                                def scannerHome = tool 'SonarQube Scanner';

                                def utils = new Utils()

                                utils.cmd("""
                                @set SRC=\"${SRC}\"
                                @echo %SRC%
                                @set SONAR_SCANNER_OPTS=-Xmx${MEMORY_FOR_JAVA}g
                                ${scannerHome}\\sonar-scanner\\bin\\sonar-scanner ${scanner_properties} -Dfile.encoding=UTF-8
                                """)

                                PROJECT_URL = "${env.SONAR_HOST_URL}/dashboard?id=${projectNameEDT}"
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
