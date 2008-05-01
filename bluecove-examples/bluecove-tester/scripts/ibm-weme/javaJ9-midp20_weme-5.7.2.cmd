@SETLOCAL
call %~dp0scripts\version.cmd


@set JAVA_HOME=%ProgramFiles%\IBM\DeviceDeveloper\wsdd5.0\ive-2.2

set JVM_ARGS=

set JVM_ARGS=%JVM_ARGS% -jcl:midp20
rem set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth

set JVM_ARGS=%JVM_ARGS% -Dcom.ibm.oti.vm.bootstrap.library.path=%~dp0..\..\bluecove\src\main\resources;%JAVA_HOME%\bin

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.native.path=%~dp0..\..\bluecove\src\main\resources

rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1

call %~dp0scripts\set-stack.cmd

@rem -cp ..\..\bluecove\target\bluecove-%VERSION%.jar
copy ..\..\bluecove\target\bluecove-%VERSION%.jar "%JAVA_HOME%\lib\jclMidp20\ext\bluecove.jar"

title J9 MIDP20-v5.7.2

"%JAVA_HOME%\bin\j9.exe" %JVM_ARGS% -cp target\bctest.jar "-jxe:%JAVA_HOME%\lib\jclMidp20\jclMidp20.jxe" target\bctest.jad

@ENDLOCAL
@pause