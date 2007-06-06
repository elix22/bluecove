@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd

java -classpath src\site\resources\bluecove-1.2.3-signed.jar;target\bluecove-tester-%VERSION%-app.jar net.sf.bluecove.awt.Main
if errorlevel 1 (
    echo Error calling java
    pause
)

ENDLOCAL