import java.text.SimpleDateFormat;

def getConnectionString(Map buildEnv, String infobase) {
    def isFileBase   = getParametrValue(buildEnv, 'isFileBase')
    def fileBasePath = getParametrValue(buildEnv, 'fileBasePath')
    def server1c     = getParametrValue(buildEnv, 'server1c')
    def agent1cPort  = getParametrValue(buildEnv, 'agent1cPort')

    if(isFileBase.trim().equals("true")) {
       connectionString = "/F${fileBasePath}" 
    }
    else{
        connectionString = "/S${server1c}:${agent1cPort}\\${infobase}"
    }
    return connectionString
}

def getConnectionString(Map buildEnv) {
    getConnectionString(buildEnv, getParametrValue(buildEnv, 'infobase'))
}

def cmd(String _command, String credentionalID){
    if(credentionalID.trim().isEmpty()){
        command = _command.replace("username", "")
        command = _command.replace("password", "")
    }
    
    withCredentials([usernamePassword(credentialsId: "${credentionalID}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]){
        command = _command.replace("username", USERNAME)
        command = _command.replace("password", PASSWORD)

        cmd(command)
    }
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

// Возвращает Timestamp вида yyyyMMdddss
//
// Возвращаемое значение
//  String - сгенерированный timestamp
//
def currentDateStamp() {
    dateFormat = new SimpleDateFormat("yyyyMMdddss");
    date = new Date();
    return  dateFormat.format(date);
}

