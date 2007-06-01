@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd

java -cp ..\..\3p\avetanaBluetooth\avetanaBluetooth.jar;target\bluecove-tester-%VERSION%.jar;"%MAVEN2_REPO%\junit\junit\3.8.1\junit-3.8.1.jar" net.sf.bluecove.awt.Main
if errorlevel 2 (
    echo Error calling java
)
pause java ends.
ENDLOCAL