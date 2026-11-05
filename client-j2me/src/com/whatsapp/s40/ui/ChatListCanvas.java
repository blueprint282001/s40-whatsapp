package com.whatsapp.s40.ui;

import com.whatsapp.s40.WhatsAppMIDlet;
import com.whatsapp.s40.net.HttpHelper;
import com.whatsapp.s40.util.MiniJson;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class ChatListCanvas extends Canvas implements CommandListener {

    private WhatsAppMIDlet midlet;

    // Active Tab: 0 = CHATS, 1 = CONTACTS, 2 = STATUS
    private int activeTab = 0;

    // Data Vectors
    private Vector chatList = new Vector();
    private Vector contactsList = new Vector();
    private Vector statusList = new Vector();
    private String myStatus = "Available";

    // Selection & Scrolling
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private boolean isLoading = false;

    // Authentic WhatsApp Graphics
    private Image imgHeaderLogo;
    private Image imgAvatar;
    private Image imgGroupAvatar;
    private Image imgTickDouble;
    private Image imgCamera;
    private Image imgMic;

    // Standard Commands
    private Command cmdOpen;
    private Command cmdOptions;
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
    private static final int COLOR_SECTION_HEADER = 0xF0F2F5;

    public ChatListCanvas(WhatsAppMIDlet midlet) {
        this.midlet = midlet;
        setFullScreenMode(true);

        loadAssets();

        cmdOpen = new Command("Open", Command.OK, 1);
        cmdOptions = new Command("Options", Command.ITEM, 2);
        cmdExit = new Command("Exit", Command.EXIT, 3);

        addCommand(cmdOpen);
        addCommand(cmdOptions);
        addCommand(cmdExit);
        setCommandListener(this);
    }

    private void loadAssets() {
        try { imgHeaderLogo = Image.createImage("/icon_header.png"); } catch (Exception e) {}
        try { imgAvatar = Image.createImage("/avatar_default.png"); } catch (Exception e) {}
        try { imgGroupAvatar = Image.createImage("/avatar_group.png"); } catch (Exception e) {}
        try { imgTickDouble = Image.createImage("/tick_double.png"); } catch (Exception e) {}
        try { imgCamera = Image.createImage("/icon_camera.png"); } catch (Exception e) {}
        try { imgMic = Image.createImage("/icon_mic.png"); } catch (Exception e) {}
    }

    public void setChats(Vector list) {
        this.chatList = list != null ? list : new Vector();
        if (activeTab == 0 && selectedIndex >= chatList.size()) {
            selectedIndex = Math.max(0, chatList.size() - 1);
        }
        isLoading = false;
        repaint();
    }

    public void setContacts(Vector list) {
        this.contactsList = list != null ? list : new Vector();
        if (activeTab == 1 && selectedIndex >= contactsList.size()) {
            selectedIndex = Math.max(0, contactsList.size() - 1);
        }
        isLoading = false;
        repaint();
    }

    public void setStatuses(Vector list) {
        this.statusList = list != null ? list : new Vector();
        if (activeTab == 2 && selectedIndex >= statusList.size()) {
            selectedIndex = Math.max(0, statusList.size() - 1);
        }
        isLoading = false;
        repaint();
    }

    public void fetchData() {
        if (activeTab == 0) {
            fetchChats();
        } else if (activeTab == 1) {
            fetchContacts();
        } else if (activeTab == 2) {
            fetchStatuses();
        }
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

    public void fetchContacts() {
        isLoading = true;
        repaint();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = midlet.getServerUrl() + "/api/contacts";
                    String res = HttpHelper.get(url);
                    Hashtable obj = (Hashtable) MiniJson.parse(res);
                    if (obj != null) {
                        Vector list = (Vector) obj.get("contacts");
                        setContacts(list);
                    }
                } catch (Exception e) {
                    isLoading = false;
                    repaint();
                }
            }
        }).start();
    }

    public void fetchStatuses() {
        isLoading = true;
        repaint();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = midlet.getServerUrl() + "/api/status-list";
                    String res = HttpHelper.get(url);
                    Hashtable obj = (Hashtable) MiniJson.parse(res);
                    if (obj != null) {
                        Vector list = (Vector) obj.get("statuses");
                        setStatuses(list);
                    }

                    // Also fetch profile for status text
                    String pUrl = midlet.getServerUrl() + "/api/profile";
                    String pRes = HttpHelper.get(pUrl);
                    Hashtable pObj = (Hashtable) MiniJson.parse(pRes);
                    if (pObj != null) {
                        myStatus = MiniJson.getString(pObj, "statusText", "Available");
                        repaint();
                    }
                } catch (Exception e) {
                    isLoading = false;
                    repaint();
                }
            }
        }).start();
    }

    protected void paint(Graphics g) {
        try {
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

            // 2. Tab Bar: CHATS | CONTACTS | STATUS (24px)
            int tabH = 24;
            int tabY = headerH;
            g.setColor(COLOR_TAB_BG);
            g.fillRect(0, tabY, w, tabH);

            Font fontTab = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            g.setFont(fontTab);

            int tabColW = w / 3;

            // Tab 0: CHATS
            int t0X = tabColW / 2;
            g.setColor(activeTab == 0 ? 0xFFFFFF : COLOR_TAB_INACTIVE);
            g.drawString("CHATS", t0X, tabY + 4, Graphics.TOP | Graphics.HCENTER);
            if (activeTab == 0) {
                g.setColor(0xFFFFFF);
                g.fillRect(4, tabY + tabH - 3, tabColW - 8, 3);
            }

            // Tab 1: CONTACTS
            int t1X = tabColW + (tabColW / 2);
            g.setColor(activeTab == 1 ? 0xFFFFFF : COLOR_TAB_INACTIVE);
            g.drawString("CONTACTS", t1X, tabY + 4, Graphics.TOP | Graphics.HCENTER);
            if (activeTab == 1) {
                g.setColor(0xFFFFFF);
                g.fillRect(tabColW + 4, tabY + tabH - 3, tabColW - 8, 3);
            }

            // Tab 2: STATUS
            int t2X = (2 * tabColW) + (tabColW / 2);
            g.setColor(activeTab == 2 ? 0xFFFFFF : COLOR_TAB_INACTIVE);
            g.drawString("STATUS", t2X, tabY + 4, Graphics.TOP | Graphics.HCENTER);
            if (activeTab == 2) {
                g.setColor(0xFFFFFF);
                g.fillRect((2 * tabColW) + 4, tabY + tabH - 3, w - (2 * tabColW) - 8, 3);
            }

            // 3. Bottom Softkey Bar (20px)
            int softkeyH = 20;
            int softkeyY = h - softkeyH;
            g.setColor(COLOR_BOTTOM_BAR);
            g.fillRect(0, softkeyY, w, softkeyH);

            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
            g.setColor(0xDFE5E7);
            g.drawString("Options", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(COLOR_BADGE);
            String centerAction = activeTab == 0 ? "Open" : (activeTab == 1 ? "Chat" : "Edit");
            g.drawString(centerAction, w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0xDFE5E7);
            g.drawString("Exit", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);

            // 4. Content Area
            int listTop = tabY + tabH;
            int listHeight = softkeyY - listTop;

            int origClipX = g.getClipX();
            int origClipY = g.getClipY();
            int origClipW = g.getClipWidth();
            int origClipH = g.getClipHeight();

            if (w > 0 && listHeight > 0) {
                g.setClip(0, listTop, w, listHeight);
            }

            // Background
            g.setColor(COLOR_BG);
            g.fillRect(0, listTop, w, listHeight);

            if (activeTab == 0) {
                paintChatsTab(g, w, listTop, listHeight, softkeyY);
            } else if (activeTab == 1) {
                paintContactsTab(g, w, listTop, listHeight, softkeyY);
            } else if (activeTab == 2) {
                paintStatusTab(g, w, listTop, listHeight, softkeyY);
            }

            g.setClip(origClipX, origClipY, origClipW, origClipH);
        } catch (Throwable t) {
            // Safe fallback: never crash with IllegalArgumentException in paint
        }
    }

    private void paintChatsTab(Graphics g, int w, int listTop, int listHeight, int softkeyY) {
        int rowH = 48;
        int visibleRows = listHeight / rowH;

        if (chatList == null || chatList.size() == 0) {
            g.setColor(COLOR_TEXT_SNIPPET);
            Font fontSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(fontSmall);
            g.drawString(isLoading ? "Syncing with WhatsApp..." : "No recent chats.", w / 2, listTop + 45, Graphics.TOP | Graphics.HCENTER);
            g.drawString("Press Options -> New Chat", w / 2, listTop + 68, Graphics.TOP | Graphics.HCENTER);
            return;
        }

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

            if (isSelected) {
                g.setColor(COLOR_ROW_HIGHLIGHT);
                g.fillRect(0, currentY, w, rowH);
                g.setColor(COLOR_ROW_ACCENT);
                g.fillRect(0, currentY, 3, rowH);
            }

            boolean isGroup = MiniJson.getBoolean(chat, "isGroup", false);
            String name = MiniJson.getString(chat, "name", "Chat");
            String lastMsg = MiniJson.getString(chat, "lastMessage", "");
            int unread = MiniJson.getInt(chat, "unread", 0);

            // Group vs individual Avatar
            int avatarX = 8;
            int avatarY = currentY + 8;
            if (isGroup && imgGroupAvatar != null) {
                g.drawImage(imgGroupAvatar, avatarX, avatarY, Graphics.TOP | Graphics.LEFT);
            } else if (imgAvatar != null) {
                g.drawImage(imgAvatar, avatarX, avatarY, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(isGroup ? COLOR_TAB_BG : 0xDFE5E7);
                g.fillRoundRect(avatarX, avatarY, 32, 32, 8, 8);
            }

            int textX = avatarX + 32 + 10;

            // Chat Name
            g.setFont(fontName);
            g.setColor(COLOR_TEXT_NAME);
            String displayName = name;
            if (displayName.length() > 16) displayName = displayName.substring(0, 14) + "...";
            g.drawString(displayName, textX, currentY + 7, Graphics.TOP | Graphics.LEFT);

            // Timestamp (Top Right)
            g.setFont(fontMicro);
            g.setColor(unread > 0 ? COLOR_TIME_UNREAD : COLOR_TIME_GRAY);
            g.drawString("11:30", w - 8, currentY + 8, Graphics.TOP | Graphics.RIGHT);

            // Last message snippet & status tick
            int snippetY = currentY + 27;
            int snippetStartX = textX;

            if (lastMsg.startsWith("[Photo]") && imgCamera != null) {
                g.drawImage(imgCamera, snippetStartX, snippetY + 1, Graphics.TOP | Graphics.LEFT);
                snippetStartX += 16;
                lastMsg = "Photo";
            } else if (lastMsg.startsWith("[Voice Note]") && imgMic != null) {
                g.drawImage(imgMic, snippetStartX, snippetY + 1, Graphics.TOP | Graphics.LEFT);
                snippetStartX += 14;
                lastMsg = "Voice message";
            } else if (!isGroup && imgTickDouble != null) {
                g.drawImage(imgTickDouble, snippetStartX, snippetY + 3, Graphics.TOP | Graphics.LEFT);
                snippetStartX += 18;
            }

            if (lastMsg.length() > 22) lastMsg = lastMsg.substring(0, 20) + "...";
            g.setFont(fontSnippet);
            g.setColor(COLOR_TEXT_SNIPPET);
            g.drawString(lastMsg, snippetStartX, snippetY, Graphics.TOP | Graphics.LEFT);

            // Unread Count Badge
            if (unread > 0) {
                int badgeSize = 18;
                int badgeX = w - badgeSize - 8;
                int badgeY = currentY + 24;
                g.setColor(COLOR_BADGE);
                g.fillRoundRect(badgeX, badgeY, badgeSize, badgeSize, 6, 6);
                g.setColor(0xFFFFFF);
                g.setFont(fontName);
                String unreadStr = String.valueOf(unread);
                int numW = fontName.stringWidth(unreadStr);
                g.drawString(unreadStr, badgeX + (badgeSize - numW) / 2, badgeY + 2, Graphics.TOP | Graphics.LEFT);
            }

            // Divider
            g.setColor(COLOR_DIVIDER);
            g.drawLine(textX, currentY + rowH - 1, w, currentY + rowH - 1);

            currentY += rowH;
        }
    }

    private void paintContactsTab(Graphics g, int w, int listTop, int listHeight, int softkeyY) {
        int rowH = 44;
        int visibleRows = listHeight / rowH;

        if (contactsList == null || contactsList.size() == 0) {
            g.setColor(COLOR_TEXT_SNIPPET);
            Font fontSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(fontSmall);
            g.drawString(isLoading ? "Loading contacts..." : "No contacts synced.", w / 2, listTop + 45, Graphics.TOP | Graphics.HCENTER);
            g.drawString("Press Options -> Refresh Contacts", w / 2, listTop + 68, Graphics.TOP | Graphics.HCENTER);
            return;
        }

        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        int currentY = listTop;
        Font fontName = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font fontSnippet = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        for (int i = scrollOffset; i < contactsList.size() && currentY < softkeyY; i++) {
            Hashtable contact = (Hashtable) contactsList.elementAt(i);
            boolean isSelected = (i == selectedIndex);

            if (isSelected) {
                g.setColor(COLOR_ROW_HIGHLIGHT);
                g.fillRect(0, currentY, w, rowH);
                g.setColor(COLOR_ROW_ACCENT);
                g.fillRect(0, currentY, 3, rowH);
            }

            int avatarX = 8;
            int avatarY = currentY + 6;
            if (imgAvatar != null) {
                g.drawImage(imgAvatar, avatarX, avatarY, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(0xDFE5E7);
                g.fillRoundRect(avatarX, avatarY, 32, 32, 8, 8);
            }

            String name = MiniJson.getString(contact, "name", "Contact");
            String phone = MiniJson.getString(contact, "phone", "");
            String status = MiniJson.getString(contact, "status", "Hey there! I am using WhatsApp.");

            int textX = avatarX + 32 + 10;

            g.setFont(fontName);
            g.setColor(COLOR_TEXT_NAME);
            String displayName = name;
            if (displayName.length() > 20) displayName = displayName.substring(0, 18) + "...";
            g.drawString(displayName, textX, currentY + 5, Graphics.TOP | Graphics.LEFT);

            g.setFont(fontSnippet);
            g.setColor(COLOR_TEXT_SNIPPET);
            String sub = (phone.length() > 0 ? phone + " - " : "") + status;
            if (sub.length() > 25) sub = sub.substring(0, 23) + "...";
            g.drawString(sub, textX, currentY + 23, Graphics.TOP | Graphics.LEFT);

            g.setColor(COLOR_DIVIDER);
            g.drawLine(textX, currentY + rowH - 1, w, currentY + rowH - 1);

            currentY += rowH;
        }
    }

    private void paintStatusTab(Graphics g, int w, int listTop, int listHeight, int softkeyY) {
        Font fontName = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font fontSnippet = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        int currentY = listTop;

        // Row 0: My Status
        int myStatusH = 50;
        boolean isMyStatusSelected = (selectedIndex == 0);
        if (isMyStatusSelected) {
            g.setColor(COLOR_ROW_HIGHLIGHT);
            g.fillRect(0, currentY, w, myStatusH);
            g.setColor(COLOR_ROW_ACCENT);
            g.fillRect(0, currentY, 3, myStatusH);
        }

        // Green circle outline around user avatar
        int myAvX = 8;
        int myAvY = currentY + 8;
        g.setColor(COLOR_BADGE);
        g.drawArc(myAvX - 2, myAvY - 2, 36, 36, 0, 360);
        if (imgAvatar != null) {
            g.drawImage(imgAvatar, myAvX, myAvY, Graphics.TOP | Graphics.LEFT);
        } else {
            g.setColor(0xDFE5E7);
            g.fillRoundRect(myAvX, myAvY, 32, 32, 8, 8);
        }

        int textX = myAvX + 32 + 12;
        g.setFont(fontName);
        g.setColor(COLOR_TEXT_NAME);
        g.drawString("My Status", textX, currentY + 8, Graphics.TOP | Graphics.LEFT);

        g.setFont(fontSnippet);
        g.setColor(COLOR_TEXT_SNIPPET);
        String s = myStatus;
        if (s.length() > 24) s = s.substring(0, 22) + "...";
        g.drawString(s, textX, currentY + 26, Graphics.TOP | Graphics.LEFT);

        // Edit hint on right
        g.setColor(COLOR_ROW_ACCENT);
        g.drawString("[Edit]", w - 8, currentY + 8, Graphics.TOP | Graphics.RIGHT);

        currentY += myStatusH;

        // Section Header: RECENT UPDATES
        int headerH = 22;
        g.setColor(COLOR_SECTION_HEADER);
        g.fillRect(0, currentY, w, headerH);
        g.setColor(COLOR_TEXT_SNIPPET);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString("RECENT UPDATES", 10, currentY + 4, Graphics.TOP | Graphics.LEFT);

        currentY += headerH;

        int rowH = 46;
        for (int i = 0; i < statusList.size() && currentY < softkeyY; i++) {
            Hashtable st = (Hashtable) statusList.elementAt(i);
            boolean isSelected = (selectedIndex == i + 1);

            if (isSelected) {
                g.setColor(COLOR_ROW_HIGHLIGHT);
                g.fillRect(0, currentY, w, rowH);
                g.setColor(COLOR_ROW_ACCENT);
                g.fillRect(0, currentY, 3, rowH);
            }

            int stAvX = 8;
            int stAvY = currentY + 6;
            // Green ring indicating unviewed status
            g.setColor(COLOR_BADGE);
            g.drawArc(stAvX - 2, stAvY - 2, 36, 36, 0, 360);
            if (imgAvatar != null) {
                g.drawImage(imgAvatar, stAvX, stAvY, Graphics.TOP | Graphics.LEFT);
            }

            String sName = MiniJson.getString(st, "name", "Contact");
            String sText = MiniJson.getString(st, "status", "Available");
            String sTime = MiniJson.getString(st, "time", "Today");

            g.setFont(fontName);
            g.setColor(COLOR_TEXT_NAME);
            g.drawString(sName, textX, currentY + 6, Graphics.TOP | Graphics.LEFT);

            g.setFont(fontSnippet);
            g.setColor(COLOR_TEXT_SNIPPET);
            if (sText.length() > 24) sText = sText.substring(0, 22) + "...";
            g.drawString(sText, textX, currentY + 24, Graphics.TOP | Graphics.LEFT);

            g.setFont(fontSnippet);
            g.setColor(COLOR_TIME_GRAY);
            g.drawString(sTime, w - 8, currentY + 6, Graphics.TOP | Graphics.RIGHT);

            g.setColor(COLOR_DIVIDER);
            g.drawLine(textX, currentY + rowH - 1, w, currentY + rowH - 1);

            currentY += rowH;
        }
    }

    protected void keyPressed(int keyCode) {
        int ga = getGameAction(keyCode);

        // Nokia Softkeys
        if (keyCode == -6) {
            // LSK: Options
            showOptionsMenu();
            return;
        } else if (keyCode == -7) {
            // RSK: Exit
            midlet.exitApp();
            return;
        }

        // D-Pad Left / Right switches Tabs!
        if (ga == LEFT || keyCode == -3) {
            activeTab = (activeTab + 2) % 3; // Cycle left
            selectedIndex = 0;
            scrollOffset = 0;
            fetchData();
            return;
        } else if (ga == RIGHT || keyCode == -4) {
            activeTab = (activeTab + 1) % 3; // Cycle right
            selectedIndex = 0;
            scrollOffset = 0;
            fetchData();
            return;
        }

        // Numeric Key Shortcuts
        if (keyCode == '1') {
            activeTab = 0;
            selectedIndex = 0;
            fetchData();
            return;
        } else if (keyCode == '2') {
            activeTab = 1;
            selectedIndex = 0;
            fetchData();
            return;
        } else if (keyCode == '3') {
            activeTab = 2;
            selectedIndex = 0;
            fetchData();
            return;
        }

        // Up / Down
        int maxItems = 0;
        if (activeTab == 0) maxItems = chatList.size();
        else if (activeTab == 1) maxItems = contactsList.size();
        else if (activeTab == 2) maxItems = statusList.size() + 1;

        if (ga == UP || keyCode == -1) {
            if (selectedIndex > 0) {
                selectedIndex--;
                repaint();
            }
        } else if (ga == DOWN || keyCode == -2) {
            if (selectedIndex < maxItems - 1) {
                selectedIndex++;
                repaint();
            }
        } else if (ga == FIRE || keyCode == -5 || keyCode == 10) {
            performCenterAction();
        }
    }

    private void performCenterAction() {
        if (activeTab == 0) {
            openSelectedChat();
        } else if (activeTab == 1) {
            openSelectedContactChat();
        } else if (activeTab == 2) {
            if (selectedIndex == 0) {
                openEditStatusPrompt();
            } else if (statusList != null && selectedIndex - 1 < statusList.size()) {
                Hashtable st = (Hashtable) statusList.elementAt(selectedIndex - 1);
                String sName = MiniJson.getString(st, "name", "Status");
                String sText = MiniJson.getString(st, "status", "");
                midlet.showAlert(sName, sText, AlertType.INFO);
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

    private void openSelectedContactChat() {
        if (contactsList != null && selectedIndex >= 0 && selectedIndex < contactsList.size()) {
            Hashtable contact = (Hashtable) contactsList.elementAt(selectedIndex);
            String cid = MiniJson.getString(contact, "id", "");
            String cname = MiniJson.getString(contact, "name", "Contact");
            String phone = MiniJson.getString(contact, "phone", "");

            if (cid.length() > 0 && !cid.equals("self")) {
                midlet.openConversation(cid, cname);
            } else if (phone.length() > 0) {
                String clean = phone.replace('+', ' ').trim();
                midlet.openConversation(clean + "@s.whatsapp.net", cname);
            }
        }
    }

    private void openEditStatusPrompt() {
        final TextBox tb = new TextBox("My WhatsApp Status", myStatus, 140, TextField.ANY);
        Command cmdSave = new Command("Save", Command.OK, 1);
        Command cmdCancel = new Command("Cancel", Command.CANCEL, 2);
        tb.addCommand(cmdSave);
        tb.addCommand(cmdCancel);

        tb.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.OK) {
                    final String newSt = tb.getString().trim();
                    if (newSt.length() > 0) {
                        myStatus = newSt;
                        repaint();
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    String url = midlet.getServerUrl() + "/api/profile/status";
                                    String json = "{\"statusText\":\"" + newSt + "\"}";
                                    HttpHelper.post(url, json);
                                } catch (Exception e) {}
                            }
                        }).start();
                    }
                }
                midlet.getDisplay().setCurrent(ChatListCanvas.this);
            }
        });
        midlet.getDisplay().setCurrent(tb);
    }

    private void showOptionsMenu() {
        final List optList = new List("Options", Choice.IMPLICIT);

        if (activeTab == 0) {
            optList.append("Open Chat", null);
            optList.append("New Chat", null);
            optList.append("Refresh Chats", null);
            optList.append("WhatsApp Settings", null);
            optList.append("Link Multi-Device", null);
            optList.append("Exit WhatsApp", null);
        } else if (activeTab == 1) {
            optList.append("Chat with Contact", null);
            optList.append("New Chat", null);
            optList.append("Refresh Contacts", null);
            optList.append("WhatsApp Settings", null);
            optList.append("Exit WhatsApp", null);
        } else {
            optList.append("Set My Status", null);
            optList.append("Refresh Statuses", null);
            optList.append("WhatsApp Settings", null);
            optList.append("Exit WhatsApp", null);
        }

        Command cmdSelect = new Command("Select", Command.OK, 1);
        Command cmdBack = new Command("Back", Command.BACK, 2);
        optList.addCommand(cmdSelect);
        optList.addCommand(cmdBack);

        optList.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c == List.SELECT_COMMAND || c.getCommandType() == Command.OK) {
                    int sel = optList.getSelectedIndex();
                    String action = optList.getString(sel);

                    midlet.getDisplay().setCurrent(ChatListCanvas.this);

                    if (action.equals("Open Chat")) {
                        openSelectedChat();
                    } else if (action.equals("Chat with Contact")) {
                        openSelectedContactChat();
                    } else if (action.equals("New Chat")) {
                        midlet.showNewChatPrompt();
                    } else if (action.startsWith("Refresh")) {
                        fetchData();
                    } else if (action.equals("WhatsApp Settings")) {
                        midlet.showSettings();
                    } else if (action.equals("Link Multi-Device")) {
                        midlet.showPairingScreen();
                    } else if (action.equals("Set My Status")) {
                        openEditStatusPrompt();
                    } else if (action.equals("Exit WhatsApp")) {
                        midlet.exitApp();
                    }
                } else if (c.getCommandType() == Command.BACK) {
                    midlet.getDisplay().setCurrent(ChatListCanvas.this);
                }
            }
        });

        midlet.getDisplay().setCurrent(optList);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdOpen) {
            performCenterAction();
        } else if (c == cmdOptions) {
            showOptionsMenu();
        } else if (c == cmdExit) {
            midlet.exitApp();
        }
    }
}
