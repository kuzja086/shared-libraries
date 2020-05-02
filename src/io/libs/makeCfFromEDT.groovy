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
        }

        stages{
            stage{
                
            }
        }
    }
}

def call(){
    call([:])
}