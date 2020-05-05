def call(Map buildEnv){
    pipeline {
        agent {
            label getParametrValue(buildEnv, 'agent')
        }

        // post { // Выполняется после сборки
        //     failure {
        //        // sendEmailMessage("Failed", buildEnv.emailForNotification) // Научиться отправлять почту и добавить условие истина
        //     }
        // }

        environment {
            // Заполнить параметры для пайплайна
            def CURRENT_CATALOG  = pwd()
            def EDT_VERSION      = getParametrValue(buildEnv, 'edtVersion')
            def TEMP_CATALOG     = getParametrValue(buildEnv, 'tempCatalog')
            def PROJECT_NAME     = getParametrValue(buildEnv, 'projectNameEDT')
            def XMLPATH          = getParametrValue(buildEnv, 'xmlPath')
            def PLATFORM1C       = getParametrValue(buildEnv, 'platform1C')
            def CFPATH           = getParametrValue(buildEnv, 'cfPath')
            def SAVEEXTENSIONINFILE = getParametrValue(buildEnv, 'saveExtensionInFile')
            def EXTENSION = getParametrValue(buildEnv, 'extension')
            def XMLPATHEXTENSION = getParametrValue(buildEnv, 'xmlPathExtension')
        }

        stages{
            stage('Инициализация') {
                steps {
                    timestamps {
                        script {
                            dir(TEMP_CATALOG) {
                                deleteDir()
                            }
                            dir(XMLPATH){
                                deleteDir()
                            }
                        }
                    }
                }
            }
            stage('Выгрузка проекта из EDT в файлы конфигурации') {
                steps {
                    timestamps {
                        script {
                            PROJECT_NAME_EDT = "${CURRENT_CATALOG}\\${PROJECT_NAME}"
                            println PROJECT_NAME_EDT
                            dumpProjectEDTInFiles(EDT_VERSION, TEMP_CATALOG, PROJECT_NAME_EDT, XMLPATH)
                    }
                    }
                }
            }
            stage('Загрузка конфигурации из файлов') {
                steps {
                    timestamps {
                        script {
                            IB = "File=${TEMP_CATALOG}"

                            utils.cmd("""
                            cd /D C:\\Program Files (x86)\\1cv8\\${PLATFORM1C}\\bin\\
                            1cv8.exe CREATEINFOBASE ${IB}
                            1cv8.exe DESIGNER /WA- /DISABLESTARTUPDIALOGS /IBConnectionString ${IB} /LoadConfigFromFiles ${XMLPATH} /UpdateDBCfg
                            """)
                    }
                    }
                }
            }
            stage('Сохранение файла .cf') {
                steps {
                    timestamps {
                        script {
                            CFPATH = "${CFPATH}\\${PROJECT_NAME}.cf" 

                            utils.cmd("""
                            cd /D C:\\Program Files (x86)\\1cv8\\${PLATFORM1C}\\bin\\
                            1cv8.exe DESIGNER /WA- /DISABLESTARTUPDIALOGS /IBConnectionString ${IB} /CreateDistributionFiles -cffile ${CFPATH}
                            """)
                    }
                    }
                }
            }
            stage('Сохранение файла .cfe') {
                steps {
                    timestamps {
                        script {
                            // TODO Сделать цикл по расширениям, передавать через запятую имена расширений
                            if (SAVEEXTENSIONINFILE.trim().equals('true')){
                                PROJECT_NAME_EDT = "${CURRENT_CATALOG}\\${EXTENSION}"
                                dumpProjectEDTInFiles(EDT_VERSION, TEMP_CATALOG, PROJECT_NAME_EDT, XMLPATHEXTENSION)

                                utils.cmd("""
                                cd /D C:\\Program Files (x86)\\1cv8\\${PLATFORM1C}\\bin\\
                                1cv8.exe DESIGNER /WA- /DISABLESTARTUPDIALOGS /IBConnectionString ${IB} /LoadConfigFromFiles ${XMLPAthExtension} -Extension ${Extension} /UpdateDBCfg
                                1cv8.exe DESIGNER /WA- /DISABLESTARTUPDIALOGS /IBConnectionString ${IB} /DumpCfg ${ExtensionPath} -Extension ${Extension}
                                """)
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

def dumpProjectEDTInFiles(EDT_VERSION, TEMP_CATALOG, PROJECT_NAME_EDT, XMLPATH) {
    println "Выгрузка проекта из EDT" 
    utils.cmd("""
            @set RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru
            ring edt@${EDT_VERSION} workspace export --workspace-location ${TEMP_CATALOG} --project ${PROJECT_NAME_EDT} --configuration-files ${XMLPATH}
             """)  
}
