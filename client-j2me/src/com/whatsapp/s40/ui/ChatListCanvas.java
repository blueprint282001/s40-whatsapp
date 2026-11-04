package com.whatsapp.s40.ui;

import com.whatsapp.s40.WhatsAppMIDlet;
import com.whatsapp.s40.net.HttpHelper;
import com.whatsapp.s40.util.MiniJson;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class ChatListCanvas extends Canvas implements CommandListener {

    private WhatsAppMIDlet midlet;
    private Vector chatList = new Vector();
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private boolean isLoading = false;
    private String statusMsg = "Syncing...";

    // Commands
    private Command cmdOpen;
    private Command cmdRefresh;
    private Command cmdLink;
    private Command cmdSettings;
    private Command cmdExit;

    // S40 WhatsApp Theme Colors
    private static final int COLOR_HEADER_BG = 0x075E54;
    private static final int COLOR_HEADER_TEXT = 0xFFFFFF;
    private static final int COLOR_SUBHEADER_BG = 0x128C7E;
    private static final int COLOR_BG = 0xFFFFFF;
    private static final int COLOR_ROW_HIGHLIGHT = 0xE1F3E9;
    private static final int COLOR_TEXT_NAME = 0x111111;
    private static final int COLOR_TEXT_MSG = 0x777777;
    private static final int COLOR_TIME = 0x888888;
    private static final int COLOR_BADGE = 0x25D366;
    private static final int COLOR_DIVIDER = 0xEEEEEE;

    public ChatListCanvas(WhatsAppMIDlet midlet) {
        this.midlet = midlet;
        setFullScreenMode(false);

        cmdOpen = new Command("Open", Command.OK, 1);
        cmdRefresh = new Command("Refresh", Command.ITEM, 2);
        cmdLink = new Command("Link Account", Command.ITEM, 3);
        cmdSettings = new Command("Settings", Command.ITEM, 4);
        cmdExit = new Command("Exit", Command.EXIT, 5);

        addCommand(cmdOpen);
        addCommand(cmdRefresh);
        addCommand(cmdLink);
        addCommand(cmdSettings);
        addCommand(cmdExit);
        setCommandListener(this);
    }

    public void setChats(Vector list) {
        this.chatList = list != null ? list : new Vector();
        if (selectedIndex >= chatList.size()) {
            selectedIndex = Math.max(0, chatList.size() - 1);
        }
        isLoading = false;
        statusMsg = chatList.size() + " chats";
        repaint();
    }

    public void fetchChats() {
        isLoading = true;
        statusMsg = "Connecting...";
        repaint();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = midlet.getServerUrl() + "/api/chats";
                    String res = HttpHelper.get(url);
                    Hashtable obj = (Hashtable) MiniJson.parse(res);
                    if (obj != null) {
                        Vector list = (Vector) obj.get("chats");
                        setChats(list);
                    }
                } catch (Exception e) {
                    isLoading = false;
                    statusMsg = "Offline";
                    repaint();
                }
            }
        }).start();
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Header bar
        int headerHeight = 28;
        g.setColor(COLOR_HEADER_BG);
        g.fillRect(0, 0, w, headerHeight);

        Font fontHeader = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        g.setFont(fontHeader);
        g.setColor(COLOR_HEADER_TEXT);
        g.drawString("WhatsApp", 6, 5, Graphics.TOP | Graphics.LEFT);

        // Status pill on right of header
        Font fontSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(fontSmall);
        g.drawString(statusMsg, w - 6, 8, Graphics.TOP | Graphics.RIGHT);

        // Subheader tabs: [ CHATS ]
        int subHeaderHeight = 20;
        int subHeaderY = headerHeight;
        g.setColor(COLOR_SUBHEADER_BG);
        g.fillRect(0, subHeaderY, w, subHeaderHeight);

        g.setColor(0xFFFFFF);
        g.drawString("CHATS", 12, subHeaderY + 3, Graphics.TOP | Graphics.LEFT);
        g.setColor(COLOR_BADGE);
        g.fillRect(10, subHeaderY + subHeaderHeight - 2, 45, 2); // green underline tab

        int listTop = headerHeight + subHeaderHeight;
        int rowHeight = 44;
        int visibleRows = (h - listTop) / rowHeight;

        if (chatList == null || chatList.size() == 0) {
            g.setColor(COLOR_TEXT_MSG);
            g.drawString(isLoading ? "Loading chats..." : "No chats yet.", w / 2, listTop + 40, Graphics.TOP | Graphics.HCENTER);
            g.drawString("Options -> Link Account to link", w / 2, listTop + 60, Graphics.TOP | Graphics.HCENTER);
            return;
        }

        // Adjust scroll offset
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        // Render chat rows
        int currentY = listTop;
        for (int i = scrollOffset; i < chatList.size() && currentY < h; i++) {
            Hashtable chat = (Hashtable) chatList.elementAt(i);
            boolean isSelected = (i == selectedIndex);

            // Row background
            if (isSelected) {
                g.setColor(COLOR_ROW_HIGHLIGHT);
                g.fillRect(0, currentY, w, rowHeight);
                g.setColor(COLOR_HEADER_BG);
                g.fillRect(0, currentY, 3, rowHeight); // left indicator bar
            }

            // Avatar circle
            int avatarX = 8;
            int avatarY = currentY + 7;
            int avatarSize = 30;
            g.setColor(0x128C7E);
            g.fillRoundRect(avatarX, avatarY, avatarSize, avatarSize, avatarSize, avatarSize);

            String name = MiniJson.getString(chat, "name", "Chat");
            char initial = name.length() > 0 ? Character.toUpperCase(name.charAt(0)) : 'W';
            g.setFont(fontHeader);
            g.setColor(0xFFFFFF);
            g.drawChar(initial, avatarX + 10, avatarY + 6, Graphics.TOP | Graphics.LEFT);

            // Contact Name
            int textX = avatarX + avatarSize + 10;
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.setColor(COLOR_TEXT_NAME);
            String displayName = name;
            if (displayName.length() > 18) displayName = displayName.substring(0, 16) + "...";
            g.drawString(displayName, textX, currentY + 6, Graphics.TOP | Graphics.LEFT);

            // Last message preview
            String lastMsg = MiniJson.getString(chat, "lastMessage", "");
            if (lastMsg.length() > 24) lastMsg = lastMsg.substring(0, 22) + "...";
            g.setFont(fontSmall);
            g.setColor(COLOR_TEXT_MSG);
            g.drawString(lastMsg, textX, currentY + 24, Graphics.TOP | Graphics.LEFT);

            // Unread badge
            int unread = MiniJson.getInt(chat, "unread", 0);
            if (unread > 0) {
                int badgeSize = 16;
                int badgeX = w - badgeSize - 10;
                int badgeY = currentY + 14;
                g.setColor(COLOR_BADGE);
                g.fillRoundRect(badgeX, badgeY, badgeSize, badgeSize, badgeSize, badgeSize);
                g.setColor(0xFFFFFF);
                g.drawString(String.valueOf(unread), badgeX + 4, badgeY + 1, Graphics.TOP | Graphics.LEFT);
            }

            // Divider line
            g.setColor(COLOR_DIVIDER);
            g.drawLine(textX, currentY + rowHeight - 1, w, currentY + rowHeight - 1);

            currentY += rowHeight;
        }
    }

    protected void keyPressed(int keyCode) {
        int ga = getGameAction(keyCode);
        if (ga == UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                repaint();
            }
        } else if (ga == DOWN) {
            if (chatList != null && selectedIndex < chatList.size() - 1) {
                selectedIndex++;
                repaint();
            }
        } else if (ga == FIRE || keyCode == -5 || keyCode == 10) {
            openSelectedChat();
        }
    }

    protected void pointerPressed(int x, int y) {
        int listTop = 48;
        int rowHeight = 44;
        if (y >= listTop) {
            int clickedIndex = scrollOffset + (y - listTop) / rowHeight;
            if (chatList != null && clickedIndex >= 0 && clickedIndex < chatList.size()) {
                if (selectedIndex == clickedIndex) {
                    openSelectedChat();
                } else {
                    selectedIndex = clickedIndex;
                    repaint();
                }
            }
        }
    }

    private void openSelectedChat() {
        if (chatList != null && selectedIndex >= 0 && selectedIndex < chatList.size()) {
            Hashtable chat = (Hashtable) chatList.elementAt(selectedIndex);
            String chatId = MiniJson.getString(chat, "id", "");
            String chatName = MiniJson.getString(chat, "name", "Chat");
            if (chatId.length() > 0) {
                midlet.openConversation(chatId, chatName);
            }
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdOpen) {
            openSelectedChat();
        } else if (c == cmdRefresh) {
            fetchChats();
        } else if (c == cmdLink) {
            midlet.showPairingScreen();
        } else if (c == cmdSettings) {
            midlet.showSettings();
        } else if (c == cmdExit) {
            midlet.exitApp();
        }
    }
}
