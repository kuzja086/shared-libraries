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
        'platform1C' : '8.3.17.1496', //Версия платформы
        'serverSql' : 'sqlserver\\sqlexpress', // Адрес sql сервера
        'emailForNotification' : 'kozs@tlink.ru', // Email для отправки результатов сборки
        'tempCatalpgOtherDisc' : 'P:\\Козинский', // Временный каталог на другом диске
        'catalog1c' : "", // Имя каталога с платформой
        'executorPath' : 'D:\\Executor\\bin\\', // Путь к Исполнителю

        // EDT
        'edtVersion' : '2020.5', // Версия EDT
        'projectNameEDT' : 'projectNameEDT', // Имя проекта в EDT

        // Тестирование
        'debugger' : 'http://192.168.0.112:2450', // Адрес сервера отладки
        'runTesting' : 'true', // Проводить тестирование
        'testFeature' : 'false', // Запускать тестовые фичи

        //Git
        'toolsBranch' : 'master', // Ветка Git по умолчанию
        'toolsCredentialsId' : 'GitHubID', // Имя credentialsId для GitHub
        'toolsTargetDir' : "${CURRENT_CATALOG}\\tools", // Каталог для вспомогательных инструментов
        'toolsRepo' : 'https://github.com/kuzja086/testing_tool.git', // Репозитарий с инструментами для тестирования

        //Sonar
        'memoryForJava' : '16', // Количество ГБ для выполнения операций на JAVA
        'oneAgent' : 'false', // Пайплайн выполняется на одном агенте
        'runSonar' : 'true', // Запускать проверку в SonarQube

        // Создание Дистрибутивов
        'makeDistrib' : 'true',
        'xmlPath' : "${CURRENT_CATALOG}\\xmlpath", // Путь к выгрузке файлов конфигурации
        'cfPath' : "${CURRENT_CATALOG}\\buildDistrib", // Каталог для выгрузкки *.cf и *.cfe
        'saveExtensionInFile' : 'false', // Сохранить расширение в файл *.cfe
        'extension' : "extension", // Имя расширения по умолчанию
        'xmlPathExtension' : "${CURRENT_CATALOG}\\xmlpathExtension" // Путь к выгрузке файлов расширения
    ]
}