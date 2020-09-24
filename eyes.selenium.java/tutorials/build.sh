#!/bin/bash

set -e
DIR=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd $DIR/../
mvn clean package -Dmaven.test.skip=true
rm ./tutorials/base/eyes-selenium* || true
mv ./target/eyes-selenium-java*jar-with-dependencies.jar ./tutorials/base/
regex='eyes-selenium-java3-([0-9].[0-9]+.[0-9]+)'
[[ $(ls tutorials/base | grep eyes-selenium) =~ $regex ]]
version="${BASH_REMATCH[1]}"
echo Current package version is $version
cd $DIR
docker build --build-arg version=$version -t tutorial_java -f ./base/Dockerfile .
docker build --no-cache -t tutorial_java_basic -f ./basic/Dockerfile .
docker build --no-cache -t tutorial_java_ufg -f ./ultrafastgrid/Dockerfile .
