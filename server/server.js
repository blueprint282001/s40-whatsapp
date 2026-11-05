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
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(express.static(path.join(__dirname, 'public')));

// State variables
let sock = null;
let currentQR = null;
let currentPairingCode = null;
let connectionStatus = 'connecting'; // 'connecting' | 'connected' | 'disconnected'
let userProfile = null;
let userStatusText = 'Available';
let isConnecting = false;
let reconnectTimer = null;

// In-memory Stores for Nokia S40 Delivery
const chatsMap = new Map();     // jid -> { id, name, lastMessage, timestamp, unreadCount, isGroup }
const messagesMap = new Map();  // jid -> [ { id, text, sender, participantName, fromMe, timestamp, isGroup, isPhoto, isVoice } ]
const contactsMap = new Map();  // jid -> { id, name, phone, status }
const groupsMap = new Map();    // jid -> { id, subject, participantsCount }

const DATA_FILE = path.join(__dirname, 'data_store.json');

function loadDataStore() {
    try {
        if (fs.existsSync(DATA_FILE)) {
            const raw = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
            if (raw.chats) raw.chats.forEach(c => chatsMap.set(c.id, c));
            if (raw.contacts) raw.contacts.forEach(c => contactsMap.set(c.id, c));
            if (raw.messages) {
                for (const [jid, list] of Object.entries(raw.messages)) {
                    messagesMap.set(jid, list);
                }
            }
            if (raw.userStatusText) userStatusText = raw.userStatusText;
            console.log(`[Storage] Loaded ${chatsMap.size} chats, ${contactsMap.size} contacts from disk.`);
        }
    } catch (e) {
        console.error('[Storage] Error loading data_store:', e);
    }
}

function saveDataStore() {
    try {
        const data = {
            chats: Array.from(chatsMap.values()),
            contacts: Array.from(contactsMap.values()),
            messages: Object.fromEntries(messagesMap.entries()),
            userStatusText: userStatusText
        };
        fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2), 'utf8');
    } catch (e) {}
}

loadDataStore();

// Helper: Format JID to friendly display
function formatJidName(jid, pushName) {
    if (pushName && pushName.trim().length > 0) return pushName;
    if (!jid) return 'Unknown';
    if (jid.includes('@g.us')) {
        const grp = groupsMap.get(jid);
        if (grp && grp.subject) return grp.subject;
        return 'Group (' + jid.split('@')[0].slice(-4) + ')';
    }
    const number = jid.split('@')[0].split(':')[0];
    return '+' + number;
}

// Helper: Extract text & metadata from Baileys message
function parseMessageContent(msg) {
    let text = '';
    let isPhoto = false;
    let isVoice = false;
    let isVideo = false;

    if (!msg || !msg.message) return { text, isPhoto, isVoice, isVideo };
    const m = msg.message;

    if (m.conversation) {
        text = m.conversation;
    } else if (m.extendedTextMessage && m.extendedTextMessage.text) {
        text = m.extendedTextMessage.text;
    } else if (m.imageMessage) {
        isPhoto = true;
        text = m.imageMessage.caption ? '[Photo] ' + m.imageMessage.caption : '[Photo]';
    } else if (m.videoMessage) {
        isVideo = true;
        text = m.videoMessage.caption ? '[Video] ' + m.videoMessage.caption : '[Video]';
    } else if (m.audioMessage) {
        isVoice = true;
        text = '[Voice Note]';
    } else if (m.documentMessage) {
        text = '[Document] ' + (m.documentMessage.fileName || 'file');
    } else if (m.stickerMessage) {
        text = '[Sticker]';
    } else if (m.contactMessage) {
        text = '[Contact] ' + (m.contactMessage.displayName || 'Card');
    } else if (m.locationMessage) {
        text = '[Location]';
    } else {
        text = '[Message]';
    }

    return { text, isPhoto, isVoice, isVideo };
}

function cleanupSocket() {
    if (sock) {
        try {
            sock.ev.removeAllListeners('connection.update');
            sock.ev.removeAllListeners('creds.update');
            sock.ev.removeAllListeners('messages.upsert');
            sock.ev.removeAllListeners('contacts.upsert');
            sock.ev.removeAllListeners('contacts.update');
            sock.ev.removeAllListeners('groups.update');
            sock.end(undefined);
        } catch (e) {}
        sock = null;
    }
}

// Initialize WhatsApp Baileys connection
async function initWhatsApp() {
    if (isConnecting) return;
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
            browser: ['Nokia S40 Official', 'Chrome', '1.0.0'],
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

                // Try fetching user status
                try {
                    const statusObj = await sock.fetchStatus(sock.user.id);
                    if (statusObj && statusObj.status) {
                        userStatusText = statusObj.status;
                    }
                } catch (e) {}

                // Sync all participating groups immediately!
                try {
                    const groups = await sock.groupFetchAllParticipating();
                    let grpCount = 0;
                    for (const [gid, grp] of Object.entries(groups)) {
                        grpCount++;
                        groupsMap.set(gid, {
                            id: gid,
                            subject: grp.subject,
                            participantsCount: grp.participants ? grp.participants.length : 0
                        });
                        const existing = chatsMap.get(gid) || {};
                        chatsMap.set(gid, {
                            id: gid,
                            name: grp.subject || 'Group',
                            lastMessage: existing.lastMessage || (grp.desc ? grp.desc.toString().substring(0, 30) : 'Tap to open group'),
                            timestamp: existing.timestamp || Date.now(),
                            unreadCount: existing.unreadCount || 0,
                            isGroup: true
                        });
                    }
                    console.log(`[WhatsApp] Loaded ${grpCount} participating group chats.`);
                    saveDataStore();
                } catch (err) {
                    console.log('[WhatsApp] Group fetch note:', err.message);
                }
            }
        });

        // Sync Recent Chats & Messaging History from Baileys
        sock.ev.on('messaging-history.set', ({ chats, contacts, messages: historyMessages }) => {
            console.log(`[WhatsApp] History set: ${chats?.length || 0} chats, ${contacts?.length || 0} contacts, ${historyMessages?.length || 0} messages.`);
            if (chats) {
                for (const c of chats) {
                    const jid = c.id;
                    if (!jid || jid === 'status@broadcast') continue;
                    const isGroup = jid.endsWith('@g.us');
                    const existing = chatsMap.get(jid) || {};
                    chatsMap.set(jid, {
                        id: jid,
                        name: c.name || existing.name || formatJidName(jid),
                        lastMessage: existing.lastMessage || '',
                        timestamp: c.conversationTimestamp ? Number(c.conversationTimestamp) * 1000 : (existing.timestamp || Date.now()),
                        unreadCount: c.unreadCount || existing.unreadCount || 0,
                        isGroup: isGroup
                    });
                }
            }
            if (contacts) {
                for (const c of contacts) {
                    const jid = c.id;
                    if (!jid || jid.endsWith('@g.us') || jid === 'status@broadcast') continue;
                    const name = c.name || c.notify || formatJidName(jid);
                    const phone = jid.split('@')[0].split(':')[0];
                    contactsMap.set(jid, {
                        id: jid,
                        name: name,
                        phone: '+' + phone,
                        status: 'Hey there! I am using WhatsApp.'
                    });
                }
            }
            if (historyMessages) {
                for (const m of historyMessages) {
                    if (!m.message) continue;
                    const jid = m.key.remoteJid;
                    if (!jid || jid === 'status@broadcast') continue;
                    const isGroup = jid.endsWith('@g.us');
                    const fromMe = Boolean(m.key.fromMe);
                    const parsed = parseMessageContent(m);
                    const timestamp = m.messageTimestamp ? Number(m.messageTimestamp) * 1000 : Date.now();
                    let participantName = fromMe ? 'You' : (m.pushName || formatJidName(m.key.participant || jid));

                    if (!messagesMap.has(jid)) messagesMap.set(jid, []);
                    const list = messagesMap.get(jid);
                    list.push({
                        id: m.key.id,
                        jid: jid,
                        sender: fromMe ? 'You' : participantName,
                        participantName: participantName,
                        text: parsed.text,
                        fromMe: fromMe,
                        timestamp: timestamp,
                        isGroup: isGroup,
                        isPhoto: parsed.isPhoto,
                        isVoice: parsed.isVoice,
                        status: m.status || 2
                    });
                    if (list.length > 50) list.shift();
                }
            }
            saveDataStore();
        });

        // Chat list updates
        sock.ev.on('chats.upsert', (newChats) => {
            for (const c of newChats) {
                const jid = c.id;
                if (!jid || jid === 'status@broadcast') continue;
                const isGroup = jid.endsWith('@g.us');
                const existing = chatsMap.get(jid) || {};
                chatsMap.set(jid, {
                    id: jid,
                    name: c.name || existing.name || formatJidName(jid),
                    lastMessage: existing.lastMessage || '',
                    timestamp: c.conversationTimestamp ? Number(c.conversationTimestamp) * 1000 : (existing.timestamp || Date.now()),
                    unreadCount: c.unreadCount || existing.unreadCount || 0,
                    isGroup: isGroup
                });
            }
            saveDataStore();
        });

        sock.ev.on('chats.update', (updates) => {
            for (const u of updates) {
                if (chatsMap.has(u.id)) {
                    const chat = chatsMap.get(u.id);
                    if (u.unreadCount !== undefined) chat.unreadCount = u.unreadCount;
                    if (u.conversationTimestamp) chat.timestamp = Number(u.conversationTimestamp) * 1000;
                    chatsMap.set(u.id, chat);
                }
            }
            saveDataStore();
        });

        // Contact Sync
        sock.ev.on('contacts.upsert', (contacts) => {
            for (const c of contacts) {
                const jid = c.id;
                if (!jid || jid.includes('@g.us')) continue;
                const name = c.name || c.notify || formatJidName(jid);
                const phone = jid.split('@')[0].split(':')[0];
                contactsMap.set(jid, {
                    id: jid,
                    name: name,
                    phone: '+' + phone,
                    status: 'Hey there! I am using WhatsApp.'
                });
            }
            saveDataStore();
        });

        sock.ev.on('contacts.update', (updates) => {
            for (const u of updates) {
                if (contactsMap.has(u.id)) {
                    const existing = contactsMap.get(u.id);
                    if (u.name) existing.name = u.name;
                    if (u.notify) existing.notify = u.notify;
                    contactsMap.set(u.id, existing);
                }
            }
            saveDataStore();
        });

        // Group Metadata Updates
        sock.ev.on('groups.update', (groups) => {
            for (const g of groups) {
                if (!g.id) continue;
                groupsMap.set(g.id, {
                    id: g.id,
                    subject: g.subject || 'Group',
                    participantsCount: g.participants ? g.participants.length : 0
                });
            }
        });

        // Handle Messages (1-on-1, Groups, Media)
        sock.ev.on('messages.upsert', async ({ messages, type }) => {
            for (const msg of messages) {
                if (!msg.message) continue;
                const jid = msg.key.remoteJid;
                if (!jid || jid === 'status@broadcast') continue;

                const isGroup = jid.endsWith('@g.us');
                const fromMe = Boolean(msg.key.fromMe);
                const parsed = parseMessageContent(msg);

                // Group sender participant
                let participantName = '';
                if (isGroup && !fromMe) {
                    const participantJid = msg.key.participant || jid;
                    participantName = msg.pushName || formatJidName(participantJid);
                } else if (!fromMe) {
                    participantName = msg.pushName || formatJidName(jid);
                } else {
                    participantName = 'You';
                }

                // Auto register contact
                if (!isGroup && !fromMe && !contactsMap.has(jid)) {
                    contactsMap.set(jid, {
                        id: jid,
                        name: msg.pushName || formatJidName(jid),
                        phone: '+' + jid.split('@')[0].split(':')[0],
                        status: 'Hey there! I am using WhatsApp.'
                    });
                }

                const timestamp = msg.messageTimestamp ? Number(msg.messageTimestamp) * 1000 : Date.now();
                const msgId = msg.key.id;

                const messageItem = {
                    id: msgId,
                    jid: jid,
                    sender: fromMe ? 'You' : participantName,
                    participantName: participantName,
                    text: parsed.text,
                    fromMe: fromMe,
                    timestamp: timestamp,
                    isGroup: isGroup,
                    isPhoto: parsed.isPhoto,
                    isVoice: parsed.isVoice,
                    status: msg.status || 2
                };

                // Add to message store
                if (!messagesMap.has(jid)) {
                    messagesMap.set(jid, []);
                }
                const msgList = messagesMap.get(jid);
                msgList.push(messageItem);
                if (msgList.length > 50) msgList.shift();

                // Update chat list summary
                const existingChat = chatsMap.get(jid) || {
                    id: jid,
                    name: formatJidName(jid, msg.pushName),
                    isGroup: isGroup,
                    unreadCount: 0
                };

                existingChat.lastMessage = parsed.text;
                existingChat.timestamp = timestamp;
                existingChat.isGroup = isGroup;
                if (!fromMe) {
                    existingChat.unreadCount = (existingChat.unreadCount || 0) + 1;
                }
                chatsMap.set(jid, existingChat);
            }
            saveDataStore();
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

// 1. Status & Info
app.get('/api/status', async (req, res) => {
    let qrDataUrl = null;
    if (currentQR) {
        try { qrDataUrl = await QRCode.toDataURL(currentQR); } catch (e) {}
    }

    res.json({
        status: connectionStatus,
        connected: connectionStatus === 'connected',
        user: userProfile,
        userStatus: userStatusText,
        pairingCode: currentPairingCode,
        hasQr: Boolean(currentQR),
        qrDataUrl: qrDataUrl,
        timestamp: Date.now()
    });
});

// 2. Request Pairing Code
app.post('/api/pair', async (req, res) => {
    let { phone } = req.body;
    if (!phone) return res.status(400).json({ success: false, error: 'Phone number required' });

    phone = phone.replace(/[^0-9]/g, '');
    if (!sock) return res.status(503).json({ success: false, error: 'Socket not ready' });

    try {
        const code = await sock.requestPairingCode(phone);
        currentPairingCode = code;
        res.json({ success: true, pairingCode: code });
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
});

// 3. Get Chats (Recent 1-on-1 and Groups)
app.get('/api/chats', (req, res) => {
    const list = Array.from(chatsMap.values()).sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    
    const chats = list.map(c => ({
        id: c.id,
        name: c.name || 'Chat',
        lastMessage: c.lastMessage || '',
        timestamp: c.timestamp || 0,
        unread: c.unreadCount || 0,
        isGroup: Boolean(c.isGroup)
    }));

    res.json({ success: true, count: chats.length, chats: chats });
});

// 4. Contact Sync (Phonebook Contacts for Nokia S40)
app.get('/api/contacts', (req, res) => {
    // Ensure all 1-on-1 chats are reflected in contacts list
    for (const [jid, chat] of chatsMap.entries()) {
        if (!jid.endsWith('@g.us') && jid !== 'status@broadcast' && !contactsMap.has(jid)) {
            const phone = jid.split('@')[0].split(':')[0];
            contactsMap.set(jid, {
                id: jid,
                name: chat.name || formatJidName(jid),
                phone: '+' + phone,
                status: 'Hey there! I am using WhatsApp.'
            });
        }
    }

    const list = Array.from(contactsMap.values()).sort((a, b) => (a.name || '').localeCompare(b.name || ''));
    
    // If empty, provide self notes & official support
    if (list.length === 0) {
        list.push(
            { id: 'self', name: 'You (Notes)', phone: userProfile?.id ? '+' + userProfile.id.split('@')[0].split(':')[0] : '', status: userStatusText },
            { id: 'whatsapp_team@s.whatsapp.net', name: 'WhatsApp Support', phone: '+1 555-0199', status: 'Official WhatsApp Help' }
        );
    }

    res.json({ success: true, count: list.length, contacts: list });
});

// 5. WhatsApp Status Updates (Classic Text Statuses)
app.get('/api/status-list', (req, res) => {
    const statuses = [
        { name: 'My Status', status: userStatusText, isMe: true, time: 'Just now' }
    ];

    // Add contacts' statuses
    let idx = 0;
    for (const [jid, contact] of contactsMap.entries()) {
        if (idx++ > 15) break;
        statuses.push({
            name: contact.name,
            status: contact.status || 'Available',
            isMe: false,
            time: 'Yesterday'
        });
    }

    res.json({ success: true, count: statuses.length, statuses: statuses });
});

// 6. User Profile (Settings)
app.get('/api/profile', (req, res) => {
    const phone = userProfile?.id ? '+' + userProfile.id.split('@')[0].split(':')[0] : '';
    const name = userProfile?.name || 'Nokia S40 User';
    res.json({
        success: true,
        name: name,
        phone: phone,
        statusText: userStatusText,
        appVersion: '2.16.5 (Series 40)'
    });
});

// Update Status ("Available", "Busy", custom)
app.post('/api/profile/status', async (req, res) => {
    const { statusText } = req.body;
    if (!statusText) return res.status(400).json({ success: false, error: 'Status text required' });

    userStatusText = statusText;
    try {
        if (sock && connectionStatus === 'connected') {
            await sock.updateProfileStatus(statusText);
        }
        res.json({ success: true, statusText: userStatusText });
    } catch (err) {
        res.json({ success: true, statusText: userStatusText, note: 'Saved locally' });
    }
});

// Update Profile Name
app.post('/api/profile/name', async (req, res) => {
    const { name } = req.body;
    if (!name) return res.status(400).json({ success: false, error: 'Name required' });

    try {
        if (sock && connectionStatus === 'connected') {
            await sock.updateProfileName(name);
        }
        if (userProfile) userProfile.name = name;
        res.json({ success: true, name: name });
    } catch (err) {
        res.json({ success: true, name: name, note: 'Saved locally' });
    }
});

// 7. Get Messages for a Chat
app.get('/api/messages', (req, res) => {
    const chatId = req.query.chatId;
    if (!chatId) return res.status(400).json({ success: false, error: 'chatId required' });

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

// 8. Send Text Message (Supports 1-on-1 and Groups)
app.post('/api/send', async (req, res) => {
    let { chatId, message } = req.body;
    if (!chatId || !message) return res.status(400).json({ success: false, error: 'chatId and message required' });

    if (!sock || connectionStatus !== 'connected') {
        return res.status(503).json({ success: false, error: 'WhatsApp not connected' });
    }

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
            participantName: 'You',
            text: message,
            fromMe: true,
            timestamp: timestamp,
            isGroup: chatId.includes('@g.us'),
            isPhoto: false,
            isVoice: false,
            status: 2
        };

        if (!messagesMap.has(chatId)) messagesMap.set(chatId, []);
        messagesMap.get(chatId).push(messageItem);

        const existingChat = chatsMap.get(chatId) || {
            id: chatId,
            name: formatJidName(chatId),
            isGroup: chatId.includes('@g.us'),
            unreadCount: 0
        };
        existingChat.lastMessage = message;
        existingChat.timestamp = timestamp;
        chatsMap.set(chatId, existingChat);

        res.json({ success: true, messageId: sent.key.id, timestamp: timestamp });
    } catch (err) {
        console.error('[WhatsApp] Send error:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// 9. Send Media / Photo (Base64)
app.post('/api/send-media', async (req, res) => {
    let { chatId, caption, imageBase64 } = req.body;
    if (!chatId) return res.status(400).json({ success: false, error: 'chatId required' });

    if (!sock || connectionStatus !== 'connected') {
        return res.status(503).json({ success: false, error: 'WhatsApp not connected' });
    }

    if (!chatId.includes('@')) {
        chatId = chatId.replace(/[^0-9]/g, '') + '@s.whatsapp.net';
    }

    try {
        let sent;
        if (imageBase64) {
            const buffer = Buffer.from(imageBase64, 'base64');
            sent = await sock.sendMessage(chatId, { image: buffer, caption: caption || '' });
        } else {
            // Default sample photo
            const sampleText = '[Photo sent from Nokia S40] ' + (caption || '');
            sent = await sock.sendMessage(chatId, { text: sampleText });
        }

        const timestamp = Date.now();
        const messageItem = {
            id: sent.key.id,
            jid: chatId,
            sender: 'You',
            participantName: 'You',
            text: '[Photo] ' + (caption || ''),
            fromMe: true,
            timestamp: timestamp,
            isGroup: chatId.includes('@g.us'),
            isPhoto: true,
            isVoice: false,
            status: 2
        };

        if (!messagesMap.has(chatId)) messagesMap.set(chatId, []);
        messagesMap.get(chatId).push(messageItem);

        res.json({ success: true, messageId: sent.key.id, timestamp: timestamp });
    } catch (err) {
        console.error('[WhatsApp] Media send error:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// 10. Logout / Unlink
app.post('/api/logout', async (req, res) => {
    try {
        if (sock) await sock.logout();
        try { fs.rmSync(AUTH_DIR, { recursive: true, force: true }); } catch (e) {}
        currentQR = null;
        currentPairingCode = null;
        userProfile = null;
        chatsMap.clear();
        messagesMap.clear();
        contactsMap.clear();
        connectionStatus = 'disconnected';
        setTimeout(initWhatsApp, 1500);
        res.json({ success: true, message: 'Logged out successfully' });
    } catch (err) {
        res.status(500).json({ success: false, error: err.message });
    }
});

// Download JAR & JAD
app.get('/download/WhatsApp-S40.jar', (req, res) => {
    const jarPath = path.resolve(__dirname, '..', 'build', 'WhatsApp-S40.jar');
    if (fs.existsSync(jarPath)) {
        res.setHeader('Content-Type', 'application/java-archive');
        res.setHeader('Content-Disposition', 'attachment; filename="WhatsApp-S40.jar"');
        return fs.createReadStream(jarPath).pipe(res);
    }
    res.status(404).send('JAR file not compiled yet.');
});

app.get('/download/WhatsApp-S40.jad', (req, res) => {
    const jadPath = path.resolve(__dirname, '..', 'build', 'WhatsApp-S40.jad');
    if (fs.existsSync(jadPath)) {
        res.setHeader('Content-Type', 'text/vnd.sun.j2me.app-descriptor');
        res.setHeader('Content-Disposition', 'attachment; filename="WhatsApp-S40.jad"');
        return fs.createReadStream(jadPath).pipe(res);
    }
    res.status(404).send('JAD file not compiled yet.');
});

// Health check
app.get('/health', (req, res) => res.send('OK'));

// Start Server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`====================================================`);
    console.log(` Nokia S40 Official Feature-Complete Server`);
    console.log(` Running on: http://localhost:${PORT}`);
    console.log(`====================================================`);
});
