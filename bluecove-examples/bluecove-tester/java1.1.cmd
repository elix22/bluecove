SETLOCAL
SET JAVA_HOME=D:\jdk1.1.8
call %~dp0scripts\version.cmd
%JAVA_HOME%\bin\java -Djava.compiler=NONE -classpath %JAVA_HOME%\lib\classes.zip;target\bluecove-tester-%VERSION%-app.jar net.sf.bluecove.awt.Main
ENDLOCAL
pause