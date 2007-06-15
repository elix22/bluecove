@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd
SET STACK=bluesoleil
title %STACK%-tester
java -Dbluecove.stack=%STACK% -jar target\bluecove-tester-%VERSION%-app.jar >  run-%STACK%.cmd.log
if errorlevel 1 (
    echo Error calling java
    pause
)

ENDLOCAL