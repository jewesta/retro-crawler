@echo off
setlocal

set "REPO_DIR=%~dp0.."
set "DEVTOOLS_DIR=%WESTARPS_DEVTOOLS_HOME%"

if not defined DEVTOOLS_DIR (
	for /f "usebackq delims=" %%I in (`git -C "%REPO_DIR%" config --local --get westarps.devtools.path 2^>nul`) do set "DEVTOOLS_DIR=%%I"
)

if not defined DEVTOOLS_DIR (
	set "DEVTOOLS_DIR=%REPO_DIR%\..\devtools"
) else (
	if not "%DEVTOOLS_DIR:~1,1%"==":" if not "%DEVTOOLS_DIR:~0,2%"=="\\" set "DEVTOOLS_DIR=%REPO_DIR%\%DEVTOOLS_DIR%"
)

set "LAUNCHER=%DEVTOOLS_DIR%\run\prettify.bat"
if not exist "%LAUNCHER%" (
	echo Prettify launcher not found at: %LAUNCHER% 1>&2
	echo Clone https://github.com/jewesta/devtools.git beside RetroCrawler, 1>&2
	echo set WESTARPS_DEVTOOLS_HOME, or configure westarps.devtools.path locally. 1>&2
	exit /b 2
)

call "%LAUNCHER%" --repo "%REPO_DIR%" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
