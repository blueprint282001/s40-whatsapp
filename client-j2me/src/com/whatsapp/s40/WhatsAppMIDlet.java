package com.whatsapp.s40;

import com.whatsapp.s40.ui.ChatListCanvas;
import com.whatsapp.s40.ui.ConversationCanvas;
import com.whatsapp.s40.ui.PairingScreen;
import com.whatsapp.s40.ui.SettingsScreen;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

public class WhatsAppMIDlet extends MIDlet {

    private Display display;
    private ChatListCanvas chatListCanvas;
    private ConversationCanvas currentConversation;
    private String serverUrl = "http://localhost:3000";
    private Timer pollTimer;

    private static final String RS_NAME = "wa_s40_cfg";

    public WhatsAppMIDlet() {
        // Constructor
    }

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
            loadSettings();

            chatListCanvas = new ChatListCanvas(this);
            display.setCurrent(chatListCanvas);
            chatListCanvas.fetchChats();

            startBackgroundPolling();
        }
    }

    protected void pauseApp() {
        // Backgrounded
    }

    protected void destroyApp(boolean unconditional) {
        if (pollTimer != null) {
            pollTimer.cancel();
            pollTimer = null;
        }
    }

    public Display getDisplay() {
        return display;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void showChatList() {
        currentConversation = null;
        display.setCurrent(chatListCanvas);
        chatListCanvas.fetchChats();
    }

    public void openConversation(String chatId, String chatName) {
        currentConversation = new ConversationCanvas(this, chatId, chatName);
        display.setCurrent(currentConversation);
        currentConversation.startPolling();
    }

    public void showPairingScreen() {
        display.setCurrent(new PairingScreen(this));
    }

    public void showSettings() {
        display.setCurrent(new SettingsScreen(this));
    }

    public void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(title, message, null, type);
        alert.setTimeout(3000);
        display.setCurrent(alert, display.getCurrent());
    }

    public void exitApp() {
        destroyApp(true);
        notifyDestroyed();
    }

    private void startBackgroundPolling() {
        pollTimer = new Timer();
        pollTimer.schedule(new TimerTask() {
            public void run() {
                if (display.getCurrent() == chatListCanvas) {
                    chatListCanvas.fetchChats();
                } else if (currentConversation != null && display.getCurrent() == currentConversation) {
                    currentConversation.fetchMessages();
                }
            }
        }, 8000, 8000);
    }

    private void loadSettings() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RS_NAME, true);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length > 0) {
                    serverUrl = new String(data, "UTF-8");
                }
            }
        } catch (Exception e) {
            // Keep default
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
    }

    public void saveServerUrl(String url) {
        this.serverUrl = url;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(RS_NAME, true);
            byte[] data = url.getBytes("UTF-8");
            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (Exception e) {
            // Ignore
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
    }
}
