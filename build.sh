#!/bin/bash
set -e

echo "===================================================="
echo " Building Nokia S40 WhatsApp Client (CLDC 1.1 / MIDP 2.0)"
echo "===================================================="

mkdir -p bin build

echo "[1/4] Compiling Java ME sources with ECJ (Target 1.3 / Class Version 45.3)..."
find client-j2me/src -name "*.java" > sources.txt
java -jar lib/ecj.jar -1.3 -bootclasspath "lib/cldcapi11.jar:lib/midpapi20.jar" -d bin @sources.txt

echo "[2/4] Preverifying with ProGuard for MicroEdition..."
rm -f build/WhatsApp-S40.jar
java -jar lib/proguard5.jar -injars bin -outjars build/WhatsApp-S40.jar -libraryjars "lib/cldcapi11.jar:lib/midpapi20.jar" -microedition -dontoptimize -dontobfuscate -keep "public class * extends javax.microedition.midlet.MIDlet"

echo "[3/4] Updating Manifest and Icon..."
jar uvfm build/WhatsApp-S40.jar client-j2me/META-INF/MANIFEST.MF -C client-j2me/res .

echo "[4/4] Updating JAD descriptor..."
JAR_SIZE=$(wc -c < build/WhatsApp-S40.jar | tr -d ' ')

cat <<EOF > build/WhatsApp-S40.jad
MIDlet-Name: WhatsApp S40
MIDlet-Version: 2.16.5
MIDlet-Vendor: WhatsApp Inc.
MIDlet-1: WhatsApp, /icon.png, com.whatsapp.s40.WhatsAppMIDlet
MicroEdition-Profile: MIDP-2.0
MicroEdition-Configuration: CLDC-1.1
MIDlet-Jar-URL: WhatsApp-S40.jar
MIDlet-Jar-Size: ${JAR_SIZE}
MIDlet-Description: Official WhatsApp client for Nokia Series 40 with Multi-Device Cloud Link.
EOF

echo ""
echo "Build successful!"
echo "Output:"
echo "  - build/WhatsApp-S40.jar (${JAR_SIZE} bytes, Class Version 45.3 Preverified)"
echo "  - build/WhatsApp-S40.jad"
echo "===================================================="
