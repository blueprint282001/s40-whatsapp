// Nokia S40 WhatsApp Cloud Bridge Client Logic

const statusIndicator = document.getElementById('statusIndicator');
const statusText = document.getElementById('statusText');
const phoneInput = document.getElementById('phoneInput');
const btnPair = document.getElementById('btnPair');
const pairingCodeContainer = document.getElementById('pairingCodeContainer');
const pairingCodeVal = document.getElementById('pairingCodeVal');
const qrContainer = document.getElementById('qrContainer');
const qrLoading = document.getElementById('qrLoading');
const qrImage = document.getElementById('qrImage');
const btnLogout = document.getElementById('btnLogout');
const serverUrlDisplay = document.getElementById('serverUrlDisplay');
const messagesList = document.getElementById('messagesList');
const targetJidInput = document.getElementById('targetJidInput');
const testMsgInput = document.getElementById('testMsgInput');
const btnSendTest = document.getElementById('btnSendTest');
const activeChatName = document.getElementById('activeChatName');

// Set current server host URL
serverUrlDisplay.innerText = window.location.origin;

let isConnected = false;
let pollingInterval = null;

// Fetch status periodically
async function checkStatus() {
    try {
        const res = await fetch('/api/status');
        const data = await res.json();

        if (data.connected) {
            isConnected = true;
            statusIndicator.className = 'status-dot connected';
            statusText.innerText = 'Connected (' + (data.user?.name || data.user?.id || 'Linked') + ')';
            
            // Hide pairing elements
            pairingCodeContainer.classList.add('hidden');
            qrLoading.classList.add('hidden');
            qrImage.classList.add('hidden');
            btnLogout.classList.remove('hidden');

            loadRecentChats();
        } else {
            isConnected = false;
            statusIndicator.className = 'status-dot disconnected';
            statusText.innerText = 'Not Connected / Awaiting Link';
            btnLogout.classList.add('hidden');

            if (data.qrDataUrl) {
                qrLoading.classList.add('hidden');
                qrImage.src = data.qrDataUrl;
                qrImage.classList.remove('hidden');
            } else {
                qrLoading.innerText = 'Waiting for QR / Pairing...';
                qrLoading.classList.remove('hidden');
                qrImage.classList.add('hidden');
            }

            if (data.pairingCode) {
                pairingCodeVal.innerText = data.pairingCode;
                pairingCodeContainer.classList.remove('hidden');
            }
        }
    } catch (err) {
        statusIndicator.className = 'status-dot disconnected';
        statusText.innerText = 'Server Offline';
    }
}

// Request 8-digit Pairing Code
btnPair.addEventListener('click', async () => {
    const phone = phoneInput.value.trim();
    if (!phone) {
        alert('Kripya apna phone number country code ke sath daalein (e.g. 919876543210)');
        return;
    }

    btnPair.disabled = true;
    btnPair.innerText = 'Requesting...';

    try {
        const res = await fetch('/api/pair', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phone: phone })
        });
        const data = await res.json();

        if (data.success && data.pairingCode) {
            pairingCodeVal.innerText = data.pairingCode;
            pairingCodeContainer.classList.remove('hidden');
        } else {
            alert('Pairing Code error: ' + (data.error || 'Check server logs'));
        }
    } catch (e) {
        alert('Server request failed');
    } finally {
        btnPair.disabled = false;
        btnPair.innerText = 'Get Pairing Code';
    }
});

// Logout / Unlink
btnLogout.addEventListener('click', async () => {
    if (!confirm('Kya aap is WhatsApp session ko unlink karna chahte hain?')) return;
    try {
        await fetch('/api/logout', { method: 'POST' });
        checkStatus();
    } catch (e) {
        alert('Logout error');
    }
});

// Load recent chats & preview
async function loadRecentChats() {
    try {
        const res = await fetch('/api/chats');
        const data = await res.json();

        if (data.success && data.chats && data.chats.length > 0) {
            const firstChat = data.chats[0];
            activeChatName.innerText = firstChat.name;
            if (!targetJidInput.value) {
                targetJidInput.value = firstChat.id.replace('@s.whatsapp.net', '');
            }
            loadMessages(firstChat.id);
        }
    } catch (e) {}
}

// Load messages for chat
async function loadMessages(chatId) {
    try {
        const res = await fetch(`/api/messages?chatId=${encodeURIComponent(chatId)}`);
        const data = await res.json();

        if (data.success && data.messages) {
            messagesList.innerHTML = '';
            if (data.messages.length === 0) {
                messagesList.innerHTML = '<div class="msg-placeholder">No messages yet.</div>';
                return;
            }

            data.messages.forEach(msg => {
                const bubble = document.createElement('div');
                bubble.className = 'msg-bubble ' + (msg.fromMe ? 'out' : 'in');
                const timeStr = new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                bubble.innerHTML = `<div>${escapeHtml(msg.text)}</div><div class="msg-meta">${timeStr}</div>`;
                messagesList.appendChild(bubble);
            });
            messagesList.scrollTop = messagesList.scrollHeight;
        }
    } catch (e) {}
}

// Send test message
btnSendTest.addEventListener('click', async () => {
    const rawTarget = targetJidInput.value.trim();
    const msg = testMsgInput.value.trim();

    if (!rawTarget || !msg) {
        alert('Recipient number aur message dono enter karein');
        return;
    }

    const chatId = rawTarget.includes('@') ? rawTarget : rawTarget + '@s.whatsapp.net';

    try {
        const res = await fetch('/api/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ chatId: chatId, message: msg })
        });
        const data = await res.json();
        if (data.success) {
            testMsgInput.value = '';
            loadMessages(chatId);
        } else {
            alert('Send error: ' + data.error);
        }
    } catch (e) {
        alert('Network error sending message');
    }
});

function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// Polling loop
checkStatus();
pollingInterval = setInterval(checkStatus, 3000);
