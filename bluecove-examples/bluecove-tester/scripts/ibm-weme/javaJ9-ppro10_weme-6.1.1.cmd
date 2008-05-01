@SETLOCAL
call %~dp0scripts\version.cmd

@SET JAVA_HOME=%ProgramFiles%\IBM\WEME\runtimes\61\win-x86-ppro10

set JVM_ARGS=
set JVM_ARGS=%JVM_ARGS% -jcl:ppro10

set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

title J9 PPRO1.0-v6.1.1

%JAVA_HOME%\bin\j9 %JVM_ARGS% -classpath target\bluecove-tester-%VERSION%-app.jar net.sf.bluecove.awt.Main
@ENDLOCAL
@pause