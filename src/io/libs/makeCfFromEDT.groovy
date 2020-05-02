package io.libs

def call(Map buildEnv){
    def connectionString = projecthelpers.getconnectionString(buildEnv)

    pipeline {
        agent {
            label getParametrValue(buildEnv, 'agent')
        }

        post { // Выполняется после сборки
            failure {
                sendEmailMessage("Failed", buildEnv.emailForNotification) // Научиться отправлять почту и добавить условие истина
            }
        }

        enviroment {
            // Заполнить параметры для пайплайна
            EDT_VERSION      = getParametrValue(buildEnv, 'edtVersion')
            TEMP_CATALOG     = getParametrValue(buildEnv, 'tempCatalog')
            PROJECT_NAME     = getParametrValue(buildEnv, 'projectNameEDT')
            XMLPATH          = getParametrValue(buildEnv, 'xmlPath')
            PLATFORM1C       = getParametrValue(buildEnv, 'platform1C')
            CFPATH           = getParametrValue(buildEnv, 'cfPath')
            CURRENT_CATALOG  = pwd()
        }

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
                        cmd("""
                        @set RING_OPTS=-Dfile.encoding=UTF-8 -Dosgi.nl=ru
                        ring edt@${EDT_VERSION} workspace export --workspace-location \"${TEMP_CATALOG}\" --project \"${PROJECT_NAME_EDT}\" --configuration-files \"${XMLPATH}\
                        """)
                   }
                }
            }
        }
        stage('Загрузка конфигурации из файлов') {
            steps {
                timestamps {
                    script {
                        IB = "File=${TEMP_CATALOG}"

                        cmd("""
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

                        cmd("""
                        cd /D C:\\Program Files (x86)\\1cv8\\${PLATFORM1C}\\bin\\
                        1cv8.exe DESIGNER /WA- /DISABLESTARTUPDIALOGS /IBConnectionString ${IB} /CreateDistributionFiles -cffile ${CFPATH}
                        """)
                   }
                }
            }
        }
    }
}

def call(){
    call([:])
}