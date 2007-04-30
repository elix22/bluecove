SETLOCAL
SET JAVA_HOME=D:\jdk1.1.8
%JAVA_HOME%\bin\java -classpath %JAVA_HOME%\lib\classes.zip;target\bluecove-tester-1.2.3-SNAPSHOT-app.jar net.sf.bluecove.awt.Main
ENDLOCAL
pause