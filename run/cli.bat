@echo off

REM Build modules relevant for retro-crawler-cli in the correct order,
REM skipping tests. Must be executed from parent pom, NOT retro-crawler-cli pom.
REM To do this we target the parent pom via -f
mvn -f ..\pom.xml -pl retro-crawler-cli -am -DskipTests clean install
IF ERRORLEVEL 1 (
    echo Build failed.
    exit /b 1
)

REM Start CLI via exec-maven-plugin. Must be executed from the cli pom
mvn -f ..\retro-crawler-cli\pom.xml exec:java