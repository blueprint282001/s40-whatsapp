@echo off
echo ====================================================
echo  Building Nokia S40 WhatsApp Client (.JAR / .JAD)
echo ====================================================

if not exist bin mkdir bin
if not exist build mkdir build

echo [1/3] Compiling Java ME sources...
dir /s /b client-j2me\src\*.java > sources.txt
javac -source 1.7 -target 1.7 -cp "lib\cldcapi11.jar;lib\midpapi20.jar" -d bin @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [2/3] Packaging WhatsApp-S40.jar...
jar cvfm build\WhatsApp-S40.jar client-j2me\META-INF\MANIFEST.MF -C bin . -C client-j2me\res .
if %ERRORLEVEL% NEQ 0 (
    echo Jar packaging failed!
    exit /b %ERRORLEVEL%
)

echo [3/3] Generating JAD descriptor...
for %%I in (build\WhatsApp-S40.jar) do set JAR_SIZE=%%~zI

(
echo MIDlet-Name: WhatsApp S40
echo MIDlet-Version: 2.16.5
echo MIDlet-Vendor: WhatsApp Inc.
echo MIDlet-1: WhatsApp, /icon.png, com.whatsapp.s40.WhatsAppMIDlet
echo MicroEdition-Profile: MIDP-2.0
echo MicroEdition-Configuration: CLDC-1.1
echo MIDlet-Jar-URL: WhatsApp-S40.jar
echo MIDlet-Jar-Size: %JAR_SIZE%
echo MIDlet-Description: Official WhatsApp client for Nokia Series 40 with Multi-Device Cloud Link.
) > build\WhatsApp-S40.jad

echo.
echo Build successful!
echo Output:
echo   - build\WhatsApp-S40.jar (%JAR_SIZE% bytes)
echo   - build\WhatsApp-S40.jad
echo ====================================================
