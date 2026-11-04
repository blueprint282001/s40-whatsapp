package com.whatsapp.s40.ui;

import com.whatsapp.s40.WhatsAppMIDlet;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

public class SettingsScreen extends Form implements CommandListener {

    private WhatsAppMIDlet midlet;
    private TextField txtServerUrl;
    private Command cmdSave;
    private Command cmdBack;

    public SettingsScreen(WhatsAppMIDlet midlet) {
        super("WhatsApp Settings");
        this.midlet = midlet;

        txtServerUrl = new TextField("Server URL (Cloud/IP):", midlet.getServerUrl(), 120, TextField.URL);
        append(txtServerUrl);

        append(new StringItem("Example:", "http://192.168.1.10:3000\nor\nhttp://myserver.onrender.com"));

        cmdSave = new Command("Save", Command.OK, 1);
        cmdBack = new Command("Back", Command.BACK, 2);

        addCommand(cmdSave);
        addCommand(cmdBack);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdSave) {
            String url = txtServerUrl.getString().trim();
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            midlet.saveServerUrl(url);
            midlet.showChatList();
        } else if (c == cmdBack) {
            midlet.showChatList();
        }
    }
}
