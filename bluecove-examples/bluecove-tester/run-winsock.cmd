@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

java -Dbluecove.stack=winsock -jar target\bluecove-tester-2.0.0-SNAPSHOT-app.jar

ENDLOCAL