#!/bin/bash

set -e
DIR=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

mkdir $DIR/tmp
cd $DIR/tmp
git clone https://github.com/applitools/tutorial-selenium-java-basic.git
sed -i 's/"APPLITOOLS_API_KEY"/System.getenv("APPLITOOLS_API_KEY")/g' tutorial-selenium-java-basic/src/test/java/com/applitools/quickstarts/BasicDemo.java
git clone https://github.com/applitools/tutorial-selenium-java-ultrafastgrid.git
sed -i 's/"APPLITOOLS_API_KEY"/System.getenv("APPLITOOLS_API_KEY")/g' tutorial-selenium-java-ultrafastgrid/src/test/java/com/applitools/quickstarts/AppTest.java
sed -i 's/new VisualGridRunner(1)/new VisualGridRunner(7)/g' tutorial-selenium-java-ultrafastgrid/src/test/java/com/applitools/quickstarts/AppTest.java
