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

public class ConversationCanvas extends Canvas implements CommandListener {

    private WhatsAppMIDlet midlet;
    private String chatId;
    private String chatName;
    private boolean isGroup;
    private Vector messages = new Vector();

    private int scrollY = 0;
    private int totalContentHeight = 0;

    // Authentic Graphics
    private Image imgDoodle;
    private Image imgAvatar;
    private Image imgGroupAvatar;
    private Image imgTickBlue;
    private Image imgTickDouble;
    private Image imgCamera;
    private Image imgMic;

    // Commands
    private Command cmdSend;
    private Command cmdOptions;
    private Command cmdBack;

    // Official WhatsApp Colors
    private static final int COLOR_HEADER = 0x075E54;
    private static final int COLOR_STATUS_TEXT = 0xA2D2CE;
    private static final int COLOR_BUBBLE_IN = 0xFFFFFF;
    private static final int COLOR_BUBBLE_OUT = 0xDCF8C6;
    private static final int COLOR_BORDER_IN = 0xD1D7DB;
    private static final int COLOR_BORDER_OUT = 0xC2E2A8;
    private static final int COLOR_TEXT = 0x111111;
    private static final int COLOR_TIME = 0x8696A0;
    private static final int COLOR_BOTTOM_BAR = 0x1F2C34;
    private static final int COLOR_DATE_PILL_BG = 0xDDE8ED;
    private static final int COLOR_DATE_PILL_TEXT = 0x54656F;

    // Group Participant Sender Colors
    private static final int[] GROUP_COLORS = {
        0x1F7A8C, 0xD8315B, 0x3E8914, 0x9D4EDD, 0xEA580C, 0x0284C7, 0x7E22CE
    };

    public ConversationCanvas(WhatsAppMIDlet midlet, String chatId, String chatName) {
        this.midlet = midlet;
        this.chatId = chatId;
        this.chatName = chatName;
        this.isGroup = (chatId != null && chatId.indexOf("@g.us") != -1);

        setFullScreenMode(true);
        loadAssets();

        cmdSend = new Command("Type / Send", Command.OK, 1);
        cmdOptions = new Command("Options", Command.ITEM, 2);
        cmdBack = new Command("Back", Command.BACK, 3);

        addCommand(cmdSend);
        addCommand(cmdOptions);
        addCommand(cmdBack);
        setCommandListener(this);
    }

    private void loadAssets() {
        try { imgDoodle = Image.createImage("/doodle_bg.png"); } catch (Exception e) {}
        try { imgAvatar = Image.createImage("/avatar_default.png"); } catch (Exception e) {}
        try { imgGroupAvatar = Image.createImage("/avatar_group.png"); } catch (Exception e) {}
        try { imgTickBlue = Image.createImage("/tick_blue.png"); } catch (Exception e) {}
        try { imgTickDouble = Image.createImage("/tick_double.png"); } catch (Exception e) {}
        try { imgCamera = Image.createImage("/icon_camera.png"); } catch (Exception e) {}
        try { imgMic = Image.createImage("/icon_mic.png"); } catch (Exception e) {}
    }

    public void startPolling() {
        fetchMessages();
    }

    public void stopPolling() {
    }

    public void fetchMessages() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = midlet.getServerUrl() + "/api/messages?chatId=" + chatId;
                    String res = HttpHelper.get(url);
                    Hashtable obj = (Hashtable) MiniJson.parse(res);
                    if (obj != null) {
                        Vector list = (Vector) obj.get("messages");
                        if (list != null) {
                            messages = list;
                            scrollToBottom();
                            repaint();
                        }
                    }
                } catch (Exception e) {}
            }
        }).start();
    }

    private void scrollToBottom() {
        int viewHeight = getHeight() - (36 + 20);
        if (totalContentHeight > viewHeight) {
            scrollY = totalContentHeight - viewHeight;
        } else {
            scrollY = 0;
        }
    }

    protected void paint(Graphics g) {
        try {
            int w = getWidth();
            int h = getHeight();

            // 1. WhatsApp Conversation Header (36px)
            int headerH = 36;
            g.setColor(COLOR_HEADER);
            g.fillRect(0, 0, w, headerH);

            // Back Arrow ◀
            Font fontArrow = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            g.setFont(fontArrow);
            g.setColor(0xFFFFFF);
            g.drawString("◀", 6, 11, Graphics.TOP | Graphics.LEFT);

            // Contact / Group Avatar in Header
            int avatarH = 24;
            if (isGroup && imgGroupAvatar != null) {
                g.drawImage(imgGroupAvatar, 18, 6, Graphics.TOP | Graphics.LEFT);
            } else if (imgAvatar != null) {
                g.drawImage(imgAvatar, 18, 6, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(0xDFE5E7);
                g.fillRoundRect(18, 6, avatarH, avatarH, 6, 6);
            }

            // Contact / Group Subject Name
            Font fontName = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            g.setFont(fontName);
            g.setColor(0xFFFFFF);
            String title = chatName != null ? chatName : "Chat";
            if (title.length() > 16) title = title.substring(0, 14) + "...";
            g.drawString(title, 46, 5, Graphics.TOP | Graphics.LEFT);

            // Subtitle (online status / group indicator)
            Font fontSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(fontSmall);
            g.setColor(COLOR_STATUS_TEXT);
            g.drawString(isGroup ? "Group info" : "online", 46, 19, Graphics.TOP | Graphics.LEFT);

            // Header right 3-dots menu
            g.setColor(0xFFFFFF);
            g.fillRect(w - 12, 11, 2, 2);
            g.fillRect(w - 12, 16, 2, 2);
            g.fillRect(w - 12, 21, 2, 2);

            // 2. Bottom Softkey Bar (20px)
            int softkeyH = 20;
            int softkeyY = h - softkeyH;
            g.setColor(COLOR_BOTTOM_BAR);
            g.fillRect(0, softkeyY, w, softkeyH);

            g.setFont(fontArrow);
            g.setColor(0xDFE5E7);
            g.drawString("Options", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(0x25D366);
            g.drawString("Type / Send", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0xDFE5E7);
            g.drawString("Back", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);

            // 3. Conversation Area with Wallpaper
            int clipTop = headerH;
            int clipHeight = softkeyY - clipTop;

            int origClipX = g.getClipX();
            int origClipY = g.getClipY();
            int origClipW = g.getClipWidth();
            int origClipH = g.getClipHeight();

            if (w > 0 && clipHeight > 0) {
                g.setClip(0, clipTop, w, clipHeight);
            }

            // Seamless tile authentic WhatsApp doodle wallpaper
            if (imgDoodle != null) {
                int dw = imgDoodle.getWidth();
                int dh = imgDoodle.getHeight();
                for (int dy = clipTop; dy < softkeyY; dy += dh) {
                    for (int dx = 0; dx < w; dx += dw) {
                        g.drawImage(imgDoodle, dx, dy, Graphics.TOP | Graphics.LEFT);
                    }
                }
            } else {
                g.setColor(0xECE5DD);
                g.fillRect(0, clipTop, w, clipHeight);
            }

            int currentY = clipTop + 8 - scrollY;

            // Date Separator Pill: TODAY
            int pillW = 60;
            int pillH = 16;
            int pillX = (w - pillW) / 2;
            g.setColor(COLOR_DATE_PILL_BG);
            g.fillRoundRect(pillX, currentY, pillW, pillH, 8, 8);
            g.setColor(COLOR_DATE_PILL_TEXT);
            g.setFont(fontSmall);
            g.drawString("TODAY", pillX + 12, currentY + 2, Graphics.TOP | Graphics.LEFT);

            currentY += pillH + 10;

            int maxBubbleWidth = (w * 78) / 100;

            if (messages.size() == 0) {
                g.setColor(COLOR_DATE_PILL_TEXT);
                g.drawString("Messages are end-to-end encrypted", w / 2, currentY + 30, Graphics.TOP | Graphics.HCENTER);
                g.drawString("Press Center key to type a message", w / 2, currentY + 50, Graphics.TOP | Graphics.HCENTER);
            }

            Font fontSender = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

            for (int i = 0; i < messages.size(); i++) {
                Hashtable msg = (Hashtable) messages.elementAt(i);
                boolean fromMe = MiniJson.getBoolean(msg, "fromMe", false);
                String text = MiniJson.getString(msg, "text", "");
                String participant = MiniJson.getString(msg, "participantName", "Participant");
                boolean isPhoto = MiniJson.getBoolean(msg, "isPhoto", false);
                boolean isVoice = MiniJson.getBoolean(msg, "isVoice", false);

                boolean showSenderHeader = isGroup && !fromMe;
                int senderHeaderH = showSenderHeader ? 14 : 0;

                Vector lines = wrapText(text, maxBubbleWidth - 16, fontSmall);
                int lineHeight = fontSmall.getHeight();
                int bubbleContentH = (lines.size() * lineHeight) + 16 + senderHeaderH;
                if (isPhoto || isVoice) bubbleContentH += 14;

                int bubbleW = 0;
                if (showSenderHeader) {
                    bubbleW = fontSender.stringWidth(participant);
                }
                for (int l = 0; l < lines.size(); l++) {
                    int lw = fontSmall.stringWidth((String) lines.elementAt(l));
                    if (lw > bubbleW) bubbleW = lw;
                }
                bubbleW = Math.max(bubbleW + 28, 80);

                int bubbleX;
                if (fromMe) {
                    bubbleX = w - bubbleW - 10;
                    // Outgoing Olive Green Bubble
                    g.setColor(COLOR_BUBBLE_OUT);
                    g.fillRoundRect(bubbleX, currentY, bubbleW, bubbleContentH, 8, 8);
                    g.setColor(COLOR_BORDER_OUT);
                    g.drawRoundRect(bubbleX, currentY, bubbleW, bubbleContentH, 8, 8);

                    // Speech Tail pointing right
                    g.setColor(COLOR_BUBBLE_OUT);
                    g.fillTriangle(bubbleX + bubbleW, currentY + 2, bubbleX + bubbleW + 5, currentY + 8, bubbleX + bubbleW, currentY + 12);
                    g.setColor(COLOR_BORDER_OUT);
                    g.drawLine(bubbleX + bubbleW, currentY + 2, bubbleX + bubbleW + 5, currentY + 8);
                    g.drawLine(bubbleX + bubbleW + 5, currentY + 8, bubbleX + bubbleW, currentY + 12);
                } else {
                    bubbleX = 10;
                    // Incoming White Bubble
                    g.setColor(COLOR_BUBBLE_IN);
                    g.fillRoundRect(bubbleX, currentY, bubbleW, bubbleContentH, 8, 8);
                    g.setColor(COLOR_BORDER_IN);
                    g.drawRoundRect(bubbleX, currentY, bubbleW, bubbleContentH, 8, 8);

                    // Speech Tail pointing left
                    g.setColor(COLOR_BUBBLE_IN);
                    g.fillTriangle(bubbleX, currentY + 2, bubbleX - 5, currentY + 8, bubbleX, currentY + 12);
                    g.setColor(COLOR_BORDER_IN);
                    g.drawLine(bubbleX, currentY + 2, bubbleX - 5, currentY + 8);
                    g.drawLine(bubbleX - 5, currentY + 8, bubbleX, currentY + 12);
                }

                int contentY = currentY + 4;

                // In Group Chat: draw participant sender name in color!
                if (showSenderHeader) {
                    int cIdx = Math.abs(participant.hashCode()) % GROUP_COLORS.length;
                    g.setColor(GROUP_COLORS[cIdx]);
                    g.setFont(fontSender);
                    g.drawString(participant, bubbleX + 8, contentY, Graphics.TOP | Graphics.LEFT);
                    contentY += senderHeaderH;
                }

                // Media Preview Indicators
                if (isPhoto && imgCamera != null) {
                    g.drawImage(imgCamera, bubbleX + 8, contentY + 1, Graphics.TOP | Graphics.LEFT);
                    g.setColor(COLOR_TEXT);
                    g.setFont(fontSmall);
                    g.drawString("Photo", bubbleX + 26, contentY, Graphics.TOP | Graphics.LEFT);
                    contentY += 14;
                } else if (isVoice && imgMic != null) {
                    g.drawImage(imgMic, bubbleX + 8, contentY + 1, Graphics.TOP | Graphics.LEFT);
                    g.setColor(COLOR_TEXT);
                    g.setFont(fontSmall);
                    g.drawString("Voice note (0:15)", bubbleX + 24, contentY, Graphics.TOP | Graphics.LEFT);
                    contentY += 14;
                }

                // Text lines
                g.setColor(COLOR_TEXT);
                g.setFont(fontSmall);
                for (int l = 0; l < lines.size(); l++) {
                    g.drawString((String) lines.elementAt(l), bubbleX + 8, contentY + (l * lineHeight), Graphics.TOP | Graphics.LEFT);
                }

                // Timestamp at bottom-right
                int metaY = currentY + bubbleContentH - 12;
                g.setColor(COLOR_TIME);
                g.drawString("11:35", bubbleX + bubbleW - (fromMe ? 28 : 12), metaY, Graphics.TOP | Graphics.RIGHT);

                // Double Blue Tick for outgoing messages
                if (fromMe) {
                    if (imgTickBlue != null) {
                        g.drawImage(imgTickBlue, bubbleX + bubbleW - 14, metaY + 2, Graphics.TOP | Graphics.LEFT);
                    } else if (imgTickDouble != null) {
                        g.drawImage(imgTickDouble, bubbleX + bubbleW - 14, metaY + 2, Graphics.TOP | Graphics.LEFT);
                    }
                }

                currentY += bubbleContentH + 6;
            }

            totalContentHeight = (currentY + scrollY) - (clipTop + 8);
            g.setClip(origClipX, origClipY, origClipW, origClipH);
        } catch (Throwable t) {
            // Guarantee paint never crashes
        }
    }

    private Vector wrapText(String text, int maxWidth, Font font) {
        Vector lines = new Vector();
        if (text == null || text.length() == 0) {
            lines.addElement("");
            return lines;
        }

        if (maxWidth < 20) maxWidth = 100;

        int start = 0;
        int len = text.length();

        while (start < len) {
            int count = 1;
            while (start + count <= len && font.substringWidth(text, start, count) <= maxWidth) {
                count++;
            }
            if (start + count > len) {
                lines.addElement(text.substring(start));
                break;
            }
            int spaceIdx = -1;
            for (int i = start + count - 1; i >= start; i--) {
                if (text.charAt(i) == ' ') {
                    spaceIdx = i;
                    break;
                }
            }
            if (spaceIdx > start) {
                lines.addElement(text.substring(start, spaceIdx));
                start = spaceIdx + 1;
            } else {
                int end = Math.max(start + 1, start + count - 1);
                if (end > len) end = len;
                lines.addElement(text.substring(start, end));
                start = end;
            }
        }
        return lines;
    }

    protected void keyPressed(int keyCode) {
        int ga = getGameAction(keyCode);
        int viewHeight = getHeight() - (36 + 20);

        // Nokia Softkeys
        if (keyCode == -6) {
            showConversationOptions();
            return;
        } else if (keyCode == -7) {
            stopPolling();
            midlet.showChatList();
            return;
        }

        if (ga == UP || keyCode == -1) {
            scrollY = Math.max(0, scrollY - 24);
            repaint();
        } else if (ga == DOWN || keyCode == -2) {
            int maxScroll = Math.max(0, totalContentHeight - viewHeight);
            scrollY = Math.min(maxScroll, scrollY + 24);
            repaint();
        } else if (ga == FIRE || keyCode == 10 || keyCode == -5) {
            openInputBox();
        }
    }

    protected void pointerPressed(int x, int y) {
        int w = getWidth();
        int h = getHeight();

        // Bottom Softkeys
        if (y >= h - 22) {
            if (x < w / 3) {
                showConversationOptions();
            } else if (x > (2 * w) / 3) {
                stopPolling();
                midlet.showChatList();
            } else {
                openInputBox();
            }
            return;
        }

        // Top Header back button
        if (y < 36 && x < 45) {
            stopPolling();
            midlet.showChatList();
            return;
        }

        openInputBox();
    }

    private void openInputBox() {
        final TextBox tb = new TextBox("Message to " + chatName, "", 500, TextField.ANY);
        Command cmdOk = new Command("Send", Command.OK, 1);
        Command cmdCancel = new Command("Cancel", Command.CANCEL, 2);
        tb.addCommand(cmdOk);
        tb.addCommand(cmdCancel);

        tb.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.OK) {
                    final String msg = tb.getString().trim();
                    if (msg.length() > 0) {
                        sendMessage(msg);
                    }
                }
                midlet.getDisplay().setCurrent(ConversationCanvas.this);
            }
        });

        midlet.getDisplay().setCurrent(tb);
    }

    private void openMediaSendPrompt() {
        final TextBox tb = new TextBox("Send Photo Caption", "", 120, TextField.ANY);
        Command cmdSendPhoto = new Command("Send Photo", Command.OK, 1);
        Command cmdCancel = new Command("Cancel", Command.CANCEL, 2);
        tb.addCommand(cmdSendPhoto);
        tb.addCommand(cmdCancel);

        tb.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.OK) {
                    final String cap = tb.getString().trim();
                    sendPhotoMessage(cap);
                }
                midlet.getDisplay().setCurrent(ConversationCanvas.this);
            }
        });

        midlet.getDisplay().setCurrent(tb);
    }

    private void showConversationOptions() {
        final List opt = new List("Options", Choice.IMPLICIT);
        opt.append("Type Message", null);
        opt.append("Attach / Send Photo", null);
        opt.append("View Chat / Group Info", null);
        opt.append("Refresh Messages", null);
        opt.append("Clear Messages", null);
        opt.append("Back to Chats", null);

        Command cmdSelect = new Command("Select", Command.OK, 1);
        Command cmdBack = new Command("Back", Command.BACK, 2);
        opt.addCommand(cmdSelect);
        opt.addCommand(cmdBack);

        opt.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c == List.SELECT_COMMAND || c.getCommandType() == Command.OK) {
                    int idx = opt.getSelectedIndex();
                    midlet.getDisplay().setCurrent(ConversationCanvas.this);

                    if (idx == 0) {
                        openInputBox();
                    } else if (idx == 1) {
                        openMediaSendPrompt();
                    } else if (idx == 2) {
                        String info = "Chat: " + chatName + "\nJID: " + chatId + "\nType: " + (isGroup ? "Group Chat" : "Direct Message");
                        midlet.showAlert("Info", info, AlertType.INFO);
                    } else if (idx == 3) {
                        fetchMessages();
                    } else if (idx == 4) {
                        messages.removeAllElements();
                        repaint();
                    } else if (idx == 5) {
                        stopPolling();
                        midlet.showChatList();
                    }
                } else if (c.getCommandType() == Command.BACK) {
                    midlet.getDisplay().setCurrent(ConversationCanvas.this);
                }
            }
        });

        midlet.getDisplay().setCurrent(opt);
    }

    private void sendMessage(final String text) {
        Hashtable temp = new Hashtable();
        temp.put("fromMe", new Boolean(true));
        temp.put("text", text);
        temp.put("isPhoto", new Boolean(false));
        temp.put("isVoice", new Boolean(false));
        temp.put("timestamp", new Long(System.currentTimeMillis()));
        messages.addElement(temp);
        scrollToBottom();
        repaint();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = midlet.getServerUrl() + "/api/send";
                    String jsonBody = "{\"chatId\":\"" + chatId + "\",\"message\":\"" + text + "\"}";
                    HttpHelper.post(url, jsonBody);
                    fetchMessages();
                } catch (Exception e) {}
            }
        }).start();
    }

    private void sendPhotoMessage(final String caption) {
        Hashtable temp = new Hashtable();
        temp.put("fromMe", new Boolean(true));
        temp.put("text", "[Photo] " + caption);
        temp.put("isPhoto", new Boolean(true));
        temp.put("isVoice", new Boolean(false));
        temp.put("timestamp", new Long(System.currentTimeMillis()));
        messages.addElement(temp);
        scrollToBottom();
        repaint();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = midlet.getServerUrl() + "/api/send-media";
                    String jsonBody = "{\"chatId\":\"" + chatId + "\",\"caption\":\"" + caption + "\"}";
                    HttpHelper.post(url, jsonBody);
                    fetchMessages();
                } catch (Exception e) {}
            }
        }).start();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdSend) {
            openInputBox();
        } else if (c == cmdOptions) {
            showConversationOptions();
        } else if (c == cmdBack) {
            stopPolling();
            midlet.showChatList();
        }
    }
}
