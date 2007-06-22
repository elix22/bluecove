@SETLOCAL
call %~dp0scripts\version.cmd

@SET JAVA_HOME=%ProgramFiles%\IBM\WEME\runtimes\61\win-x86-ppro10\_jvm\jre

%JAVA_HOME%\bin\java -Dmicroedition.connection.pkgs=com.intel.bluetooth -classpath target\bluecove-tester-%VERSION%-app.jar net.sf.bluecove.awt.Main
@ENDLOCAL
@pause