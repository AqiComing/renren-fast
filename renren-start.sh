#!/bin/bash
cd /jmeter/apache-jmeter-4.0/bin/stressTestCases &&

nohup ../jmeter-server -Dserver.rmi.localport=50000 -Dserver_port=1099 -Jserver.rmi.ssl.disable=true >nohup.out &

#启动服务
java -jar /app.jar --spring.profiles.active=pro