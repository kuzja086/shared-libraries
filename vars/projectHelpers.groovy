import vars
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
    withCredentials([usernamePassword(credentialsId: "${base1CCredentialID}", usernameVariable: 'USERNAMEBASE', passwordVariable: 'PASSWORDBASE'),
        usernamePassword(credentialsId: "${sqlCredentialsID}", usernameVariable: 'USERNAMESQL', passwordVariable: 'PASSWORDSQL')]){
       
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
//
//
// def loadCfgFrom1CStorage(storageTCP, storageUser, storagePwd, connString, admin1cUser, admin1cPassword) {

//     storagePwdLine = ""
//     if (storagePwd != null && !storagePwd.isEmpty()) {
//         storagePwdLine = "--storage-pwd ${storagePwd}"
//     }

//     platformLine = ""

//     returnCode = utils.cmd("runner loadrepo --storage-name ${storageTCP} --storage-user ${storageUser} ${storagePwdLine} --ibconnection ${connString} --db-user ${admin1cUser} --db-pwd ${admin1cPassword}")
//     if (returnCode != 0) {
//          utils.raiseError("Загрузка конфигурации из 1С хранилища  ${storageTCP} завершилась с ошибкой. Для подробностей смотрите логи.")
//     }
// }

// // Обновляет базу в режиме конфигуратора. Аналог нажатия кнопки f7
// //
// // Параметры:
// //
// //  connString - строка соединения, например /Sdevadapter\template_adapter_adapter
// //  platform - полный номер платформы 1с
// //  admin1cUser - администратор базы
// //  admin1cPassword - пароль администратора базы
// //
// def updateInfobase(connString, admin1cUser, admin1cPassword, platform) {
//     admin1cUserLine = "";
//     if (!admin1cUser.isEmpty()) {
//         admin1cUserLine = "--db-user ${admin1cUser}"
//     }
//     admin1cPassLine = "";
//     if (!admin1cPassword.isEmpty()) {
//         admin1cPassLine = "--db-pwd ${admin1cPassword}"
//     }
//     platformLine = ""
//     if (platform != null && !platform.isEmpty()) {
//         platformLine = "--v8version ${platform}"
//     }

//     returnCode = utils.cmd("runner updatedb --ibconnection ${connString} ${admin1cUserLine} ${admin1cPassLine} ${platformLine}")
//     if (returnCode != 0) {
//         utils.raiseError("Обновление базы ${connString} в режиме конфигуратора завершилось с ошибкой. Для дополнительной информации смотрите логи")
//     }
// }