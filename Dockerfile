FROM openjdk:8-jre
EXPOSE 8080

# JMeter version
ARG JMETER_VERSION=4.0

# Install few utilities
RUN apt-get clean && \
apt-get update && \
apt-get -qy install \
wget \
cron \
procps \
telnet \
vim \
iputils-ping \
unzip \
net-tools

# Install JMeter
RUN   mkdir /jmeter \
      && cd /jmeter/ \
      && wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-$JMETER_VERSION.tgz \
      && tar -xzf apache-jmeter-$JMETER_VERSION.tgz \
      && rm apache-jmeter-$JMETER_VERSION.tgz

# ADD all the plugins
ADD jmeter-plugins-dubbo-2.7.3-jar-with-dependencies.jar /jmeter/apache-jmeter-$JMETER_VERSION/lib/ext/jmeter-plugins-dubbo-2.7.3-jar-with-dependencies.jar

ADD jmeter.properties /jmeter/apache-jmeter-$JMETER_VERSION/bin/jmeter.properties

# Set JMeter Home
ENV JMETER_HOME /jmeter/apache-jmeter-$JMETER_VERSION/

# Add JMeter to the Path
ENV PATH $JMETER_HOME/bin:$PATH

VOLUME /tmp
ADD renren-fast.jar /app.jar

EXPOSE 1099 50000

RUN cd /jmeter/apache-jmeter-4.0/bin \
    && mkdir stressTestCases

ADD renren-start.sh /usr/bin/renren-start.sh
RUN sed -i 's/\r$//' /usr/bin/renren-start.sh \
&& chmod +x /usr/bin/renren-start.sh

RUN bash -c 'touch /app.jar'
ENTRYPOINT ["renren-start.sh"]