#!/bin/sh

# Build modules relevant for retro-crawler-cli in the correct order,
# skipping tests. Must be executed from parent pom, NOT retro-crawler-cli pom.
# To do this we target the parent pom via -f
mvn -f ../pom.xml -pl retro-crawler-cli -am -DskipTests clean install

# Start command line demo via Java main. Must be executed from the app pom.
mvn -f ../retro-crawler-cli exec:java