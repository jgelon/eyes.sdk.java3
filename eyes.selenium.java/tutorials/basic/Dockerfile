FROM tutorial_java:latest

ARG repo=tutorial-selenium-java-basic
ENV REPOSITORY_NAME=$repo
RUN cd home/project \
    && git clone https://github.com/applitools/${REPOSITORY_NAME}.git \
    && cd $REPOSITORY_NAME \
    && cd src/test/java/com/applitools/quickstarts \
    && sed -i 's/"APPLITOOLS_API_KEY"/System.getenv("APPLITOOLS_API_KEY")/g' BasicDemo.java

WORKDIR home/project/$REPOSITORY_NAME

CMD //bootstrap.sh > /dev/null 2>&1 && mvn -Dtest=BasicDemo test
