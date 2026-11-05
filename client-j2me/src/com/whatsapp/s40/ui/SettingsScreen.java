package com.whatsapp.s40.ui;

import com.whatsapp.s40.WhatsAppMIDlet;
import com.whatsapp.s40.net.HttpHelper;
import com.whatsapp.s40.util.MiniJson;
import java.util.Hashtable;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

public class SettingsScreen extends Form implements CommandListener, ItemStateListener {

    private WhatsAppMIDlet midlet;

    // Profile Settings
    private TextField txtProfileName;
    private ChoiceGroup choiceStatus;
    private TextField txtCustomStatus;
    private StringItem lblConnectedPhone;

    // Server Settings
    private TextField txtServerUrl;

    // About
    private StringItem lblAbout;

    // Commands
    private Command cmdSave;
    private Command cmdTestConn;
    private Command cmdBack;

    private static final String[] STATUS_PRESETS = {
        "Available",
        "Busy",
        "At school",
        "At the movies",
        "At work",
        "Battery about to die",
        "Can't talk, WhatsApp only",
        "In a meeting",
        "At the gym",
        "Sleeping",
        "Urgent calls only",
        "Custom..."
    };

    public SettingsScreen(WhatsAppMIDlet midlet) {
        super("WhatsApp Settings");
        this.midlet = midlet;

        try {
            // 1. Profile Section
            append(new StringItem(null, "--- [ PROFILE ] ---"));
            txtProfileName = new TextField("Profile Name:", "Nokia User", 35, TextField.ANY);
            append(txtProfileName);

            choiceStatus = new ChoiceGroup("Status:", Choice.EXCLUSIVE, STATUS_PRESETS, null);
            try {
                choiceStatus.setSelectedIndex(0, true);
            } catch (Throwable t) {}
            append(choiceStatus);

            txtCustomStatus = new TextField("Custom Status:", "Hey there! I am using WhatsApp.", 140, TextField.ANY);
            append(txtCustomStatus);

            lblConnectedPhone = new StringItem("Account:", "Loading...");
            append(lblConnectedPhone);

            // 2. Server & Cloud Link Section
            append(new StringItem(null, "--- [ NETWORK ] ---"));
            txtServerUrl = new TextField("Server URL:", midlet.getServerUrl(), 140, TextField.ANY);
            append(txtServerUrl);

            // 3. About Section
            append(new StringItem(null, "--- [ ABOUT ] ---"));
            lblAbout = new StringItem("WhatsApp S40", 
                "Version: 2.16.5\n" +
                "Architecture: CLDC 1.1 / MIDP 2.0\n" +
                "Multi-Device Cloud Link: Active\n" +
                "(c) 2011-2026 WhatsApp Inc.");
            append(lblAbout);

            cmdSave = new Command("Save", Command.OK, 1);
            cmdTestConn = new Command("Test Conn", Command.ITEM, 2);
            cmdBack = new Command("Back", Command.BACK, 3);

            addCommand(cmdSave);
            addCommand(cmdTestConn);
            addCommand(cmdBack);

            setCommandListener(this);
            setItemStateListener(this);

            fetchProfile();
        } catch (Throwable t) {
            // Guarantee constructor never throws uncaught exception
        }
    }

    private void fetchProfile() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = midlet.getServerUrl() + "/api/profile";
                    String res = HttpHelper.get(url);
                    final Hashtable obj = (Hashtable) MiniJson.parse(res);
                    if (obj != null) {
                        midlet.getDisplay().callSerially(new Runnable() {
                            public void run() {
                                String name = MiniJson.getString(obj, "name", "");
                                String phone = MiniJson.getString(obj, "phone", "");
                                String status = MiniJson.getString(obj, "statusText", "Available");

                                if (name.length() > 0) txtProfileName.setString(name);
                                if (phone.length() > 0) lblConnectedPhone.setText("Linked: " + phone);
                                else lblConnectedPhone.setText("Not linked yet (Use Link Device)");

                                txtCustomStatus.setString(status);

                                // Select matching preset if exists
                                for (int i = 0; i < STATUS_PRESETS.length - 1; i++) {
                                    if (STATUS_PRESETS[i].equalsIgnoreCase(status)) {
                                        choiceStatus.setSelectedIndex(i, true);
                                        return;
                                    }
                                }
                                choiceStatus.setSelectedIndex(STATUS_PRESETS.length - 1, true);
                            }
                        });
                    }
                } catch (Exception e) {
                    midlet.getDisplay().callSerially(new Runnable() {
                        public void run() {
                            lblConnectedPhone.setText("Connection offline");
                        }
                    });
                }
            }
        }).start();
    }

    public void itemStateChanged(Item item) {
        if (item == choiceStatus) {
            int idx = choiceStatus.getSelectedIndex();
            if (idx >= 0 && idx < STATUS_PRESETS.length - 1) {
                txtCustomStatus.setString(STATUS_PRESETS[idx]);
            }
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdSave) {
            String url = txtServerUrl.getString().trim();
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            midlet.saveServerUrl(url);

            final String newName = txtProfileName.getString().trim();
            final String newStatus = txtCustomStatus.getString().trim();

            new Thread(new Runnable() {
                public void run() {
                    try {
                        String sUrl = midlet.getServerUrl() + "/api/profile/status";
                        String sJson = "{\"statusText\":\"" + newStatus + "\"}";
                        HttpHelper.post(sUrl, sJson);

                        String nUrl = midlet.getServerUrl() + "/api/profile/name";
                        String nJson = "{\"name\":\"" + newName + "\"}";
                        HttpHelper.post(nUrl, nJson);
                    } catch (Exception e) {}
                }
            }).start();

            midlet.showAlert("Saved", "WhatsApp settings saved successfully!", AlertType.CONFIRMATION);
            midlet.showChatList();

        } else if (c == cmdTestConn) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        String url = txtServerUrl.getString().trim() + "/api/status";
                        String res = HttpHelper.get(url);
                        Hashtable obj = (Hashtable) MiniJson.parse(res);
                        boolean connected = MiniJson.getBoolean(obj, "connected", false);
                        if (connected) {
                            midlet.showAlert("Connected!", "Server connected & WhatsApp account is linked!", AlertType.CONFIRMATION);
                        } else {
                            midlet.showAlert("Server OK", "Server is reachable, but WhatsApp is not linked. Use 'Link Device'.", AlertType.INFO);
                        }
                    } catch (final Exception e) {
                        midlet.showAlert("Connection Failed", e.getMessage(), AlertType.ERROR);
                    }
                }
            }).start();

        } else if (c == cmdBack) {
            midlet.showChatList();
        }
    }
}
