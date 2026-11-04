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
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class ConversationCanvas extends Canvas implements CommandListener {

    private WhatsAppMIDlet midlet;
    private String chatId;
    private String chatName;
    private Vector messages = new Vector();

    private int scrollY = 0;
    private int totalContentHeight = 0;

    // Commands
    private Command cmdSend;
    private Command cmdRefresh;
    private Command cmdBack;

    // Colors
    private static final int COLOR_STATUS = 0x054C44;
    private static final int COLOR_HEADER = 0x075E54;
    private static final int COLOR_BG = 0xECE5DD; // Classic WhatsApp beige
    private static final int COLOR_BUBBLE_IN = 0xFFFFFF;
    private static final int COLOR_BUBBLE_OUT = 0xDCF8C6;
    private static final int COLOR_BORDER_IN = 0xD8D8D8;
    private static final int COLOR_BORDER_OUT = 0xC2E2A8;
    private static final int COLOR_TEXT = 0x111111;
    private static final int COLOR_TIME = 0x888888;
    private static final int COLOR_TICK = 0x34B7F1;
    private static final int COLOR_SOFTKEY_BG = 0x111111;

    public ConversationCanvas(WhatsAppMIDlet midlet, String chatId, String chatName) {
        this.midlet = midlet;
        this.chatId = chatId;
        this.chatName = chatName;

        // Full Screen Mode
        setFullScreenMode(true);

        cmdSend = new Command("Type / Send", Command.OK, 1);
        cmdRefresh = new Command("Refresh", Command.ITEM, 2);
        cmdBack = new Command("Back", Command.BACK, 3);

        addCommand(cmdSend);
        addCommand(cmdRefresh);
        addCommand(cmdBack);
        setCommandListener(this);
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
        int viewHeight = getHeight() - (14 + 28 + 20); // total view height
        if (totalContentHeight > viewHeight) {
            scrollY = totalContentHeight - viewHeight;
        } else {
            scrollY = 0;
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // 1. Nokia System Status Bar (Top 14px)
        int statusBarH = 14;
        g.setColor(COLOR_STATUS);
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

        // 2. Chat Header Bar (28px)
        int headerH = 28;
        int headerY = statusBarH;
        g.setColor(COLOR_HEADER);
        g.fillRect(0, headerY, w, headerH);

        Font fontHeader = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(fontHeader);
        g.setColor(0xFFFFFF);
        String title = chatName != null ? chatName : "Chat";
        if (title.length() > 18) title = title.substring(0, 16) + "...";
        g.drawString("◀ " + title, 6, headerY + 4, Graphics.TOP | Graphics.LEFT);

        Font fontSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(fontSmall);
        g.setColor(0xA2D2CE);
        g.drawString("online", 20, headerY + 16, Graphics.TOP | Graphics.LEFT);

        // 3. Bottom Softkey Bar (20px)
        int softkeyH = 20;
        int softkeyY = h - softkeyH;
        g.setColor(COLOR_SOFTKEY_BG);
        g.fillRect(0, softkeyY, w, softkeyH);

        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.setColor(0xEEEEEE);
        g.drawString("Options", 6, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
        g.setColor(0x25D366);
        g.drawString("Type / Send", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xEEEEEE);
        g.drawString("Back", w - 6, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);

        // 4. Conversation History Area
        int clipTop = headerY + headerH;
        int clipHeight = softkeyY - clipTop;
        g.setClip(0, clipTop, w, clipHeight);

        // Background Wallpaper
        g.setColor(COLOR_BG);
        g.fillRect(0, clipTop, w, clipHeight);

        // Wallpaper subtle grid pattern
        g.setColor(0xE5DDD4);
        for (int gy = clipTop; gy < softkeyY; gy += 16) {
            g.drawLine(0, gy, w, gy);
        }

        int currentY = clipTop + 8 - scrollY;
        int maxBubbleWidth = (w * 80) / 100;

        if (messages.size() == 0) {
            g.setColor(0x888888);
            g.setFont(fontSmall);
            g.drawString("Say hello to start chat!", w / 2, clipTop + 40, Graphics.TOP | Graphics.HCENTER);
            g.drawString("Press Center key to type", w / 2, clipTop + 60, Graphics.TOP | Graphics.HCENTER);
        }

        for (int i = 0; i < messages.size(); i++) {
            Hashtable msg = (Hashtable) messages.elementAt(i);
            boolean fromMe = MiniJson.getBoolean(msg, "fromMe", false);
            String text = MiniJson.getString(msg, "text", "");

            Vector lines = wrapText(text, maxBubbleWidth - 16, fontSmall);
            int lineHeight = fontSmall.getHeight();
            int bubbleContentH = (lines.size() * lineHeight) + 16;
            int bubbleW = 0;

            for (int l = 0; l < lines.size(); l++) {
                int lw = fontSmall.stringWidth((String) lines.elementAt(l));
                if (lw > bubbleW) bubbleW = lw;
            }
            bubbleW = Math.max(bubbleW + 22, 64);

            int bubbleX;
            if (fromMe) {
                bubbleX = w - bubbleW - 8;
                g.setColor(COLOR_BUBBLE_OUT);
            } else {
                bubbleX = 8;
                g.setColor(COLOR_BUBBLE_IN);
            }

            // Draw bubble body
            g.fillRoundRect(bubbleX, currentY, bubbleW, bubbleContentH, 8, 8);
            g.setColor(fromMe ? COLOR_BORDER_OUT : COLOR_BORDER_IN);
            g.drawRoundRect(bubbleX, currentY, bubbleW, bubbleContentH, 8, 8);

            // Draw text lines
            g.setColor(COLOR_TEXT);
            g.setFont(fontSmall);
            for (int l = 0; l < lines.size(); l++) {
                g.drawString((String) lines.elementAt(l), bubbleX + 8, currentY + 4 + (l * lineHeight), Graphics.TOP | Graphics.LEFT);
            }

            // Draw timestamp & checkmark
            g.setColor(COLOR_TIME);
            g.setFont(fontMicro);
            int metaY = currentY + bubbleContentH - 12;

            if (fromMe) {
                g.setColor(COLOR_TICK);
                g.drawString("✓✓", bubbleX + bubbleW - 14, metaY, Graphics.TOP | Graphics.LEFT);
            }

            currentY += bubbleContentH + 6;
        }

        totalContentHeight = (currentY + scrollY) - (clipTop + 8);

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
                lines.addElement(text.substring(start, start + count - 1));
                start += (count - 1);
            }
        }
        return lines;
    }

    protected void keyPressed(int keyCode) {
        int ga = getGameAction(keyCode);
        int viewHeight = getHeight() - (14 + 28 + 20);

        // Nokia Softkeys
        if (keyCode == -6) {
            // Left Softkey -> Options
            openInputBox();
            return;
        } else if (keyCode == -7) {
            // Right Softkey -> Back
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

        // Softkey row
        if (y >= h - 20) {
            if (x < w / 3) {
                openInputBox();
            } else if (x > (2 * w) / 3) {
                midlet.showChatList();
            } else {
                openInputBox();
            }
            return;
        }

        // Top bar back button
        if (y < 42 && x < 50) {
            midlet.showChatList();
            return;
        }

        // Tap on chat area opens input
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

    private void sendMessage(final String text) {
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
