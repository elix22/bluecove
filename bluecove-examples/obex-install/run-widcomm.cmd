@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\bluecove-tester\scripts\version.cmd
SET STACK=widcomm
title %STACK%-tester
java -Dbluecove.stack=%STACK% -jar target\obex-install-%VERSION%-mini.jar
if errorlevel 2 (
    echo Error calling java
)
pause java ends.
ENDLOCAL