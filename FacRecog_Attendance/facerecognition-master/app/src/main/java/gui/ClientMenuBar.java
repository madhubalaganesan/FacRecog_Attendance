/*
 *
 *  * Copyright 2015 Erik Wiséen Åberg
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package gui;


import api.ApiUrls;
import controll.MenuBarController;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientMenuBar extends JMenuBar implements ActionListener {

    private final MenuBarController menuBarController;

    private JMenu fileMenu;
    private JMenuItem exitApp;

    private JMenu settingsMenu;
    private JMenuItem requestInterval;
    private JMenuItem serviceUrl;
    private JRadioButtonMenuItem radioAsync;
    private JRadioButtonMenuItem detect;

    public ClientMenuBar(MenuBarController menuBarController) {
        super();
        this.menuBarController = menuBarController;

        createFileMenu();
        createSettingsMenu();
        asd();
    }

    private void createFileMenu() {
        fileMenu = new JMenu("File");
        exitApp = new JMenuItem("Close");
        fileMenu.add(exitApp);
        add(fileMenu);
    }

    private void createSettingsMenu() {
        settingsMenu = new JMenu("Settings");

        requestInterval = new JMenuItem("Change camera capture interv.");
        settingsMenu.add(requestInterval);

        serviceUrl = new JMenuItem("Change service URL.");
        settingsMenu.add(serviceUrl);

        settingsMenu.addSeparator();

        ButtonGroup group = new ButtonGroup();

        radioAsync = new JRadioButtonMenuItem(ApiUrls.ROOT_URL_RECOG + ApiUrls.URL_RECOG_DETECT_IDENTIFY);
        radioAsync.setSelected(true);
        group.add(radioAsync);
        settingsMenu.add(radioAsync);

        detect = new JRadioButtonMenuItem(ApiUrls.ROOT_URL_RECOG + ApiUrls.URL_RECOG_DETECT);
        group.add(detect);
        settingsMenu.add(detect);

        add(settingsMenu);
    }

    private void asd() {
        exitApp.addActionListener(this);
        requestInterval.addActionListener(this);
        serviceUrl.addActionListener(this);
        radioAsync.addActionListener(this);
        detect.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if(src == exitApp) {
            menuBarController.close();
        } else if(src == requestInterval) {
            String currentRequestInterval = "" + menuBarController.getCameraCaptureInterval();
            String newInterval = JOptionPane.showInputDialog("Insert request interval (0.1-X.0 seconds):", currentRequestInterval);
            if(newInterval != null && !newInterval.equals(currentRequestInterval)) {
                try {
                    double interval = Double.parseDouble(newInterval);
                    menuBarController.setCameraCaptureInterval(interval);
                } catch (Exception ex) {
                    System.err.println("Bad request interval: non-double value received.");
                }
            }
        } else if(src == serviceUrl) {
            String currentUrl = menuBarController.getServiceUrl();
            String newUrl = JOptionPane.showInputDialog("Insert service URL:", currentUrl);
            if(newUrl != null && !newUrl.equals(currentUrl)) {
                menuBarController.changeServiceUrl(newUrl);
            }
        } else if(src == radioAsync) {
            menuBarController.setServiceType(radioAsync.getText());
        } else if(src == detect) {
            menuBarController.setServiceType(detect.getText());
        }

    }


}
