@SETLOCAL
call %~dp0scripts\version.cmd


@rem set JAVA_HOME=%ProgramFiles%\IBM\DeviceDeveloper\wsdd5.0\ive-2.2\runtimes\win32\x86\ppro10
@set JAVA_HOME=%ProgramFiles%\IBM\DeviceDeveloper\wsdd5.0\ive-2.2

set JVM_ARGS=
set JVM_ARGS=%JVM_ARGS% -jcl:ppro10

set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

call %~dp0scripts\set-stack.cmd

title J9 PPRO1.0-v5.7.2

%JAVA_HOME%\bin\j9 %JVM_ARGS% -classpath target\bluecove-tester-%VERSION%-app.jar net.sf.bluecove.awt.Main

@ENDLOCAL
@pause