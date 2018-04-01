# SetTracingLevel
для автоматической установки уровня трейса в 4 и изменения топика для него

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
````
file_for_change=run.mailing_service_vpk.ws.esbp
archive_name=c:/Sonic2015/Workbench10.0/workspace/MailingWS/target/out/MailingWS_mapped.xar
tracking_endpoint_topic=Broker_Tracking.Entry
trackingLevel=4
````
