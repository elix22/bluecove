@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

java -jar target\bluecove-tester-2.0.0-SNAPSHOT-app.jar
if errorlevel 1 (
    echo Error calling java
    pause
)

ENDLOCAL