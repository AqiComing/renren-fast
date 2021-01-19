#!/bin/bash
#启动jmeter
nohub /jmeter/apache-jmeter-4.0/bin/jmeter-server -Dserver.rmi.localport=50000 -Dserver_port=1099 &