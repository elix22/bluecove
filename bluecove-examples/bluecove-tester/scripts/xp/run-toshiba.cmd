@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd
SET STACK=toshiba
title %STACK%-tester
java -Dbluecove.stack=%STACK% -jar target\bluecove-tester-%VERSION%-app.jar >  run-%STACK%.cmd.log
if errorlevel 2 (
    echo Error calling java
)
pause java ends.
ENDLOCAL