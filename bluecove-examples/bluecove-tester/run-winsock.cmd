@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd
SET STACK=winsock
title %STACK%-tester
java -Dbluecove.stack=%STACK% -jar bluecove-tester-%VERSION%-app.jar >  run-%STACK%.cmd.log
if errorlevel 1 (
    echo Error calling java
    pause
)

ENDLOCAL