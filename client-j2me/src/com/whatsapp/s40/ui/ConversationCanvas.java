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
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class ConversationCanvas extends Canvas implements CommandListener {

    private WhatsAppMIDlet midlet;
    private String chatId;
    private String chatName;
    private Vector messages = new Vector();

    private int scrollY = 0;
    private int totalContentHeight = 0;
    private boolean isPolling = false;

    // Commands
    private Command cmdSend;
    private Command cmdRefresh;
    private Command cmdBack;

    // Colors
    private static final int COLOR_HEADER = 0x075E54;
    private static final int COLOR_BG = 0xECE5DD; // Classic WhatsApp background
    private static final int COLOR_BUBBLE_IN = 0xFFFFFF;
    private static final int COLOR_BUBBLE_OUT = 0xDCF8C6;
    private static final int COLOR_BUBBLE_BORDER = 0xCCCCCC;
    private static final int COLOR_TEXT = 0x000000;
    private static final int COLOR_TIME = 0x888888;
    private static final int COLOR_TICK = 0x34B7F1;

    public ConversationCanvas(WhatsAppMIDlet midlet, String chatId, String chatName) {
        this.midlet = midlet;
        this.chatId = chatId;
        this.chatName = chatName;

        cmdSend = new Command("Type / Send", Command.OK, 1);
        cmdRefresh = new Command("Refresh", Command.ITEM, 2);
        cmdBack = new Command("Back", Command.BACK, 3);

        addCommand(cmdSend);
        addCommand(cmdRefresh);
        addCommand(cmdBack);
        setCommandListener(this);
    }

    public void startPolling() {
        isPolling = true;
        fetchMessages();
    }

    public void stopPolling() {
        isPolling = false;
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
        int viewHeight = getHeight() - 32;
        if (totalContentHeight > viewHeight) {
            scrollY = totalContentHeight - viewHeight;
        } else {
            scrollY = 0;
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Header bar
        int headerHeight = 32;
        g.setColor(COLOR_HEADER);
        g.fillRect(0, 0, w, headerHeight);

        Font fontHeader = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        g.setFont(fontHeader);
        g.setColor(0xFFFFFF);
        String title = chatName != null ? chatName : "Chat";
        if (title.length() > 20) title = title.substring(0, 18) + "...";
        g.drawString(title, 8, 4, Graphics.TOP | Graphics.LEFT);

        Font fontSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(fontSmall);
        g.drawString("online", 8, 18, Graphics.TOP | Graphics.LEFT);

        // Message clip area
        g.setClip(0, headerHeight, w, h - headerHeight);

        int currentY = headerHeight + 8 - scrollY;
        int maxBubbleWidth = (w * 78) / 100;

        for (int i = 0; i < messages.size(); i++) {
            Hashtable msg = (Hashtable) messages.elementAt(i);
            boolean fromMe = MiniJson.getBoolean(msg, "fromMe", false);
            String text = MiniJson.getString(msg, "text", "");

            Vector lines = wrapText(text, maxBubbleWidth - 16, fontSmall);
            int lineHeight = fontSmall.getHeight();
            int bubbleContentHeight = (lines.size() * lineHeight) + 16;
            int bubbleWidth = 0;

            for (int l = 0; l < lines.size(); l++) {
                int lw = fontSmall.stringWidth((String) lines.elementAt(l));
                if (lw > bubbleWidth) bubbleWidth = lw;
            }
            bubbleWidth = Math.max(bubbleWidth + 20, 60);

            int bubbleX;
            if (fromMe) {
                bubbleX = w - bubbleWidth - 8;
                g.setColor(COLOR_BUBBLE_OUT);
            } else {
                bubbleX = 8;
                g.setColor(COLOR_BUBBLE_IN);
            }

            // Draw bubble with border
            g.fillRoundRect(bubbleX, currentY, bubbleWidth, bubbleContentHeight, 8, 8);
            g.setColor(COLOR_BUBBLE_BORDER);
            g.drawRoundRect(bubbleX, currentY, bubbleWidth, bubbleContentHeight, 8, 8);

            // Draw text lines
            g.setColor(COLOR_TEXT);
            g.setFont(fontSmall);
            for (int l = 0; l < lines.size(); l++) {
                g.drawString((String) lines.elementAt(l), bubbleX + 8, currentY + 4 + (l * lineHeight), Graphics.TOP | Graphics.LEFT);
            }

            // Draw time & tick mark
            g.setColor(COLOR_TIME);
            int metaY = currentY + bubbleContentHeight - 12;
            if (fromMe) {
                g.drawString("✓✓", bubbleX + bubbleWidth - 14, metaY, Graphics.TOP | Graphics.LEFT);
            }

            currentY += bubbleContentHeight + 6;
        }

        totalContentHeight = (currentY + scrollY) - (headerHeight + 8);

        // Reset clip
        g.setClip(0, 0, w, h);
    }

    private Vector wrapText(String text, int maxWidth, Font font) {
        Vector lines = new Vector();
        if (text == null || text.length() == 0) {
            lines.addElement("");
            return lines;
        }

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
            // Find last space
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
                // Break word
                lines.addElement(text.substring(start, start + count - 1));
                start += (count - 1);
            }
        }
        return lines;
    }

    protected void keyPressed(int keyCode) {
        int ga = getGameAction(keyCode);
        int viewHeight = getHeight() - 32;

        if (ga == UP) {
            scrollY = Math.max(0, scrollY - 20);
            repaint();
        } else if (ga == DOWN) {
            int maxScroll = Math.max(0, totalContentHeight - viewHeight);
            scrollY = Math.min(maxScroll, scrollY + 20);
            repaint();
        } else if (ga == FIRE || keyCode == 10 || keyCode == -5) {
            openInputBox();
        }
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

    private void sendMessage(final String text) {
        // Optimistically add to UI
        Hashtable temp = new Hashtable();
        temp.put("fromMe", new Boolean(true));
        temp.put("text", text);
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

    public void commandAction(Command c, Displayable d) {
        if (c == cmdSend) {
            openInputBox();
        } else if (c == cmdRefresh) {
            fetchMessages();
        } else if (c == cmdBack) {
            stopPolling();
            midlet.showChatList();
        }
    }
}
