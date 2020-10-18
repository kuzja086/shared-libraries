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
            def EDT_VERSION      = getParametrValue(buildEnv, 'edtVersion')
            def TEMP_CATALOG     = getParametrValue(buildEnv, 'tempCatalog')
            def PROJECT_NAME     = getParametrValue(buildEnv, 'projectNameEDT')
            def XMLPATH          = getParametrValue(buildEnv, 'xmlPath')
            def PLATFORM1C       = getParametrValue(buildEnv, 'platform1C')
            def CFPATH           = getParametrValue(buildEnv, 'cfPath')
            def SAVEEXTENSIONINFILE = getParametrValue(buildEnv, 'saveExtensionInFile')
            def EXTENSION = getParametrValue(buildEnv, 'extension')
            def XMLPATHEXTENSION = getParametrValue(buildEnv, 'xmlPathExtension')
            def base1CCredentialID = getParametrValue(buildEnv, 'base1CCredentialID')
            def storages1cCredentalsID = getParametrValue(buildEnv, 'storages1cCredentalsID')
        }

        stages{
            // parallel{
            stage{
                steps {
                    timestamps {
                        script {
                            def projectHelpers = new ProjectHelpers()

                            projectHelpers.storageLock(platform1C, bas)

                        }
                    }
                }
            }
            // }
            
        }
    }
}