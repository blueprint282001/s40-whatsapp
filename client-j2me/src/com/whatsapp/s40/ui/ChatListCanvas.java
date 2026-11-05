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
import javax.microedition.lcdui.Image;

public class ChatListCanvas extends Canvas implements CommandListener {

    private WhatsAppMIDlet midlet;
    private Vector chatList = new Vector();
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private boolean isLoading = false;

    // Authentic WhatsApp Graphics
    private Image imgHeaderLogo;
    private Image imgAvatar;
    private Image imgTickDouble;
    private Image imgCamera;
    private Image imgMic;

    // Commands
    private Command cmdOpen;
    private Command cmdRefresh;
    private Command cmdLink;
    private Command cmdSettings;
    private Command cmdExit;

    // Official WhatsApp Brand Colors
    private static final int COLOR_HEADER_BG = 0x075E54;
    private static final int COLOR_TAB_BG = 0x128C7E;
    private static final int COLOR_TAB_INACTIVE = 0x8CD2CE;
    private static final int COLOR_BG = 0xFFFFFF;
    private static final int COLOR_ROW_HIGHLIGHT = 0xEEF7F2;
    private static final int COLOR_ROW_ACCENT = 0x075E54;
    private static final int COLOR_TEXT_NAME = 0x111111;
    private static final int COLOR_TEXT_SNIPPET = 0x667781;
    private static final int COLOR_TIME_GRAY = 0x8696A0;
    private static final int COLOR_TIME_UNREAD = 0x25D366;
    private static final int COLOR_BADGE = 0x25D366;
    private static final int COLOR_DIVIDER = 0xE9EDEF;
    private static final int COLOR_BOTTOM_BAR = 0x1F2C34;

    public ChatListCanvas(WhatsAppMIDlet midlet) {
        this.midlet = midlet;
        setFullScreenMode(true);

        loadAssets();

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

    private void loadAssets() {
        try { imgHeaderLogo = Image.createImage("/icon_header.png"); } catch (Exception e) {}
        try { imgAvatar = Image.createImage("/avatar_default.png"); } catch (Exception e) {}
        try { imgTickDouble = Image.createImage("/tick_double.png"); } catch (Exception e) {}
        try { imgCamera = Image.createImage("/icon_camera.png"); } catch (Exception e) {}
        try { imgMic = Image.createImage("/icon_mic.png"); } catch (Exception e) {}
    }

    public void setChats(Vector list) {
        this.chatList = list != null ? list : new Vector();
        if (selectedIndex >= chatList.size()) {
            selectedIndex = Math.max(0, chatList.size() - 1);
        }
        isLoading = false;
        repaint();
    }

    public void fetchChats() {
        isLoading = true;
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
                    repaint();
                }
            }
        }).start();
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // 1. WhatsApp Signature Header (32px)
        int headerH = 32;
        g.setColor(COLOR_HEADER_BG);
        g.fillRect(0, 0, w, headerH);

        // Official WhatsApp Logo Icon
        if (imgHeaderLogo != null) {
            g.drawImage(imgHeaderLogo, 8, 6, Graphics.TOP | Graphics.LEFT);
        }

        // WhatsApp Brand Title
        Font fontTitle = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        g.setFont(fontTitle);
        g.setColor(0xFFFFFF);
        g.drawString("WhatsApp", imgHeaderLogo != null ? 32 : 10, 7, Graphics.TOP | Graphics.LEFT);

        // Search 🔍 Icon on Right
        int searchX = w - 30;
        int searchY = 10;
        g.setColor(0xFFFFFF);
        g.drawArc(searchX, searchY, 7, 7, 0, 360);
        g.drawLine(searchX + 6, searchY + 6, searchX + 10, searchY + 10);

        // Menu 3-dots icon
        g.fillRect(w - 12, 10, 2, 2);
        g.fillRect(w - 12, 15, 2, 2);
        g.fillRect(w - 12, 20, 2, 2);

        // 2. Tab Bar: CHATS | STATUS | CALLS (24px)
        int tabH = 24;
        int tabY = headerH;
        g.setColor(COLOR_TAB_BG);
        g.fillRect(0, tabY, w, tabH);

        Font fontTab = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(fontTab);

        // Active Tab: CHATS
        g.setColor(0xFFFFFF);
        g.drawString("CHATS", 14, tabY + 4, Graphics.TOP | Graphics.LEFT);
        // Active Tab Underline (3px pure white)
        g.fillRect(10, tabY + tabH - 3, 50, 3);

        // Inactive Tabs
        g.setColor(COLOR_TAB_INACTIVE);
        g.drawString("STATUS", w / 2, tabY + 4, Graphics.TOP | Graphics.HCENTER);
        g.drawString("CALLS", w - 16, tabY + 4, Graphics.TOP | Graphics.RIGHT);

        // 3. Bottom Softkey Bar (20px)
        int softkeyH = 20;
        int softkeyY = h - softkeyH;
        g.setColor(COLOR_BOTTOM_BAR);
        g.fillRect(0, softkeyY, w, softkeyH);

        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.setColor(0xDFE5E7);
        g.drawString("Options", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
        g.setColor(COLOR_BADGE);
        g.drawString("Open", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xDFE5E7);
        g.drawString("Exit", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);

        // 4. Chat List Scrollable Area
        int listTop = tabY + tabH;
        int listHeight = softkeyY - listTop;
        int rowH = 48;
        int visibleRows = listHeight / rowH;

        g.setClip(0, listTop, w, listHeight);

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, listTop, w, listHeight);

        if (chatList == null || chatList.size() == 0) {
            g.setColor(COLOR_TEXT_SNIPPET);
            Font fontSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(fontSmall);
            g.drawString(isLoading ? "Connecting to WhatsApp..." : "No recent chats.", w / 2, listTop + 45, Graphics.TOP | Graphics.HCENTER);
            g.drawString("Press Options -> Refresh", w / 2, listTop + 68, Graphics.TOP | Graphics.HCENTER);
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
        Font fontName = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font fontSnippet = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font fontMicro = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        for (int i = scrollOffset; i < chatList.size() && currentY < softkeyY; i++) {
            Hashtable chat = (Hashtable) chatList.elementAt(i);
            boolean isSelected = (i == selectedIndex);

            // Row highlight
            if (isSelected) {
                g.setColor(COLOR_ROW_HIGHLIGHT);
                g.fillRect(0, currentY, w, rowH);
                g.setColor(COLOR_ROW_ACCENT);
                g.fillRect(0, currentY, 3, rowH); // deep green left indicator
            }

            // Authentic WhatsApp Contact Avatar (32x32)
            int avatarX = 8;
            int avatarY = currentY + 8;
            if (imgAvatar != null) {
                g.drawImage(imgAvatar, avatarX, avatarY, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(0xDFE5E7);
                g.fillRoundRect(avatarX, avatarY, 32, 32, 32, 32);
            }

            String name = MiniJson.getString(chat, "name", "Chat");
            String lastMsg = MiniJson.getString(chat, "lastMessage", "");
            int unread = MiniJson.getInt(chat, "unread", 0);

            int textX = avatarX + 32 + 10;

            // Contact Name
            g.setFont(fontName);
            g.setColor(COLOR_TEXT_NAME);
            String displayName = name;
            if (displayName.length() > 16) displayName = displayName.substring(0, 14) + "...";
            g.drawString(displayName, textX, currentY + 7, Graphics.TOP | Graphics.LEFT);

            // Timestamp (Top Right)
            g.setFont(fontMicro);
            g.setColor(unread > 0 ? COLOR_TIME_UNREAD : COLOR_TIME_GRAY);
            g.drawString("10:45", w - 8, currentY + 8, Graphics.TOP | Graphics.RIGHT);

            // Last message snippet & status tick
            int snippetY = currentY + 27;
            int snippetStartX = textX;

            // Check if outgoing message (starts with You: or has tick)
            if (lastMsg.startsWith("[Photo]") && imgCamera != null) {
                g.drawImage(imgCamera, snippetStartX, snippetY + 1, Graphics.TOP | Graphics.LEFT);
                snippetStartX += 16;
                lastMsg = "Photo";
            } else if (lastMsg.startsWith("[Voice Note]") && imgMic != null) {
                g.drawImage(imgMic, snippetStartX, snippetY + 1, Graphics.TOP | Graphics.LEFT);
                snippetStartX += 14;
                lastMsg = "Voice message (0:14)";
            } else if (imgTickDouble != null) {
                // Outgoing message double-tick preview
                g.drawImage(imgTickDouble, snippetStartX, snippetY + 3, Graphics.TOP | Graphics.LEFT);
                snippetStartX += 18;
            }

            if (lastMsg.length() > 22) lastMsg = lastMsg.substring(0, 20) + "...";
            g.setFont(fontSnippet);
            g.setColor(COLOR_TEXT_SNIPPET);
            g.drawString(lastMsg, snippetStartX, snippetY, Graphics.TOP | Graphics.LEFT);

            // Unread Count Badge (Right side circle)
            if (unread > 0) {
                int badgeSize = 18;
                int badgeX = w - badgeSize - 8;
                int badgeY = currentY + 24;
                g.setColor(COLOR_BADGE);
                g.fillRoundRect(badgeX, badgeY, badgeSize, badgeSize, badgeSize, badgeSize);
                g.setColor(0xFFFFFF);
                g.setFont(fontName);
                String unreadStr = String.valueOf(unread);
                int numW = fontName.stringWidth(unreadStr);
                g.drawString(unreadStr, badgeX + (badgeSize - numW) / 2, badgeY + 2, Graphics.TOP | Graphics.LEFT);
            }

            // Divider line
            g.setColor(COLOR_DIVIDER);
            g.drawLine(textX, currentY + rowH - 1, w, currentY + rowH - 1);

            currentY += rowH;
        }

        g.setClip(0, 0, w, h);
    }

    protected void keyPressed(int keyCode) {
        int ga = getGameAction(keyCode);

        // Nokia Softkey Bindings
        if (keyCode == -6) {
            // LSK: Options
            fetchChats();
            return;
        } else if (keyCode == -7) {
            // RSK: Exit
            midlet.exitApp();
            return;
        }

        // Navi-Key Up/Down
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

        // Bottom Softkeys
        if (y >= h - 22) {
            if (x < w / 3) {
                fetchChats();
            } else if (x > (2 * w) / 3) {
                midlet.exitApp();
            } else {
                openSelectedChat();
            }
            return;
        }

        // List row click
        int listTop = 32 + 24;
        int rowH = 48;
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
