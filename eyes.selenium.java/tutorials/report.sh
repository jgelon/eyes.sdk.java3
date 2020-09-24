#!/bin/bash
sdk='java'
id=${APPLITOOLS_REPORT_ID:-$(uuidgen)}
echo $RELEASE_CANDIDATE
if [[ $TRAVIS_TAG =~ ^RELEASE_CANDIDATE ]]; then
sandbox='false';
else
sandbox='true'
fi
payload='{
      "sdk":"'"$sdk"'",
      "group":"selenium",
      "id":"'$id'",
      "sandbox":'$sandbox',
      "mandatory":false,
      "results": [
        {
          "test_name": "tutorial_basic",
          "passed": true
        },
        {
          "test_name": "tutorial_ultrafastgrid",
          "passed": true
        }
      ]
}'
echo $payload

curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X POST --data "$payload" "http://sdk-test-results.herokuapp.com/result"
