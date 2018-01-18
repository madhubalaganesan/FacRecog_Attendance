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

package client;


import api.ApiUrls;
import com.esotericsoftware.minlog.Log;

import controll.CameraController;
import controll.MenuBarController;
import controll.ServiceController;
import dto.RecognitionDTO;
import gui.ClientUI;
import opencv.CameraCapture;
import opencv.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


public class Client implements Observer, ServiceController, CameraController, MenuBarController {

    private static String SERVICE_TYPE = ApiUrls.ROOT_URL_RECOG + ApiUrls.URL_RECOG_DETECT_IDENTIFY;
    private static String SERVICE_URL = "http://localhost:8080";
    private static String SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
    private static double CAMERA_CAPTURE_INTERVAL_IN_SEC = 0.2;
    private static boolean usingGUI = true;
    private static File outputDir;


    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        if (Arrays.asList(args).contains("--nogui")) {
            usingGUI = false;
        }

        new Client();

        if (!usingGUI) {
            BufferedImage img = null;
            for (int i = 0; i < args.length; i++) {
                if (args[i].contains("--path")) {
                    if (i + 1 < args.length) {
                        try {
                            img = ImageIO.read(new File(args[i + 1]));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        i++;
                    }
                } else if (args[i].contains("--url")) {
                    if (i + 1 < args.length) {
                        if (args[i + 1].length() > 1) {
                            SERVICE_URL = args[i + 1];
                            SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
                            serviceRequester.setServiceUrl(SERVICE_REQUEST_URL);
                        }
                        i++;
                    }
                } else if (args[i].contains("--type")) {
                    if (i + 1 < args.length) {
                        SERVICE_TYPE = args[i + 1];
                        SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
                        serviceRequester.setServiceUrl(SERVICE_REQUEST_URL);
                        i++;
                    }
                } else if (args[i].contains("--outdir")) {
                    if (i + 1 < args.length) {
                        outputDir = new File(args[i + 1]);
                        i++;
                    }
                }
            }
            if (img != null) {
                serviceRequester.executeRequest(img);
            } else {
                System.err.println("No path to image given.");
            }
        }

    }

    private ClientUI clientUI;
    private static ServiceRequester serviceRequester;
    private CameraCapture cameraCapture;
    private volatile boolean activeCameraCapture = false;

    private final ExecutorService executorService;

    private int captureWidth = 1080;
    private int captureHeight = 720;


    public Client() {

        LinkedBlockingQueue<BufferedImage> capturedImageQueue = new LinkedBlockingQueue<BufferedImage>();
        executorService = Executors.newSingleThreadExecutor();

        serviceRequester = new ServiceRequester(capturedImageQueue, SERVICE_REQUEST_URL, this);

        if (usingGUI) {
            cameraCapture = new CameraCapture(captureWidth, captureHeight, capturedImageQueue);
            cameraCapture.addObserver(this);
            cameraCapture.setCaptureIntervalInSeconds(CAMERA_CAPTURE_INTERVAL_IN_SEC);

            clientUI = new ClientUI(captureWidth / 4, captureHeight / 4, this, this);
        }

    }


    @Override
    public void update(Observable observable, Object arg) {
        if (observable instanceof CameraCapture) {
            String msg = (String) arg;
            if (msg.equals("STOPPED")) {
                clientUI.toggleCameraOff();
            }
        }

    }

    //---------------- ServiceController interface -----------------
    @Override
    public void receivedRecognitionDto(final RecognitionDTO recognitionResponse) {
        if (usingGUI) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    BufferedImage bufferedImage = Util.identificationDtoToBufferedImage(recognitionResponse);
                    clientUI.updateServedImage(bufferedImage, recognitionResponse.getPredictedPerson());
                }
            });
        } else {
            try {
                BufferedImage bufferedImage = Util.identificationDtoToBufferedImage(recognitionResponse);

                String buildPath = Util.getProjectRootDir(Client.class);
                String resultImagePath;
                if(outputDir != null) {
                    resultImagePath = outputDir.getAbsolutePath() + File.separator + recognitionResponse.getPredictedPerson() + "-" + System.currentTimeMillis() + ".jpg";
                } else {
                    resultImagePath = buildPath + File.separator + recognitionResponse.getPredictedPerson() + "-" + System.currentTimeMillis() + ".jpg";
                }

                System.err.println("Writing received image to: " + resultImagePath);
                ImageIO.write(bufferedImage, "jpg", new File(resultImagePath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //---------------- CameraController interface ------------------

    @Override
    public void startCameraCapture() {
        activeCameraCapture = true;
        Log.info("Starting camera capture");
        clientUI.toggleCameraOn();
        new Thread(serviceRequester).start();

        try {
            cameraCapture.startCapture();
        } catch (Exception e1) {
            e1.printStackTrace();
            clientUI.toggleCameraOff();
        }
    }

    @Override
    public void stopCameraCapture() {
        if (activeCameraCapture) {
            activeCameraCapture = false;
            Log.info("Camera capture stopped");
            clientUI.toggleCameraOff();
            cameraCapture.stopCapture();
            serviceRequester.shutdown();
        }
    }


    //--------------- MenuBarController interface -------------------
    @Override
    public void close() {
        System.exit(0);
    }

    @Override
    public void setCameraCaptureInterval(double interval) {
        Log.info("Changing camera capture interval from '" + CAMERA_CAPTURE_INTERVAL_IN_SEC + "' to '" + interval + "'.");
        CAMERA_CAPTURE_INTERVAL_IN_SEC = interval;
        cameraCapture.setCaptureIntervalInSeconds(CAMERA_CAPTURE_INTERVAL_IN_SEC);
    }

    @Override
    public double getCameraCaptureInterval() {
        return CAMERA_CAPTURE_INTERVAL_IN_SEC;
    }

    @Override
    public void changeServiceUrl(String url) {
        Log.info("Changing service URL from '" + SERVICE_URL + "' to '" + url + "'.");
        SERVICE_URL = url;
        SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
        serviceRequester.setServiceUrl(SERVICE_REQUEST_URL);
    }

    @Override
    public String getServiceUrl() {
        return SERVICE_URL;
    }

    @Override
    public void setServiceType(String serviceType) {
        Log.info("Changing service type from '" + SERVICE_TYPE + "' to '" + serviceType + "'.");
        SERVICE_TYPE = serviceType;
        SERVICE_REQUEST_URL = SERVICE_URL + SERVICE_TYPE;
        serviceRequester.setServiceUrl(SERVICE_REQUEST_URL);

    }


}

