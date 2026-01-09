@echo off
setlocal enabledelayedexpansion

REM Build modules relevant for retro-crawler-app in the correct order,
REM skipping tests. Must be executed from parent pom, NOT retro-crawler-app pom.
REM To do this we target the parent pom via -f
mvn -f ..\pom.xml -pl retro-crawler-app -am -DskipTests install
IF ERRORLEVEL 1 (
    echo Build failed.
    exit /b 1
)

REM Start Vaadin app via Spring Boot. Must be executed from the app pom
mvn -f ..\retro-crawler-app\pom.xml spring-boot:run