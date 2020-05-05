String call(Map buildParams, String keyName) {
    def defaultParam = getDefaultParams()
    if(env."${keyName}" != null){
        println "ENV: для ключа ${keyName} найдено значение " + env."${keyName}"
        return env."${keyName}"
    }else{
        if (buildParams."${keyName}" != null){
            println "buildParams: для ключа ${keyName} найдено значение " + buildParams."${keyName}"
            return buildParams."${keyName}"
        }
        if(defaultParam."${keyName}" != null){
            println "defaultParam: для ключа ${keyName} найдено значение " + defaultParam."${keyName}"
            return defaultParam."${keyName}"
        }
        println "Значение для ключа ${keyName} не найдено. Возвращаем пустую строку."
        return new String()
    }
}

def getDefaultParams(){
   CURRENT_CATALOG = ${env.WORKSPACE}
    return [
        // Общие параметры
        'agent' : 'testserver', // Имя агента
        'isFileBase' : 'false', // Это файловая база
        'fileBasePath': 'C:\\temp', // Путь к файловой базе
        'server1c' : 'sqlserever', // Адрес сервера 1С
        'agent1cPort' : '2441', // Порт агента сервера 1С
        'infobase' : 'tempBase', // Имя базы на сервере
        'tempCatalog' : "${CURRENT_CATALOG}\\temp", // Служебный каталог
        'platform1C' : '8.3.14.1779', //Версия платформы
        'xmlPath' : "${CURRENT_CATALOG}\\xmlpath", // Путь к выгрузке файлов конфигурации
        'cfPath' : "${CURRENT_CATALOG}\\build", // Каталог для выгрузкки *.cf и *.cfe
        'emailForNotification' : 'kozs@tlink.ru', // Email для отправки результатов сборки
        'saveExtensionInFile' : 'false', // Сохранить расширение в файл *.cfe
        'extension' : "extension", // Имя расширения по умолчанию
        'xmlPathExtension' : "${CURRENT_CATALOG}\\xmlpathExtension", // Путь к выгрузке файлов расширения

        // EDT
        'edtVersion' : '2020.3',
        'projectNameEDT' : 'projectNameEDT',

        // Тестирование

        //Git
        'branch' : 'master', // Ветка Git по умолчанию
        'credentialsId' : 'GitHubID', // Имя credentialsId для GitHub
        'targetDir' : "${CURRENT_CATALOG}\\tools" // Каталог для вспомогательных инструментов
    ]
}