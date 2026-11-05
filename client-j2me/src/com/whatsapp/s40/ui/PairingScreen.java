package com.whatsapp.s40.ui;

import com.whatsapp.s40.WhatsAppMIDlet;
import com.whatsapp.s40.net.HttpHelper;
import com.whatsapp.s40.util.MiniJson;
import java.util.Hashtable;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

public class PairingScreen extends Form implements CommandListener {

    private WhatsAppMIDlet midlet;
    private TextField txtPhone;
    private StringItem lblCode;
    private StringItem lblInstructions;
    private Command cmdGetCode;
    private Command cmdCheckStatus;
    private Command cmdBack;

    public PairingScreen(WhatsAppMIDlet midlet) {
        super("Link WhatsApp");
        this.midlet = midlet;

        append(new StringItem("Step 1:", "Enter your Android / iPhone WhatsApp number with country code:"));

        txtPhone = new TextField("Phone Number:", "91", 20, TextField.PHONENUMBER);
        append(txtPhone);

        lblCode = new StringItem("Link Code:", "(Press 'Get Code')");
        append(lblCode);

        lblInstructions = new StringItem("Instructions:", 
            "1. Open WhatsApp on Android/iPhone\n" +
            "2. Go to Linked Devices\n" +
            "3. Tap 'Link with phone number'\n" +
            "4. Enter the 8-digit code shown above.");
        append(lblInstructions);

        cmdGetCode = new Command("Get Code", Command.ITEM, 1);
        cmdCheckStatus = new Command("Check Link", Command.OK, 2);
        cmdBack = new Command("Back", Command.BACK, 3);

        addCommand(cmdGetCode);
        addCommand(cmdCheckStatus);
        addCommand(cmdBack);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdGetCode) {
            String serverUrl = midlet.getServerUrl();
            if (serverUrl.indexOf("localhost") != -1 || serverUrl.indexOf("127.0.0.1") != -1) {
                lblCode.setText("Set Server URL in Settings!");
                midlet.showAlert("URL Error", "Server is set to localhost! Nokia C2-01 needs your public Cloud or Tunnel URL. Go to Options -> Settings.", AlertType.WARNING);
                return;
            }

            final String phone = txtPhone.getString().trim();
            if (phone.length() < 7) {
                midlet.showAlert("Error", "Please enter a valid phone number with country code.", AlertType.WARNING);
                return;
            }

            lblCode.setText("Requesting code...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        String url = midlet.getServerUrl() + "/api/pair";
                        String jsonBody = "{\"phone\":\"" + phone + "\"}";
                        String res = HttpHelper.post(url, jsonBody);
                        
                        Hashtable obj = (Hashtable) MiniJson.parse(res);
                        boolean success = MiniJson.getBoolean(obj, "success", false);
                        if (success) {
                            final String code = MiniJson.getString(obj, "pairingCode", "");
                            midlet.getDisplay().callSerially(new Runnable() {
                                public void run() {
                                    lblCode.setText(">>> " + code + " <<<");
                                }
                            });
                        } else {
                            final String err = MiniJson.getString(obj, "error", "Failed");
                            midlet.getDisplay().callSerially(new Runnable() {
                                public void run() {
                                    lblCode.setText("Error: " + err);
                                }
                            });
                        }
                    } catch (final Exception e) {
                        midlet.getDisplay().callSerially(new Runnable() {
                            public void run() {
                                lblCode.setText("Fail: " + e.getMessage());
                            }
                        });
                    }
                }
            }).start();

        } else if (c == cmdCheckStatus) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        String url = midlet.getServerUrl() + "/api/status";
                        String res = HttpHelper.get(url);
                        Hashtable obj = (Hashtable) MiniJson.parse(res);
                        boolean connected = MiniJson.getBoolean(obj, "connected", false);

                        if (connected) {
                            midlet.showAlert("Success", "Device linked successfully!", AlertType.CONFIRMATION);
                            midlet.showChatList();
                        } else {
                            midlet.showAlert("Not Linked", "Not linked yet. Enter code on your phone.", AlertType.INFO);
                        }
                    } catch (Exception e) {
                        midlet.showAlert("Network Error", e.getMessage(), AlertType.ERROR);
                    }
                }
            }).start();

        } else if (c == cmdBack) {
            midlet.showChatList();
        }
    }
}
