@echo off
echo ====================================================
echo  Building Nokia S40 WhatsApp Client (CLDC 1.1 / MIDP 2.0)
echo ====================================================

if not exist bin mkdir bin
if not exist build mkdir build

echo [1/4] Compiling Java ME sources with ECJ (Target 1.3 / Class Version 45.3)...
dir /s /b client-j2me\src\*.java > sources.txt
java -jar lib\ecj.jar -1.3 -bootclasspath "lib\cldcapi11.jar;lib\midpapi20.jar" -d bin @sources.txt
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [2/4] Preverifying with ProGuard for MicroEdition...
if exist build\WhatsApp-S40.jar del build\WhatsApp-S40.jar
java -jar lib\proguard5.jar -injars bin -outjars build\WhatsApp-S40.jar -libraryjars "lib\cldcapi11.jar;lib\midpapi20.jar" -microedition -dontoptimize -dontobfuscate -keep "public class * extends javax.microedition.midlet.MIDlet"
if %ERRORLEVEL% NEQ 0 (
    echo ProGuard failed!
    exit /b %ERRORLEVEL%
)

echo [3/4] Updating Manifest and Icon...
jar uvfm build\WhatsApp-S40.jar client-j2me\META-INF\MANIFEST.MF -C client-j2me\res .

echo [4/4] Updating JAD descriptor...
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
echo   - build\WhatsApp-S40.jar (%JAR_SIZE% bytes, Class Version 45.3 Preverified)
echo   - build\WhatsApp-S40.jad
echo ====================================================
