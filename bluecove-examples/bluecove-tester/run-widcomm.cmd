@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd
java -Dbluecove.stack=widcomm -jar target\bluecove-tester-%VERSION%-app.jar
if errorlevel 2 (
    echo Error calling java
)
pause java ends.
ENDLOCAL