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

    withCredentials([usernamePassword(credentionalsId: "${credentionalID}", usernameVarible: 'USERNAME', passwordVarible: 'PASSWORD')]){
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

// Удаляет базу из кластера через powershell.
//
// Параметры:
//  server1c - сервер 1с 
//  agentPort - порт агента кластера 1с
//  serverSql - сервер sql
//  base - база для удаления из кластера
//  admin1cUser - имя администратора 1С в кластере для базы
//  admin1cPwd - пароль администратора 1С в кластере для базы
//  sqluser - юзер sql
//  sqlPwd - пароль sql
//  fulldrop - если true, то удаляется база из кластера 1С и sql сервера
//
def dropDb(server1c, agentPort, serverSql, base, base1CCredentialID, sqlCredentialsID, fulldrop = false) {
     withCredentials([usernamePassword(credentionalsId: "${base1CCredentialID}", usernameVarible: 'USERNAME1C', passwordVarible: 'PASSWORD1C'),
        usernamePassword(credentionalsId: "${sqlCredentialsID}", usernameVarible: 'USERNAMESQL', passwordVarible: 'USERNAMESQL')]){
    fulldropLine = "";
    if (fulldrop) {
        fulldropLine = "-fulldrop true"
    }

    admin1cUserLine = "";
    if (base1CCredentialID != null && !base1CCredentialID.isEmpty()) {
        admin1cUserLine = "-user username -passw password"
        admin1cUserLine.replace("username", USERNAME1C)
        admin1cUserLine.replace("password", PASSWORD1C)
    }

    sqluserLine = "";
    if (sqlCredentialsID != null && !sqlCredentialsID.isEmpty()) {
        sqluserLine = "-sqluser username -sqlPwd password"
        sqluserLine.replace("username", USERNAMESQL)
        sqluserLine.replace("password", USERNAMESQL)
    }

    cmd("powershell -file \"${env.WORKSPACE}/copy_etalon/drop_db.ps1\" -server1c ${server1c} -agentPort ${agentPort} -serverSql ${serverSql} -infobase ${base} ${admin1cUserLine} ${sqluserLine} ${fulldropLine}")
}