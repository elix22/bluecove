@SETLOCAL
call %~dp0scripts\version.cmd


@SET JAVA_HOME=%ProgramFiles%\IBM\WEME\runtimes\61\win-x86-midp20

set JVM_ARGS=

rem set JVM_ARGS=%JVM_ARGS% -jcl:midp20
rem set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth

rem set JVM_ARGS=%JVM_ARGS% -Dcom.ibm.oti.vm.bootstrap.library.path=%~dp0..\..\bluecove\src\main\resources;%JAVA_HOME%\bin

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.native.path=%~dp0..\..\bluecove\src\main\resources

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

call %~dp0scripts\set-stack.cmd

@rem -cp ..\..\bluecove\target\bluecove-%VERSION%.jar
copy ..\..\bluecove\target\bluecove-%VERSION%.jar "%JAVA_HOME%\lib\jclMidp20\ext\bluecove.jar"

"%JAVA_HOME%\bin\emulator.exe" %JVM_ARGS%

@ENDLOCAL
@pause