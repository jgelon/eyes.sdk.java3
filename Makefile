SHELL := /bin/bash

ifndef VERSION
    VERSION := $(shell git rev-parse HEAD)
endif

## MVN = mvn -Drelease.version=$(VERSION)
MVN = mvn

.PHONY: clean
clean:
	$(MVN) clean

.PHONY: build
build:
	$(MVN) package -DskipTests=true

.PHONY: test
test:
	echo "Tests are not updated, so might fail"

.PHONY: pull-original
pull-original:
	git pull https://github.com/applitools/eyes.sdk.java3.git develop

.PHONY: update
update:
	git remote update
	git submodule update --recursive

