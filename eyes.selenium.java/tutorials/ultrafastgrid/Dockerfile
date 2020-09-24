FROM tutorial_java:latest

ARG repo=tutorial-selenium-java-ultrafastgrid
ENV REPOSITORY_NAME=$repo
RUN cd home/project \
    && git clone https://github.com/applitools/${REPOSITORY_NAME}.git \
    && cd $REPOSITORY_NAME \
    && cd src/test/java/com/applitools/quickstarts \
    && sed -i 's/"APPLITOOLS_API_KEY"/System.getenv("APPLITOOLS_API_KEY")/g' AppTest.java

WORKDIR home/project/$REPOSITORY_NAME

CMD //bootstrap.sh > /dev/null 2>&1 && mvn install exec:java -Dexec.mainClass="com.applitools.quickstarts.AppTest"  -Dexec.classpathScope=test -Dmaven.compiler.source="1.7" -Dmaven.compiler.target="1.7"
