@SETLOCAL
call %~dp0scripts\version.cmd
@SET JAVA_HOME=%ProgramFiles%\IBM\DeviceDeveloper\wsdd5.0\ive-2.2\runtimes\win32\x86\ppro10
%JAVA_HOME%\bin\j9  -jcl:ppro10 -Dmicroedition.connection.pkgs=com.intel.bluetooth -classpath target\bluecove-tester-%VERSION%-app.jar net.sf.bluecove.awt.Main
@ENDLOCAL
@pause