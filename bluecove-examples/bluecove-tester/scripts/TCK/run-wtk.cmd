@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

call %~dp0environment.cmd %*
if errorlevel 1 (
    echo Error calling environment.cmd
    endlocal
    pause
    exit /b 1
)

echo WTK_HOME=[%WTK_HOME%]

rem set ARGS=-Xdomain:trusted
rem set ARGS=-Xdomain:untrusted
set ARGS=-Xdomain:manufacturer

rem set ARGS=%ARGS% -Xverbose:class
rem set ARGS=%ARGS% -Xverbose:exceptions

title TCK tests on Sun WTK

set GATLING_HOST=localhost

%WTK_HOME%\bin\emulator.exe %ARGS% -Xautotest:http://%GATLING_HOST%:8080/getNextApp.jad

if errorlevel 1 goto errormark
echo [Launched OK]
pause
goto endmark
:errormark
	ENDLOCAL
	echo Error in start
	pause
:endmark
ENDLOCAL
