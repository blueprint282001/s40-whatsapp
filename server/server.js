const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const QRCode = require('qrcode');
const pino = require('pino');
const {
    default: makeWASocket,
    useMultiFileAuthState,
    DisconnectReason,
    fetchLatestBaileysVersion
} = require('@whiskeysockets/baileys');

const app = express();
const PORT = process.env.PORT || 3000;
const AUTH_DIR = path.join(__dirname, 'session_auth');

if (!fs.existsSync(AUTH_DIR)) {
    fs.mkdirSync(AUTH_DIR, { recursive: true });
}

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// State variables
let sock = null;
let currentQR = null;
let currentPairingCode = null;
let connectionStatus = 'connecting'; // 'connecting' | 'connected' | 'disconnected'
let userProfile = null;
let isConnecting = false;
let reconnectTimer = null;

// Helper: Format JID to friendly display
function formatJidName(jid, pushName) {
    if (pushName && pushName.trim().length > 0) return pushName;
    if (!jid) return 'Unknown';
    if (jid.includes('@g.us')) return 'Group (' + jid.split('@')[0].slice(-4) + ')';
    const number = jid.split('@')[0];
    return '+' + number;
}

// Helper: Extract text from Baileys message
function extractMessageText(msg) {
    if (!msg || !msg.message) return '';
    const m = msg.message;
    if (m.conversation) return m.conversation;
    if (m.extendedTextMessage && m.extendedTextMessage.text) return m.extendedTextMessage.text;
    if (m.imageMessage && m.imageMessage.caption) return '[Photo] ' + m.imageMessage.caption;
    if (m.imageMessage) return '[Photo]';
    if (m.videoMessage) return '[Video]';
    if (m.audioMessage) return '[Voice Note]';
    if (m.documentMessage) return '[Document]';
    if (m.stickerMessage) return '[Sticker]';
    if (m.contactMessage) return '[Contact]';
    if (m.locationMessage) return '[Location]';
    return '[Media / Message]';
}

function cleanupSocket() {
    if (sock) {
        try {
            sock.ev.removeAllListeners('connection.update');
            sock.ev.removeAllListeners('creds.update');
            sock.ev.removeAllListeners('messages.upsert');
            sock.end(undefined);
        } catch (e) {}
        sock = null;
    }
}

// Initialize WhatsApp Baileys connection
async function initWhatsApp() {
    if (isConnecting) {
        console.log('[WhatsApp] Already initializing, skipping duplicate call.');
        return;
    }
    isConnecting = true;

    try {
        cleanupSocket();

        const { state, saveCreds } = await useMultiFileAuthState(AUTH_DIR);
        const { version, isLatest } = await fetchLatestBaileysVersion();
        console.log(`[WhatsApp] Using WA version v${version.join('.')}, isLatest: ${isLatest}`);

        sock = makeWASocket({
            version,
            auth: state,
            printQRInTerminal: false,
            logger: pino({ level: 'silent' }),
            browser: ['Nokia S40 Companion', 'Chrome', '1.0.0'],
            syncFullHistory: false
        });

        sock.ev.on('creds.update', saveCreds);

        sock.ev.on('connection.update', async (update) => {
            const { connection, lastDisconnect, qr } = update;

            if (qr) {
                currentQR = qr;
                connectionStatus = 'awaiting_scan_or_code';
                console.log('[WhatsApp] New QR generated. Awaiting scan or pairing code.');
            }

            if (connection === 'close') {
                const statusCode = lastDisconnect?.error?.output?.statusCode;
                const isLoggedOut = statusCode === DisconnectReason.loggedOut;
                const isReplaced = statusCode === DisconnectReason.connectionReplaced;
                connectionStatus = 'disconnected';
                console.log(`[WhatsApp] Connection closed (status: ${statusCode}). Replaced: ${isReplaced}, LoggedOut: ${isLoggedOut}`);

                cleanupSocket();

                if (isLoggedOut) {
                    console.log('[WhatsApp] Logged out. Cleaning session...');
                    try {
                        fs.rmSync(AUTH_DIR, { recursive: true, force: true });
                    } catch (e) {}
                    currentQR = null;
                    currentPairingCode = null;
                    userProfile = null;
                    clearTimeout(reconnectTimer);
                    reconnectTimer = setTimeout(initWhatsApp, 3000);
                } else if (isReplaced) {
                    console.log('[WhatsApp] Connection replaced by another session. Pausing 10s...');
                    clearTimeout(reconnectTimer);
                    reconnectTimer = setTimeout(initWhatsApp, 10000);
                } else {
                    clearTimeout(reconnectTimer);
                    reconnectTimer = setTimeout(initWhatsApp, 4000);
                }
            } else if (connection === 'open') {
                connectionStatus = 'connected';
                currentQR = null;
                currentPairingCode = null;
                userProfile = sock.user;
                console.log(`[WhatsApp] Connected successfully as ${sock.user?.name || sock.user?.id}`);
            }
        });

        // Handle incoming & outgoing messages
        sock.ev.on('messages.upsert', async ({ messages, type }) => {
            for (const msg of messages) {
                if (!msg.message) continue;
                const jid = msg.key.remoteJid;
                if (!jid || jid === 'status@broadcast') continue;

                const text = extractMessageText(msg);
                const fromMe = Boolean(msg.key.fromMe);
                const senderName = msg.pushName || formatJidName(jid);
                const timestamp = msg.messageTimestamp ? Number(msg.messageTimestamp) * 1000 : Date.now();
                const msgId = msg.key.id;

                const messageItem = {
                    id: msgId,
                    jid: jid,
                    sender: fromMe ? 'You' : senderName,
                    text: text,
                    fromMe: fromMe,
                    timestamp: timestamp,
                    status: msg.status || 2
                };

                // Add to messages cache
                if (!messagesMap.has(jid)) {
                    messagesMap.set(jid, []);
                }
                const msgList = messagesMap.get(jid);
                msgList.push(messageItem);
                if (msgList.length > 50) msgList.shift(); // keep last 50

                // Update chat list summary
                const existingChat = chatsMap.get(jid) || {
                    id: jid,
                    name: formatJidName(jid, msg.pushName),
                    unreadCount: 0
                };

                existingChat.lastMessage = text;
                existingChat.timestamp = timestamp;
                if (!fromMe) {
                    existingChat.unreadCount = (existingChat.unreadCount || 0) + 1;
                }
                chatsMap.set(jid, existingChat);
            }
        });

    } catch (err) {
        console.error('[WhatsApp] Initialization error:', err);
        clearTimeout(reconnectTimer);
        reconnectTimer = setTimeout(initWhatsApp, 5000);
    } finally {
        isConnecting = false;
    }
}

// Start WhatsApp socket
initWhatsApp();

// ================= API ENDPOINTS ================= //

// Status & Info
app.get('/api/status', async (req, res) => {
    let qrDataUrl = null;
    if (currentQR) {
        try {
            qrDataUrl = await QRCode.toDataURL(currentQR);
        } catch (e) {}
    }

    res.json({
        status: connectionStatus,
        connected: connectionStatus === 'connected',
        user: userProfile,
        pairingCode: currentPairingCode,
        hasQr: Boolean(currentQR),
        qrDataUrl: qrDataUrl,
        timestamp: Date.now()
    });
});

// Request 8-character Pairing Code for phone number
app.post('/api/pair', async (req, res) => {
    let { phone } = req.body;
    if (!phone) {
        return res.status(400).json({ success: false, error: 'Phone number required' });
    }

    // Sanitize phone: digits only, e.g. "919876543210"
    phone = phone.replace(/[^0-9]/g, '');

    if (!sock) {
        return res.status(503).json({ success: false, error: 'WhatsApp socket not ready' });
    }

    try {
        console.log(`[WhatsApp] Requesting pairing code for phone: ${phone}`);
        const code = await sock.requestPairingCode(phone);
        currentPairingCode = code;
        console.log(`[WhatsApp] Pairing code generated: ${code}`);
        return res.json({
            success: true,
            pairingCode: code,
            instructions: 'Enter this 8-character code in WhatsApp on your phone: Linked Devices -> Link with phone number'
        });
    } catch (err) {
        console.error('[WhatsApp] Pairing code error:', err);
        return res.status(500).json({ success: false, error: err.message || 'Failed to request pairing code' });
    }
});

// Return QR code image
app.get('/api/qr', async (req, res) => {
    if (!currentQR) {
        return res.status(404).send('No QR code available or device already connected.');
    }
    try {
        res.setHeader('Content-Type', 'image/png');
        const buffer = await QRCode.toBuffer(currentQR, { width: 300, margin: 2 });
        res.send(buffer);
    } catch (e) {
        res.status(500).send('Error generating QR image');
    }
});

// Get Chats list (ordered by recent timestamp)
app.get('/api/chats', (req, res) => {
    const list = Array.from(chatsMap.values()).sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    
    // Format compact payload for J2ME
    const chats = list.map(c => ({
        id: c.id,
        name: c.name || 'Chat',
        lastMessage: c.lastMessage || '',
        timestamp: c.timestamp || 0,
        unread: c.unreadCount || 0
    }));

    res.json({
        success: true,
        count: chats.length,
        chats: chats
    });
});

// Get Messages for a specific chat
app.get('/api/messages', (req, res) => {
    const chatId = req.query.chatId;
    if (!chatId) {
        return res.status(400).json({ success: false, error: 'chatId is required' });
    }

    // Reset unread count for this chat
    if (chatsMap.has(chatId)) {
        chatsMap.get(chatId).unreadCount = 0;
    }

    const messages = messagesMap.get(chatId) || [];
    res.json({
        success: true,
        chatId: chatId,
        count: messages.length,
        messages: messages
    });
});

// Send a Text Message
app.post('/api/send', async (req, res) => {
    let { chatId, message } = req.body;
    if (!chatId || !message) {
        return res.status(400).json({ success: false, error: 'chatId and message are required' });
    }

    if (!sock || connectionStatus !== 'connected') {
        return res.status(503).json({ success: false, error: 'WhatsApp not connected' });
    }

    // Ensure proper JID suffix
    if (!chatId.includes('@')) {
        chatId = chatId.replace(/[^0-9]/g, '') + '@s.whatsapp.net';
    }

    try {
        const sent = await sock.sendMessage(chatId, { text: message });
        const timestamp = Date.now();

        const messageItem = {
            id: sent.key.id,
            jid: chatId,
            sender: 'You',
            text: message,
            fromMe: true,
            timestamp: timestamp,
            status: 2
        };

        if (!messagesMap.has(chatId)) {
            messagesMap.set(chatId, []);
        }
        messagesMap.get(chatId).push(messageItem);

        // Update chat list
        const existingChat = chatsMap.get(chatId) || {
            id: chatId,
            name: formatJidName(chatId),
            unreadCount: 0
        };
        existingChat.lastMessage = message;
        existingChat.timestamp = timestamp;
        chatsMap.set(chatId, existingChat);

        res.json({
            success: true,
            messageId: sent.key.id,
            timestamp: timestamp
        });
    } catch (err) {
        console.error('[WhatsApp] Send error:', err);
        res.status(500).json({ success: false, error: err.message || 'Failed to send' });
    }
});

// Unlink / Logout
app.post('/api/logout', async (req, res) => {
    try {
        if (sock) {
            await sock.logout();
        }
        try {
            fs.rmSync(AUTH_DIR, { recursive: true, force: true });
        } catch (e) {}
        currentQR = null;
        currentPairingCode = null;
        userProfile = null;
        chatsMap.clear();
        messagesMap.clear();
        connectionStatus = 'disconnected';
        setTimeout(initWhatsApp, 1500);
        res.json({ success: true, message: 'Logged out successfully' });
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
});

// Serve direct download of J2ME .jar and .jad
app.get('/download/WhatsApp-S40.jar', (req, res) => {
    const jarPath = path.resolve(__dirname, '..', 'build', 'WhatsApp-S40.jar');
    if (fs.existsSync(jarPath)) {
        res.setHeader('Content-Type', 'application/java-archive');
        res.setHeader('Content-Disposition', 'attachment; filename="WhatsApp-S40.jar"');
        const stream = fs.createReadStream(jarPath);
        return stream.pipe(res);
    }
    res.status(404).send('JAR file not compiled yet. Run build script.');
});

app.get('/download/WhatsApp-S40.jad', (req, res) => {
    const jadPath = path.resolve(__dirname, '..', 'build', 'WhatsApp-S40.jad');
    if (fs.existsSync(jadPath)) {
        res.setHeader('Content-Type', 'text/vnd.sun.j2me.app-descriptor');
        res.setHeader('Content-Disposition', 'attachment; filename="WhatsApp-S40.jad"');
        const stream = fs.createReadStream(jadPath);
        return stream.pipe(res);
    }
    res.status(404).send('JAD file not compiled yet. Run build script.');
});

// Simple Health
app.get('/health', (req, res) => {
    res.send('OK');
});

// Start Server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`====================================================`);
    console.log(` Nokia S40 WhatsApp Cloud Bridge Server`);
    console.log(` Running on port: http://localhost:${PORT}`);
    console.log(` Direct Jar Download: http://localhost:${PORT}/download/WhatsApp-S40.jar`);
    console.log(`====================================================`);
});
