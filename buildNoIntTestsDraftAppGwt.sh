#!/bin/bash

#exit script on any error
set -e

echo "Building the Server, without GWT compile at all, run Draft Compile later"
./gradlew clean build -x test -x gwtCompile "$@"

echo "Now running GWT draft compile, only on the main app"
./gradlew :stroom-app-gwt:gwtDraftCompile

