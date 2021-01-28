#!/bin/bash

set -e
DIR=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

echo run.sh
cd $DIR/tmp/tutorial-selenium-java-basic
mvn -Dtest=BasicDemo test
cd $DIR/tmp/tutorial-selenium-java-ultrafastgrid
mvn install exec:java -Dexec.mainClass="com.applitools.quickstarts.AppTest"  -Dexec.classpathScope=test -Dmaven.compiler.source="1.7" -Dmaven.compiler.target="1.7"