package io.libs


// Создает базу в кластере через RAS или пакетный режим. Для пакетного режима есть возможность создать базу с конфигурацией
//
// Параметры:
//  platform - номер платформы 1С, например 8.3.12.1529
//  server1c - сервер 1c
//  serversql - сервер sql 
//  sqluser - Пользователь sql
//  sqlpassw - Пароль sql
//  base - имя базы на сервере 1c и sql
//  cfdt - файловый путь к dt или cf конфигурации для загрузки. Только для пакетного режима!
//  isras - если true, то используется RAS для скрипта, в противном случае - пакетный режим
//
def createDb(platform, server1c, serversql, sqluser, sqlpassw, base, cfdt, isras) {
    utils = new Utils()

    cfdtpath = ""
    if (cfdt != null && !cfdt.isEmpty()) {
        cfdtpath = "-cfdt ${cfdt}"
    }

    israspath = ""
    if (isras) {
        israspath = "-isras true"
    }

    platformLine = ""
    if (platformLine != null && !platformLine.isEmpty()) {
        platformLine = "-platform ${platform}"
    }

    returnCode = utils.cmd("oscript one_script_tools/dbcreator.os ${platformLine} -server1c ${server1c} -serversql ${serversql} -sqluser ${sqluser} -sqlpassw ${sqlpassw} -base ${base} ${cfdtpath} ${israspath}")
    if (returnCode != 0) {
        utils.raiseError("Возникла ошибка при создании базы ${base} в кластере ${serversql}")
    }
}

// Убирает в 1С базу окошки с тем, что база перемещена, интернет поддержкой, очищает настройки ванессы
//
// Параметры:
//  сonnection_string - путь к 1С базе.
//  admin1cUsr - имя админа 1С базы
//  admin1cPwd - пароль админа 1С базы
//
def unlocking1cBase(connString, admin1cUsr, admin1cPwd) {
    utils = new Utils()

    admin1cUsrLine = ""
    if (admin1cUser != null && !admin1cUser.isEmpty()) {
        admin1cUsrLine = "--db-user ${admin1cUsr}"
    }

    admin1cPwdLine = ""
    if (admin1cPwd != null && !admin1cPwd.isEmpty()) {
        admin1cPwdLine = "--db-pwd ${admin1cPwd}"
    }

    utils.cmd("runner run --execute ${env.WORKSPACE}/one_script_tools/unlockBase1C.epf --command \"-locktype unlock\" ${admin1cUsrLine} ${admin1cPwdLine} --ibconnection=${connString}")
}

def getConnString(server1c, infobase, agent1cPort) {
    return "/S${server1c}:${agent1cPort}\\${infobase}"
}

// Удаляет базу из кластера через powershell.
//
// Параметры:
//  server1c - сервер 1с 
//  agentPort - порт агента кластера 1с
//  serverSql - сервер sql
//  base - база для удаления из кластера
//  base1CCredentialID - CredentialsID Для базы 1С
//  sqlCredentialsID - CredentialsID для sql сервера
//  fulldrop - если true, то удаляется база из кластера 1С и sql сервера
//
def dropDb(server1c, agentPort, serverSql, base, base1CCredentialID, sqlCredentialsID, fulldrop = false) {
    withCredentials([usernamePassword(credentialsId: "${base1CCredentialID}", usernameVariable: 'USERNAMEBASE', passwordVariable: 'PASSWORDBASE'),
        usernamePassword(credentialsId: "${sqlCredentialsID}", usernameVariable: 'USERNAMESQL', passwordVariable: 'PASSWORDSQL')]){
       
        utils = new Utils()

        fulldropLine = "";
        if (fulldrop) {
            fulldropLine = "-fulldrop true"
        }

        admin1cUserLine = "";
        if (base1CCredentialID != null && !base1CCredentialID.isEmpty()) {
            admin1cUserLine = "-user username -passw password"
            admin1cUserLine = admin1cUserLine.replace("username", USERNAMEBASE)
            admin1cUserLine = admin1cUserLine.replace("password", PASSWORDBASE)
        }

        sqluserLine = "";
        if (sqlCredentialsID != null && !sqlCredentialsID.isEmpty()) {
            sqluserLine = "-sqluser username -sqlPwd password"
            sqluserLine = sqluserLine.replace("username", USERNAMESQL)
            sqluserLine = sqluserLine.replace("password", PASSWORDSQL)
        }
        
        returnCode = utils.cmd("powershell -file \"${env.WORKSPACE}/copy_etalon/drop_db.ps1\" -server1c ${server1c} -agentPort ${agentPort} -serverSql ${serverSql} -infobase ${base} ${admin1cUserLine} ${sqluserLine} ${fulldropLine}")
        if (returnCode != 0) { 
            error "error when deleting base with COM ${server1c}\\${base}. See logs above fore more information."
        }
    }    
}

// Загружает в базу конфигурацию из 1С хранилища. Базу желательно подключить к хранилищу под загружаемым пользователем,
//  т.к. это даст буст по скорости загрузки.
//
// Параметры:
//  storageTCP - Адрес хранилища
//  storages1cCredentalsID - CredentialsID для Хранилища
//  connString - Строка подключения к базе
//  base1CCredentialID - CredentialsID Для базы 1С
//

def loadCfgFrom1CStorage(storageTCP, storages1cCredentalsID, connString, base1CCredentialID) {
     withCredentials([usernamePassword(credentialsId: "${base1CCredentialID}", usernameVariable: 'USERNAMEBASE', passwordVariable: 'PASSWORDBASE'),
        usernamePassword(credentialsId: "${storages1cCredentalsID}", usernameVariable: 'USERNAMESTORAGE', passwordVariable: 'PASSWORDSTORAGE')]){
        utils = new Utils()

        storageAuth = ""
        if (storages1cCredentalsID != null && !storages1cCredentalsID.isEmpty()) {
            storageAuth = "--storage-user username --storage-pwd password"
            storageAuth = storageAuth.replace("username", USERNAMESTORAGE)
            storageAuth = storageAuth.replace("password", PASSWORDSTORAGE)
        }

        baseAuth = "";
        if (base1CCredentialID != null && !base1CCredentialID.isEmpty()) {
            admin1cUserLine = "--db-user username --db-pwd password"
            admin1cUserLine = admin1cUserLine.replace("username", USERNAMEBASE)
            admin1cUserLine = admin1cUserLine.replace("password", PASSWORDBASE)
        }

        returnCode = utils.cmd("runner loadrepo --storage-name ${storageTCP}  ${storageAuth} --ibconnection ${connString} ${baseAuth}")
        if (returnCode != 0) {
            utils.raiseError("Загрузка конфигурации из 1С хранилища  ${storageTCP} завершилась с ошибкой. Для подробностей смотрите логи.")
        }
    }
}

// Обновляет базу в режиме конфигуратора. Аналог нажатия кнопки f7
//
// Параметры:
//
//  connString - строка соединения, например /Sdevadapter\template_adapter_adapter
//  platform - полный номер платформы 1с
//  base1CCredentialID - CredentialsID Для базы 1С
//
def updateInfobase(connString, base1CCredentialID, platform) {
    withCredentials([usernamePassword(credentialsId: "${base1CCredentialID}", usernameVariable: 'USERNAMEBASE', passwordVariable: 'PASSWORDBASE')){
        utils = new Utils()

        baseAuth = "";
        if (base1CCredentialID != null && !base1CCredentialID.isEmpty()) {
            admin1cUserLine = "--db-user username --db-pwd password"
            admin1cUserLine = admin1cUserLine.replace("username", USERNAMEBASE)
            admin1cUserLine = admin1cUserLine.replace("password", PASSWORDBASE)
        }

        platformLine = ""
        if (platform != null && !platform.isEmpty()) {
            platformLine = "--v8version ${platform}"
        }

        returnCode = utils.cmd("runner updatedb --ibconnection ${connString} ${baseAuth} ${platformLine}")
        if (returnCode != 0) {
            utils.raiseError("Обновление базы ${connString} в режиме конфигуратора завершилось с ошибкой. Для дополнительной информации смотрите логи")
        }
    }    
}
