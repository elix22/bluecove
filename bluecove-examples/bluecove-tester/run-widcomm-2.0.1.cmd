@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd

SET STACK=widcomm
title bluecove-2.0.1-tester
java  -Dbluecove.stack=%STACK% -classpath src\site\resources\bluecove-2.0.1-signed.jar;target\bluecove-tester-%VERSION%-app.jar net.sf.bluecove.awt.Main >  run-%STACK%-2.0.1.cmd.log
if errorlevel 1 (
    echo Error calling java
    pause
)

ENDLOCAL