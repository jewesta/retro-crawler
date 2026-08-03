#!/bin/sh
# Runs the repository's headless Java cleanup and formatter.

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_DIR=$(dirname -- "$SCRIPT_DIR")
TOOL_DIR="$REPO_DIR/tools/prettify-java"
CLASSPATH_FILE="$TOOL_DIR/target-cli/prettify-java-classpath.txt"

mvn \
  -q \
  -f "$REPO_DIR/pom.xml" \
  -pl tools/prettify-java \
  -DskipTests \
  -Dmaven.test.skip=true \
  -Dprettify.build.directory=target-cli \
  -Dmdep.outputFile="$CLASSPATH_FILE" \
  -Dmdep.includeScope=runtime \
  compile \
  org.apache.maven.plugins:maven-dependency-plugin:3.9.0:build-classpath

CLI_CP="$TOOL_DIR/target-cli/classes"
if [ -s "$CLASSPATH_FILE" ]; then
  CLI_CP="$CLI_CP:$(cat "$CLASSPATH_FILE")"
fi

java -cp "$CLI_CP" com.retrocrawler.tools.prettify.PrettifyJavaCli --repo "$REPO_DIR" "$@"
