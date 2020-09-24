#!/bin/bash

set -e
DIR=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

$DIR/build.sh
echo run.sh
docker run -e APPLITOOLS_API_KEY tutorial_java_basic
docker run -e APPLITOOLS_API_KEY tutorial_java_ufg
$DIR/report.sh