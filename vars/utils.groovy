def getConnectionString(Map buildEnv) {
    def isFileBase   = getParametrValue(buildEnv, 'isFileBase')
    def fileBasePath = getParametrValue(buildEnv, 'fileBasePath')
    def server1c     = getParametrValue(buildEnv, 'server1c')
    def agent1cPort  = getParametrValue(buildEnv, 'agent1cPort')
    def infobase     = getParametrValue(buildEnv, 'infobase')

    if(isFileBase.trim().equals("true")) {
       connectionString = "/F${fileBasePath}" 
    }
    else{
        connectionString = "/S${server1c}:${agent1cPort}\\${infobase}"
    }
    return connectionString
}

def cmd(String _command, String credentionalID){
    if(credentionalID.trim().isEmpty()){
        command = _command.replace("username", "")
        command = _command.replace("password", "")
    }

    withCredentials([usernamePassword(credentionalsId: "${credentionalID}", usernameVarible: 'USERNAME', passwordVarible: 'PASSWORD')])

    command = _command.replace("username", USERNAME)
    command = _command.replace("password", PASSWORD)

    cmd(command)
}

def cmd(String _command){
    if (isUnix()){
        sh "${_command}"
    }else {
        bat "chcp 65001\n${_command}"
    }
}

// Конвертирует строку в массив по сплиттеру
//
// Параметры:
//  line - строка с разделителями
//
// Возвращаемое значение
//  Array - массив строк
//
def lineToArray(line, splitter = ",") {
    dirtArray = line.replaceAll("\\s", "").split(",")
    cleanArray = []
    for (item in dirtArray) {
        if (!item.isEmpty()) {
            cleanArray.add(item)
        }
    }
    return cleanArray
}
