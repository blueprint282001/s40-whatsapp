# 📱 Nokia S40 WhatsApp Client & Cloud Bridge

> **Nokia Series 40 (S40) फोन्स (Nokia C3, X2-01, Asha 200/201/205/210/302/515 आदि) के लिए क्लासिक WhatsApp इंटरफेस वाला J2ME क्लाइंट, जो आपके Android या iPhone WhatsApp से "Link Device" के जरिए कनेक्ट हो जाता है!**

---

## 🌟 प्रोजेक्ट की खासियतें (Key Features)

1. **Original Nokia S40 WhatsApp UI:**
   * ऑथेंटिक डार्क ग्रीन हेडर (`#075E54`) और टैब नेविगेशन (`CHATS`).
   * क्लासिक स्पीच बबल्स (इनकमिंग व्हाइट और आउटगोइंग लाइट-ग्रीन `#DCF8C6`).
   * सिंगल टिक (`✓`) और डबल टिक (`✓✓`) डिलीवरी स्टेटस।
   * D-Pad (Up/Down/Center), T9 न्यूमेरिक कीपैड, और QWERTY कीबोर्ड (Nokia C3, Asha 210) सपोर्ट।
2. **Android & Apple (iPhone) Multi-Device Link:**
   * **8-Digit Pairing Code:** Nokia फोन या वेब डैशबोर्ड पर 8-अक्षरों का कोड दिखेगा (जैसे `K7P9-1A2B`)।
   * अपने Android / iPhone में WhatsApp > **Linked Devices** > **Link with phone number** में जाकर यह कोड डालें, और आपका Nokia फोन लिंक हो जाएगा!
   * **QR Code Support:** वेब डैशबोर्ड से QR कोड स्कैन करने का विकल्प भी उपलब्ध है।
3. **Cloud Bridge Server (GitHub & Docker Ready):**
   * मॉडर्न WhatsApp का Multi-Device प्रोटोकॉल (Noise / Curve25519) क्लाउड सर्वर पर रन होता है।
   * Nokia फोन के लिए अल्ट्रा-लाइटवेट HTTP REST API (सिर्फ कुछ किलोबाइट डेटा)।
   * 2G/EDGE, 3G और Wi-Fi पर सुपर-फास्ट।
4. **Direct Download to Phone:**
   * सर्वर अपने आप `WhatsApp-S40.jar` (मात्र ~27 KB) और `WhatsApp-S40.jad` सर्व करता है, जिसे आप Nokia फोन के ब्राउज़र से सीधे डाउनलोड कर सकते हैं।

---

## 📁 प्रोजेक्ट स्ट्रक्चर (Project Structure)

```
s40-whatsapp/
├── client-j2me/                # Nokia S40 Java (J2ME) सोर्स कोड
│   ├── src/com/whatsapp/s40/
│   │   ├── WhatsAppMIDlet.java # मुख्य MIDlet लाइफसाइकिल व RMS सेटिंग्स
│   │   ├── ui/
│   │   │   ├── ChatListCanvas.java     # क्लासिक WhatsApp चैट लिस्ट UI
│   │   │   ├── ConversationCanvas.java # चैट बबल्स और मैसेज सेंडिंग UI
│   │   │   ├── PairingScreen.java      # 8-डिजिट पेयरिंग कोड स्क्रीन
│   │   │   └── SettingsScreen.java     # सर्वर URL कॉन्फ़िगरेशन
│   │   ├── net/HttpHelper.java         # 2G/Wi-Fi HTTP ट्रांसपोर्ट
│   │   └── util/MiniJson.java          # ज़ीरो-डिपेंडेंसी लाइटवेट JSON पार्सर
│   ├── META-INF/MANIFEST.MF
│   └── res/icon.png
│
├── server/                     # Cloud Gateway / Bridge सर्वर
│   ├── server.js               # Baileys WhatsApp Multi-Device + REST API
│   ├── Dockerfile              # क्लाउड डेप्लॉयमेंट के लिए
│   └── public/                 # वेब पेयरिंग पोर्टल और लाइव चैट कंसोल
│       ├── index.html
│       ├── style.css
│       └── app.js
│
├── build/                      # कम्पाइल की गई रेडी-टू-इंस्टॉल फाइल्स
│   ├── WhatsApp-S40.jar        # Nokia फोन पर इंस्टॉल करने वाली JAR फाइल (~27 KB)
│   └── WhatsApp-S40.jad        # JAD डिस्क्रिप्टर फाइल
│
├── .github/workflows/          # GitHub Actions ऑटोमेटेड बिल्ड
│   └── deploy.yml
├── build.bat                   # Windows बिल्ड स्क्रिप्ट
├── build.sh                    # Linux / Mac बिल्ड स्क्रिप्ट
└── docker-compose.yml          # VPS / Cloud 1-क्लिक रन
```

---

## 🚀 1. सर्वर को GitHub और Cloud पर कैसे चलाएं (Server Setup)

### तरीका A: GitHub पर नया रिपॉजिटरी बनाएं और Render / Railway पर 1-Click चलाएं (FREE)
1. **GitHub पर कोड पुश करें:**
   ```bash
   git init
   git add .
   git commit -m "Initial Nokia S40 WhatsApp Bridge"
   git branch -M main
   git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPO>.git
   git push -u origin main
   ```
2. **Render.com / Railway.app पर फ्री में होस्ट करें:**
   * [Render.com](https://render.com) पर जाएं > **New +** > **Web Service** चुनें।
   * अपना GitHub रिपॉजिटरी कनेक्ट करें।
   * **Root Directory:** `server`
   * **Build Command:** `npm install`
   * **Start Command:** `node server.js`
   * Create Web Service पर क्लिक करें! आपको एक फ्री पब्लिक URL मिल जाएगा (जैसे: `https://my-wa-bridge.onrender.com`).

### तरीका B: अपने कंप्यूटर या VPS (Docker) पर चलाएं
```bash
# Docker के जरिए:
docker-compose up -d

# या डायरेक्ट Node.js से:
cd server
npm install
node server.js
```
सर्वर स्टार्ट होने पर `http://localhost:3000` या आपके लोकल IP पर खुल जाएगा।

---

## 📲 2. अपने Android या iPhone WhatsApp से लिंक कैसे करें

1. अपने कंप्यूटर या फोन ब्राउज़र में अपने सर्वर का URL खोलें (जैसे `http://<server-ip>:3000` या Render वाला URL)।
2. **Option A (8-Digit Pairing Code - आसान तरीका):**
   * अपना WhatsApp मोबाइल नंबर डालें (कंट्री कोड के साथ, जैसे `919876543210`) और **Get Pairing Code** दबाएं।
   * स्क्रीन पर 8-डिजिट का कोड दिखेगा (उदा. `ABCD-1234`)।
   * अपने Android या iPhone में WhatsApp खोलें > **Settings** > **Linked Devices** > **Link with phone number instead** पर जाएं।
   * यह 8-डिजिट कोड दर्ज करें।
3. **Option B (QR Code):**
   * सीधे स्क्रीन पर दिख रहा QR कोड अपने Android/iPhone WhatsApp से स्कैन कर लें।
4. लिंक होते ही स्टेटस **Connected** हो जाएगा!

---

## 📱 3. Nokia S40 फोन पर ऐप कैसे इंस्टॉल और इस्तेमाल करें

1. **इंस्टॉल करें:**
   * **तरीका 1 (Direct Browser Download):** Nokia फोन के ब्राउज़र में जाएं और लिखें: `http://<YOUR-SERVER-URL>/download/WhatsApp-S40.jar` और डाउनलोड करके इंस्टॉल करें।
   * **तरीका 2 (Bluetooth / USB):** `build/WhatsApp-S40.jar` और `build/WhatsApp-S40.jad` फाइल को ब्लूटूथ या मेमोरी कार्ड के जरिए Nokia फोन में भेजें और ओपन करें।
2. **ऐप सेटअप:**
   * Nokia फोन में **WhatsApp** ऐप खोलें।
   * **Options** > **Settings** में जाएं और अपने Cloud सर्वर का URL (जैसे `http://my-wa-bridge.onrender.com`) डालकर **Save** करें।
   * अगर पहले से वेब से लिंक नहीं किया है, तो Nokia ऐप में ही **Options** > **Link Account** में जाकर अपना फोन नंबर डालकर 8-डिजिट कोड पा सकते हैं!
3. **चैटिंग करें:**
   * मुख्य स्क्रीन पर सभी हालिया चैट्स, कांटेक्ट के नाम, लास्ट मैसेज और अनरीड बैज दिखेंगे।
   * D-Pad Up/Down से चैट सेलेक्ट करें और Center की दबाकर चैट खोलें।
   * **Type / Send** दबाकर मैसेज टाइप करें और भेजें!
