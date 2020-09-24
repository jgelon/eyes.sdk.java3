FROM ubuntu:18.04

RUN apt-get update && apt-get clean && apt-get install -y \
    x11vnc \
    xvfb \
    fluxbox \
    wmctrl \
    wget \
    curl \
    gnupg2 \
    default-jdk \
    unzip \
    && apt update \
    && apt install -y git

RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list \
    && apt-get update && apt-get -y install google-chrome-stable

COPY /base/chrome_wrapper.sh /opt/bin/wrap_chrome_binary
RUN /opt/bin/wrap_chrome_binary

RUN wget "https://chromedriver.storage.googleapis.com/$(wget -qO - "https://chromedriver.storage.googleapis.com/LATEST_RELEASE")/chromedriver_linux64.zip"
RUN unzip chromedriver_linux64
RUN mv chromedriver /usr/bin/

RUN mkdir /home/project

ENV DISPLAY :1
COPY /base/bootstrap.sh /
CMD '/bootstrap.sh'

ARG version
ENV JAVA_HOME=/usr/lib/jvm/default-java

RUN wget https://www-us.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip -P /tmp
RUN unzip ./tmp/apache-maven-3.6.3-bin.zip -d /opt

ENV PATH="/opt/apache-maven-3.6.3/bin:${PATH}"

COPY ./base/eyes-selenium-java*jar-with-dependencies.jar /eyes-selenium.jar
#RUN  mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=eyes-selenium.jar
RUN mvn -X install:install-file -Dfile=eyes-selenium.jar -DgroupId=com.applitools -DartifactId=eyes-selenium-java3 -Dversion=$version -Dpackaging=jar -DgeneratePom=true