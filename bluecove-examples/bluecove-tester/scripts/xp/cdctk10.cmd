@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL
call %~dp0..\environment.cmd
if errorlevel 1 goto endmark


@set CDCTK_HOME=%ProgramFiles%\CDCTK10

set JVM_ARGS=
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

title CDCTK 1.0

set CP=%BLUECOVE_TESTER_HOME%\target\classes
set CP=%CP%;%BLUECOVE_PROJECT_HOME%\target\classes
set CP=%CP%;%BLUECOVE_TESTER_HOME%\target\cldcunit.jar
set CP=%CP%;%BLUECOVE_TESTER_HOME%\target\cldcunit-se.jar

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.native.resource=false
rem set JVM_ARGS=%JVM_ARGS% -Djava.library.path=%BLUECOVE_PROJECT_HOME%\src\main\resources

set START_ARGS=-cp %CP% %BLUECOVE_MAIN%
rem set START_ARGS=-jar %BLUECOVE_TESTER_APP_JAR%

%CDCTK_HOME%\bin\emulator %JVM_ARGS% %START_ARGS%

pause
:endmark
ENDLOCAL