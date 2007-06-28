@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd

set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

java %JVM_ARGS% -jar target\bluecove-tester-%VERSION%-app.jar
if errorlevel 1 (
    echo Error calling java
    pause
)

ENDLOCAL