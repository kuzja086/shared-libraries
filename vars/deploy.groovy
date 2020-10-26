import io.libs.SqlUtils
import io.libs.ProjectHelpers
import io.libs.Utils

def call(Map buildEnv){
    pipeline {
        agent {
            label getParametrValue(buildEnv, 'agent')
        }

        environment {
            // Заполнить параметры для пайплайна
            def CURRENT_CATALOG  = pwd()
            def platform1C       = getParametrValue(buildEnv, 'platform1C')
            def CFPATH           = getParametrValue(buildEnv, 'cfPath')
            def base1CCredentialID = getParametrValue(buildEnv, 'base1CCredentialID')
            def storages1cCredentalsID = getParametrValue(buildEnv, 'storages1cCredentalsID')
            def listOfBase = getParametrValue(buildEnv, 'listOfBase')
            def listOfStorage = getParametrValue(buildEnv, 'listOfStorage')
            def listOfObjects = getParametrValue(buildEnv, 'ListOfObjects')
        }

        stages{
            // parallel{
            stage{
                steps {
                    timestamps {
                        script {
                            utils = new Utils()
                            projectHelpers = new ProjectHelpers()
                            //TODO Переделать на цикл
                            ib = listOfBase
                            storagePath = listOfStorage
                            objectsPath = listOfObjects
                            //Для credentional Используются одинаковые данные, если в базе другие, нужно добавить служебного пользователя
                            projectHelpers.storageLock(platform1C, base1CCredentialID, storages1cCredentalsID, ib, storagePath, objectsPath)

                        }
                    }
                }
            }
            // }
            
        }
    }
}