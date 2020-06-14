import io.libs.SqlUtils
import io.libs.ProjectHelpers
import io.libs.Utils

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
            stage('Инициализация') {
                steps {
                    timestamps {
                        script {
                            
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
