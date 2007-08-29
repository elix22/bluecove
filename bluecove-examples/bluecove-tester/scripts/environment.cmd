@echo off
rem @version $Revision$ ($Author$)  $Date$

call %~dp0version.cmd

set DEFAULT_BUILD_HOME=%~dp0
for /f %%i in ("%DEFAULT_BUILD_HOME%..") do @set DEFAULT_BUILD_HOME=%%~fi

set WMDPT="%ProgramFiles%\Windows Mobile Developer Power Toys"
if exist %WMDPT%\CECopy\cecopy.exe goto pt_found
echo Windows Mobile Developer Power Toys Not Found
goto :errormark

:pt_found

set _JVM_MYSAIFU=true
set WIN_CE_PHONE=true

set ASSEMBLY_ID=app

set WIN_CE_JVM_ARGS=
rem set WIN_CE_JVM_ARGS=%WIN_CE_JVM_ARGS% -Dbluecove.debug=true

if NOT '%WIN_CE_PHONE%' EQU 'true' (
    set BLUECOVE_INSTALL_DIR=\bluecove
)

if '%WIN_CE_PHONE%' EQU 'true' (
    set BLUECOVE_INSTALL_DIR=\Storage\bluecove
    set ASSEMBLY_ID=phone
)

rem set BLUECOVE_INSTALL_DIR=\Storage Card\bluecove


@for /f "tokens=*" %%I in ('CD') do @set CurDir=%%~nI
@title %CurDir%
goto endmark
:errormark
	exit /b 1
:endmark

