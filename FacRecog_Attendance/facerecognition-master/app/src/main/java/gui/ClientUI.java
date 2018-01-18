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

import controll.CameraController;
import controll.MenuBarController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class ClientUI {

    private final int imageWidth;
    private final int imageHeight;
    private final CameraController cameraController;
    private JFrame camCaptureControlFrame;

    private JButton buttonStartCapture;
    private JButton buttonStopCapture;

    private ImageIcon servedImageIcon;
    private JLabel servedTextLabel;
    private String currentDetectedPerson = "";


    /**
     * Sets up a window in which images received from a Face Recognition service
     * is displayed, along with a set of buttons to turn the camera capturing feed
     * on and off.
     */
    public ClientUI(int imageWidth, int imageHeight, CameraController cameraController, final MenuBarController menuBarController) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.cameraController = cameraController;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createMainFrame();
                createServedImagePane();
                createCameraControlButtons(camCaptureControlFrame.getContentPane());
                camCaptureControlFrame.setJMenuBar(new ClientMenuBar(menuBarController));
                camCaptureControlFrame.pack();
                camCaptureControlFrame.setVisible(true);
            }
        });

    }

    /**
     * Create JFrame which will contain the results from Face recognition, as well
     * as camera control buttons.
     */
    private void createMainFrame() {
        camCaptureControlFrame = new JFrame("Face recognition");
        camCaptureControlFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        camCaptureControlFrame.setLayout(new BorderLayout(10, 5));


    }

    /**
     * Pane showing images received from Face Recognition service.
     */
    private void createServedImagePane() {
        JPanel servedImagePanel = new JPanel();
        BufferedImage bufferedImage = new BufferedImage(imageWidth, imageHeight, 3);
        servedImageIcon = new ImageIcon(bufferedImage);
        JLabel servedImageLabel = new JLabel(servedImageIcon);
        servedImagePanel.add(servedImageLabel);

        camCaptureControlFrame.add(servedImagePanel, BorderLayout.CENTER);
        servedTextLabel = new JLabel("Identified person: ");
        camCaptureControlFrame.add(servedTextLabel, BorderLayout.BEFORE_FIRST_LINE);
    }

    /**
     * Create buttons used to start/stop camera capturing.
     * @param contentPane pane to attach buttons to.
     */
    private void createCameraControlButtons(Container contentPane) {
        final JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(1, 2));
        buttonStartCapture = new JButton("Start camera");
        buttonStopCapture = new JButton("Stop camera");
        buttonStopCapture.setEnabled(false);

        buttonStartCapture.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraController.startCameraCapture();
            }
        });
        buttonStopCapture.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cameraController.stopCameraCapture();
            }
        });

        panel.add(buttonStartCapture);
        panel.add(buttonStopCapture);
        contentPane.add(panel, BorderLayout.PAGE_END);
    }


    /**
     * Updates the pane which shows images received from a recognition service.
     * @param bufferedImage the new image to display.
     * @param identifiedPerson the name of the person identified in the image.
     */
    public void updateServedImage(final BufferedImage bufferedImage, final String identifiedPerson) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(!identifiedPerson.equals(currentDetectedPerson)) {
                    currentDetectedPerson = identifiedPerson;
                    servedTextLabel.setText("Identified person: " + identifiedPerson);
                }
                servedImageIcon.setImage(bufferedImage);
                camCaptureControlFrame.repaint();
            }
        });
    }


    public void toggleCameraOn() {
        buttonStartCapture.setEnabled(false);
        buttonStopCapture.setEnabled(true);
    }

    public void toggleCameraOff() {
        buttonStartCapture.setEnabled(true);
        buttonStopCapture.setEnabled(false);
    }
}
