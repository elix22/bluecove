@echo off
rem @version $Revision$ ($Author$)  $Date$
SETLOCAL

call %~dp0environment.cmd %*
if errorlevel 1 (
    echo Error calling environment.cmd
    endlocal
    pause
    exit /b 1
)

call %GATLING_CORE_ROOT%\bin\SignatureTest.bat %GATLING_CORE_ROOT%\lib  %TCK_JSR82_HOME%\Bluetooth_1-1_005_TCK\SignatureTests\CLDC %BLUECOVE_JAR% -report Bluetooth_1-1_005_TCK_CLDC_Signature-Report.txt -jars %WTK_HOME%\lib\cldcapi10.jar

rem call %GATLING_CORE_ROOT%\bin\SignatureTest.bat %GATLING_CORE_ROOT%\lib %TCK_JSR82_HOME%\Bluetooth_1-1_005_TCK\SignatureTests\CDC %BLUECOVE_JAR% -cdc -report Bluetooth_1-1_005_TCK_CDC_Signature-Report.txt -jars %WTK_HOME%\lib\cldcapi10.jar

call %GATLING_CORE_ROOT%\bin\SignatureTest.bat %GATLING_CORE_ROOT%\lib  %TCK_JSR82_HOME%\OBEX_1-1_005_TCK\SignatureTests\CLDC %BLUECOVE_JAR% -report OBEX_1-1_005_TCK_CLDC_Signature-Report.txt -jars %WTK_HOME%\lib\cldcapi10.jar

if errorlevel 1 goto errormark
echo [Launched OK]
goto endmark
:errormark
	ENDLOCAL
	echo Error in start
	pause
:endmark
ENDLOCAL
