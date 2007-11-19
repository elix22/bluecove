@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd
SET STACK=widcomm
title %STACK%-tester

set JVM_ARGS=
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.connect.timeout=10000

set BLUECOVE_PROJECT=%~dp0..\..\bluecove

set CP=target\classes;%BLUECOVE_PROJECT%\target\classes;target\cldcunit.jar;target\cldcunit-se.jar

set START_ARGS=-cp %CP% -Dbluecove.native.path=%BLUECOVE_PROJECT%/src/main/resources net.sf.bluecove.awt.Main
rem set START_ARGS=-jar target\bluecove-tester-%VERSION%-app.jar

java %JVM_ARGS% -Dbluecove.stack=%STACK% %START_ARGS% >  run-%STACK%.cmd.log
if errorlevel 2 (
    echo Error calling java
)
pause java ends.
ENDLOCAL