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

rem set ARGS=-Xdomain:trusted
set ARGS=-Xdomain:untrusted

..\..\..\..\..\..\tools\WTK22\bin\emulator.exe %ARGS% -Xautotest:http://localhost:8080/getNextApp.jad

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
