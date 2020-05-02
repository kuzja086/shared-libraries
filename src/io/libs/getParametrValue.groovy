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
        return new String
    }
}

def getDefaultParams(){
    return [
        // Общие параметры
        'isFileBase': 'false', // Это файловая база
        'fileBasePath': 'C:\\temp', // Путь к файловой базе
        'server1c' : 'sqlserever', // Адрес сервера 1С
        'agent1cPort' : '2441', // Порт агента сервера 1С
        'infobase' : 'tempBase', // Имя базы на сервере
        'tempCatalog' : './temp', // Служебный каталог
        'platform1C' : '8.3.14.1779', //Версия платформы
        'xmlPath' : './xmlpath', // Путь к выгрузке файлов конфигурации
        'cfPath' : './build', // Каталог для выгрузкки *.cf

        // EDT
        'edtVersion' : '2020.3',

        // Тестирование

    ]
}