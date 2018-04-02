# SetTracingLevel
для автоматической установки уровня трейса в 4 для всех фалов xar в директории

### Инструкция для автоматической установки уровня трейса в 4 и изменения топика для него
Для запуска проекта достаточно его собрать  
````
mvn clean package
````
и запустить в папке target  
````
SetTracingLevel.bat

он автоматически выполнит матод  main,  
а результат можно будет увидеть в логе  
SetTracingLevel.log  

config.properties содержит данные:  

archive_directory=d:/distrib/java/github/SetTracingLevel/target
tracking_endpoint_topic=Broker_Tracking.Entry  
trackingLevel=4  
````
