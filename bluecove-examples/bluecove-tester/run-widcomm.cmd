@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd
SET STACK=widcomm
title %STACK%-tester

set JVM_ARGS=
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.connect.timeout=10000

java %JVM_ARGS% -Dbluecove.stack=%STACK% -jar target\bluecove-tester-%VERSION%-app.jar >  run-%STACK%.cmd.log
if errorlevel 2 (
    echo Error calling java
)
pause java ends.
ENDLOCAL