SETLOCAL
SET JAVA_HOME=D:\jdk1.1.8
call %~dp0scripts\version.cmd

set JVM_ARGS=
set JVM_ARGS=%JVM_ARGS% -Dbluecove.connect.timeout=10000

call %~dp0scripts\set-stack.cmd

%JAVA_HOME%\bin\java %JVM_ARGS% -Djava.compiler=NONE -classpath %JAVA_HOME%\lib\classes.zip;target\bluecove-tester-%VERSION%-app.jar net.sf.bluecove.awt.Main
ENDLOCAL
pause