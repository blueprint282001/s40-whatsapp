package com.whatsapp.s40.ui;

import com.whatsapp.s40.WhatsAppMIDlet;
import com.whatsapp.s40.net.HttpHelper;
import com.whatsapp.s40.util.MiniJson;
import java.util.Calendar;
import java.util.Date;
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
    private String statusMsg = "Connected";

    // Standard Commands
    private Command cmdOpen;
    private Command cmdRefresh;
    private Command cmdLink;
    private Command cmdSettings;
    private Command cmdExit;

    // S40 WhatsApp Theme Colors
    private static final int COLOR_STATUS_BG = 0x054C44;
    private static final int COLOR_HEADER_BG = 0x075E54;
    private static final int COLOR_HEADER_TEXT = 0xFFFFFF;
    private static final int COLOR_SUBHEADER_BG = 0x128C7E;
    private static final int COLOR_BG = 0xFFFFFF;
    private static final int COLOR_ROW_HIGHLIGHT = 0xE1F3E9;
    private static final int COLOR_TEXT_NAME = 0x111111;
    private static final int COLOR_TEXT_MSG = 0x666666;
    private static final int COLOR_TIME = 0x25D366;
    private static final int COLOR_BADGE = 0x25D366;
    private static final int COLOR_DIVIDER = 0xEEEEEE;
    private static final int COLOR_SOFTKEY_BG = 0x111111;

    // Contact avatar colors
    private static final int[] AVATAR_COLORS = {
        0x128C7E, 0x00A884, 0x34B7F1, 0x9B59B6, 0xE67E22, 0xE74C3C, 0x27AE60, 0x34495E
    };

    public ChatListCanvas(WhatsAppMIDlet midlet) {
        this.midlet = midlet;
        // Full screen mode for Nokia C2-01 / X2-00
        setFullScreenMode(true);

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
        statusMsg = chatList.size() + " Chats";
        repaint();
    }

    public void fetchChats() {
        isLoading = true;
        statusMsg = "Syncing...";
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

    private int getAvatarColor(String name) {
        if (name == null || name.length() == 0) return AVATAR_COLORS[0];
        int hash = 0;
        for (int i = 0; i < name.length(); i++) {
            hash += name.charAt(i);
        }
        return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // 1. Nokia System Status Bar (Top 14px)
        int statusBarH = 14;
        g.setColor(COLOR_STATUS_BG);
        g.fillRect(0, 0, w, statusBarH);

        Font fontMicro = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(fontMicro);
        g.setColor(0xFFFFFF);
        g.drawString("3G 📶", 4, 1, Graphics.TOP | Graphics.LEFT);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int hour = cal.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        int min = cal.get(Calendar.MINUTE);
        String timeStr = hour + ":" + (min < 10 ? "0" + min : "" + min);
        g.drawString(timeStr, w / 2, 1, Graphics.TOP | Graphics.HCENTER);
        g.drawString("🔋 90%", w - 4, 1, Graphics.TOP | Graphics.RIGHT);

        // 2. WhatsApp Main Header (24px)
        int headerH = 24;
        int headerY = statusBarH;
        g.setColor(COLOR_HEADER_BG);
        g.fillRect(0, headerY, w, headerH);

        Font fontHeader = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        g.setFont(fontHeader);
        g.setColor(COLOR_HEADER_TEXT);
        g.drawString("WhatsApp", 6, headerY + 4, Graphics.TOP | Graphics.LEFT);

        Font fontSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(fontSmall);
        g.setColor(0xA2D2CE);
        g.drawString(statusMsg, w - 6, headerY + 6, Graphics.TOP | Graphics.RIGHT);

        // 3. Tab Bar (20px): CHATS | CONTACTS | STATUS
        int tabH = 18;
        int tabY = headerY + headerH;
        g.setColor(COLOR_SUBHEADER_BG);
        g.fillRect(0, tabY, w, tabH);

        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.setColor(0xFFFFFF);
        g.drawString("CHATS", 12, tabY + 2, Graphics.TOP | Graphics.LEFT);
        // Active tab underline
        g.setColor(0xFFFFFF);
        g.fillRect(10, tabY + tabH - 2, 45, 2);

        g.setColor(0x80D8CE);
        g.drawString("CONTACTS", w / 2, tabY + 2, Graphics.TOP | Graphics.HCENTER);
        g.drawString("STATUS", w - 12, tabY + 2, Graphics.TOP | Graphics.RIGHT);

        // 4. Bottom Softkey Bar (20px)
        int softkeyH = 20;
        int softkeyY = h - softkeyH;
        g.setColor(COLOR_SOFTKEY_BG);
        g.fillRect(0, softkeyY, w, softkeyH);

        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.setColor(0xEEEEEE);
        g.drawString("Options", 6, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
        g.setColor(COLOR_BADGE);
        g.drawString("Open", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xEEEEEE);
        g.drawString("Exit", w - 6, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);

        // 5. Chat List Area
        int listTop = tabY + tabH;
        int listBottom = softkeyY;
        int listHeight = listBottom - listTop;
        int rowH = 46;
        int visibleRows = listHeight / rowH;

        g.setClip(0, listTop, w, listHeight);

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, listTop, w, listHeight);

        if (chatList == null || chatList.size() == 0) {
            g.setColor(COLOR_TEXT_MSG);
            g.setFont(fontSmall);
            g.drawString(isLoading ? "Connecting to WhatsApp..." : "No recent chats.", w / 2, listTop + 40, Graphics.TOP | Graphics.HCENTER);
            g.drawString("Press Options -> Refresh", w / 2, listTop + 65, Graphics.TOP | Graphics.HCENTER);
            g.setClip(0, 0, w, h);
            return;
        }

        // Adjust scroll offset
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        int currentY = listTop;
        for (int i = scrollOffset; i < chatList.size() && currentY < listBottom; i++) {
            Hashtable chat = (Hashtable) chatList.elementAt(i);
            boolean isSelected = (i == selectedIndex);

            // Row background highlight
            if (isSelected) {
                g.setColor(COLOR_ROW_HIGHLIGHT);
                g.fillRect(0, currentY, w, rowH);
                g.setColor(COLOR_HEADER_BG);
                g.fillRect(0, currentY, 3, rowH); // left green accent
            }

            String name = MiniJson.getString(chat, "name", "Chat");

            // Circular Avatar with distinct colors
            int avatarX = 8;
            int avatarY = currentY + 7;
            int avatarSize = 32;
            g.setColor(getAvatarColor(name));
            g.fillRoundRect(avatarX, avatarY, avatarSize, avatarSize, avatarSize, avatarSize);

            // Avatar initial
            char initial = name.length() > 0 ? Character.toUpperCase(name.charAt(0)) : 'W';
            g.setFont(fontHeader);
            g.setColor(0xFFFFFF);
            g.drawChar(initial, avatarX + 11, avatarY + 7, Graphics.TOP | Graphics.LEFT);

            // Contact Name
            int textX = avatarX + avatarSize + 8;
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.setColor(COLOR_TEXT_NAME);
            String displayName = name;
            if (displayName.length() > 16) displayName = displayName.substring(0, 14) + "...";
            g.drawString(displayName, textX, currentY + 6, Graphics.TOP | Graphics.LEFT);

            // Last message snippet
            String lastMsg = MiniJson.getString(chat, "lastMessage", "");
            if (lastMsg.length() > 22) lastMsg = lastMsg.substring(0, 20) + "...";
            g.setFont(fontSmall);
            g.setColor(COLOR_TEXT_MSG);
            g.drawString(lastMsg, textX, currentY + 25, Graphics.TOP | Graphics.LEFT);

            // Timestamp / Unread badge
            int unread = MiniJson.getInt(chat, "unread", 0);
            if (unread > 0) {
                int badgeSize = 16;
                int badgeX = w - badgeSize - 8;
                int badgeY = currentY + 15;
                g.setColor(COLOR_BADGE);
                g.fillRoundRect(badgeX, badgeY, badgeSize, badgeSize, badgeSize, badgeSize);
                g.setColor(0xFFFFFF);
                g.setFont(fontMicro);
                g.drawString(String.valueOf(unread), badgeX + 4, badgeY + 1, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(COLOR_TIME);
                g.setFont(fontMicro);
                g.drawString("10:45", w - 8, currentY + 7, Graphics.TOP | Graphics.RIGHT);
            }

            // Divider
            g.setColor(COLOR_DIVIDER);
            g.drawLine(textX, currentY + rowH - 1, w, currentY + rowH - 1);

            currentY += rowH;
        }

        g.setClip(0, 0, w, h);
    }

    protected void keyPressed(int keyCode) {
        int ga = getGameAction(keyCode);

        // Nokia C2-01 / X2-00 Softkeys & D-Pad
        if (keyCode == -6) {
            // Left Softkey -> Options
            showOptionsMenu();
            return;
        } else if (keyCode == -7) {
            // Right Softkey -> Exit
            midlet.exitApp();
            return;
        }

        if (ga == UP || keyCode == -1) {
            if (selectedIndex > 0) {
                selectedIndex--;
                repaint();
            }
        } else if (ga == DOWN || keyCode == -2) {
            if (chatList != null && selectedIndex < chatList.size() - 1) {
                selectedIndex++;
                repaint();
            }
        } else if (ga == FIRE || keyCode == -5 || keyCode == 10) {
            openSelectedChat();
        }
    }

    protected void pointerPressed(int x, int y) {
        int w = getWidth();
        int h = getHeight();

        // Check softkeys tap
        if (y >= h - 24) {
            if (x < w / 3) {
                showOptionsMenu();
            } else if (x > (2 * w) / 3) {
                midlet.exitApp();
            } else {
                openSelectedChat();
            }
            return;
        }

        // List row tap
        int listTop = 14 + 24 + 18;
        int rowH = 46;
        if (y >= listTop && y < h - 20) {
            int clicked = scrollOffset + (y - listTop) / rowH;
            if (chatList != null && clicked >= 0 && clicked < chatList.size()) {
                if (selectedIndex == clicked) {
                    openSelectedChat();
                } else {
                    selectedIndex = clicked;
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

    private void showOptionsMenu() {
        // Can display options form or trigger refresh
        fetchChats();
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
