@SETLOCAL
@call %~dp0scripts\version.cmd
rem echo on

@SET JAVA_HOME=%ProgramFiles%\IBM\WEME\runtimes\61\win-x86-midp20

set JVM_ARGS=

set JVM_ARGS=%JVM_ARGS% -jcl:midp20
set JVM_ARGS=%JVM_ARGS% -Xbootclasspath:%JAVA_HOME%\lib\jclMidp20\jclMidp20.jxe
rem set JVM_ARGS=%JVM_ARGS% -jxe %JAVA_HOME%\lib\jclMidp20\jclMidp20.jxe

rem set JVM_ARGS=%JVM_ARGS% -Dmicroedition.connection.pkgs=com.intel.bluetooth
rem set JVM_ARGS=%JVM_ARGS% -Dbluecove.debug=1


copy ..\..\bluecove\target\bluecove-%VERSION%.jar "%JAVA_HOME%\lib\jclMidp20\ext\bluecove.jar"
copy ..\..\bluecove\src\main\resources\*.dll "%JAVA_HOME%\bin"

title J9 MIDP20-v6.1.1

%JAVA_HOME%\bin\j9 %JVM_ARGS% -cp target\bctest.jar javax.microedition.lcdui.AppManager target\bctest.jad
@ENDLOCAL
@pause