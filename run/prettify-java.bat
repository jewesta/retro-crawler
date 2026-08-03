@echo off
setlocal

set "REPO_DIR=%~dp0.."
set "TOOL_DIR=%REPO_DIR%\tools\prettify-java"
set "CLASSPATH_FILE=%TOOL_DIR%\target-cli\prettify-java-classpath.txt"

mvn ^
-q ^
-f "%REPO_DIR%\pom.xml" ^
-pl tools/prettify-java ^
-DskipTests ^
-Dmaven.test.skip=true ^
-Dprettify.build.directory=target-cli ^
-Dmdep.outputFile="%CLASSPATH_FILE%" ^
-Dmdep.includeScope=runtime ^
compile ^
org.apache.maven.plugins:maven-dependency-plugin:3.9.0:build-classpath
if errorlevel 1 exit /b 1

set "DEP_CP="
if exist "%CLASSPATH_FILE%" set /p DEP_CP=<"%CLASSPATH_FILE%"

set "CLI_CP=%TOOL_DIR%\target-cli\classes"
if defined DEP_CP set "CLI_CP=%CLI_CP%;%DEP_CP%"

java -cp "%CLI_CP%" com.retrocrawler.tools.prettify.PrettifyJavaCli --repo "%REPO_DIR%" %*
