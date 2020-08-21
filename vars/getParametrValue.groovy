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
        println "Значение для ключа ${keyName} не найдено. Возвращаем null"
        return null
    }
}

def getDefaultParams(){
   CURRENT_CATALOG = "${env.WORKSPACE}"
    return [
        // Общие параметры
        'agent' : 'testserver', // Имя агента
        'isFileBase' : 'false', // Это файловая база
        'fileBasePath': 'C:\\temp', // Путь к файловой базе
        'server1c' : 'sqlserver', // Адрес сервера 1С
        'server1cPort' : '2440', // Порт рабочего сервера 1с. Не путать с портом агента кластера (2441)
        'agent1cPort' : '2441', // Порт агента кластера 1с
        'infobase' : 'tempBase', // Имя базы на сервере
        'tempCatalog' : "${CURRENT_CATALOG}\\temp", // Служебный каталог
        'serverCopyPath' : '\\\\sqlserver\\temp_for_1c$\\Kozynskiy\\jenkins',
        'platform1C' : '8.3.14.1779', //Версия платформы
        'serverSql' : 'sqlserver\\sqlexpress', // Адрес sql сервера
        'xmlPath' : "${CURRENT_CATALOG}\\xmlpath", // Путь к выгрузке файлов конфигурации
        'cfPath' : "${CURRENT_CATALOG}\\build", // Каталог для выгрузкки *.cf и *.cfe
        'emailForNotification' : 'kozs@tlink.ru', // Email для отправки результатов сборки
        'saveExtensionInFile' : 'false', // Сохранить расширение в файл *.cfe
        'extension' : "extension", // Имя расширения по умолчанию
        'xmlPathExtension' : "${CURRENT_CATALOG}\\xmlpathExtension", // Путь к выгрузке файлов расширения
        'tempCatalpgOtherDisc' : 'P:\\Козинский\\', // Временный каталог на другом диске
        
        // EDT
        'edtVersion' : '2020.5', // Версия EDT
        'projectNameEDT' : 'projectNameEDT', // Имя проекта в EDT

        // Тестирование

        //Git
        'toolsBranch' : 'master', // Ветка Git по умолчанию
        'toolsCredentialsId' : 'GitHubID', // Имя credentialsId для GitHub
        'toolsTargetDir' : "${CURRENT_CATALOG}\\tools", // Каталог для вспомогательных инструментов
        'toolsRepo' : 'https://github.com/kuzja086/testing_tool.git', // Репозитарий с инструментами для тестирования

        //Sonar
        'memoryForJava' : '2', // Количество ГБ для выполнения операций на JAVA
        'perf_catalog' : '' // Каталог для замеров производительности
    ]
}