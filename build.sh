#!/bin/bash
set -e

echo "===================================================="
echo " Building Nokia S40 WhatsApp Client (.JAR / .JAD)"
echo "===================================================="

mkdir -p bin build

echo "[1/3] Compiling Java ME sources..."
find client-j2me/src -name "*.java" > sources.txt
javac -source 1.7 -target 1.7 -cp "lib/cldcapi11.jar:lib/midpapi20.jar" -d bin @sources.txt

echo "[2/3] Packaging WhatsApp-S40.jar..."
jar cvfm build/WhatsApp-S40.jar client-j2me/META-INF/MANIFEST.MF -C bin . -C client-j2me/res .

echo "[3/3] Generating JAD descriptor..."
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
echo "  - build/WhatsApp-S40.jar (${JAR_SIZE} bytes)"
echo "  - build/WhatsApp-S40.jad"
echo "===================================================="
