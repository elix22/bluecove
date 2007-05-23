@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0scripts\version.cmd

set TITLE=bluecove-tester
set ERRORS=0
set SUCCESS=0
:startagain
title %TITLE%  SUCCESS=%SUCCESS% ERRORS=%ERRORS%
java -jar target\bluecove-tester-%VERSION%-app.jar --runonce
if errorlevel 2 (
    echo Error calling java
    set /A ERRORS+=1
    rem pause
    rem exit /b 1
    goto startagain
)
if errorlevel 1 (
    set /A SUCCESS+=1
    goto startagain
)
echo Done SUCCESS=%SUCCESS% ERRORS=%ERRORS%
pause
ENDLOCAL