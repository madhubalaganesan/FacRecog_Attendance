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

package opencv;

import com.esotericsoftware.minlog.Log;
import org.bytedeco.javacv.*;
import static org.bytedeco.javacpp.opencv_core.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.concurrent.LinkedBlockingQueue;


public class CameraCapture extends Observable {

    private final LinkedBlockingQueue<BufferedImage> capturedImageQueue;
    private CanvasFrame captureCanvasFrame;
    private FrameGrabber cameraFrameGrabber;

    private double captureIntervalInSeconds = 0.5;
    private final CvMemStorage storage;
    private final Java2DFrameConverter frame2bufferedImageConverter;
    private WindowRenderer windowRenderer;

    public CameraCapture(final int prefWidth, final int prefHeight, LinkedBlockingQueue<BufferedImage> capturedImageQueue) {

        this.capturedImageQueue = capturedImageQueue;

        try {
            cameraFrameGrabber = FrameGrabber.createDefault(0);
        } catch (FrameGrabber.Exception e) {
            cameraFrameGrabber = new OpenCVFrameGrabber(0);
        }

        cameraFrameGrabber.setImageHeight(prefHeight);
        cameraFrameGrabber.setImageWidth(prefWidth);
        cameraFrameGrabber.setImageMode(FrameGrabber.ImageMode.GRAY);

        frame2bufferedImageConverter = new Java2DFrameConverter();

        storage = CvMemStorage.create();

        createCaptureDisplay(prefHeight, prefWidth);
    }

    /**
     * Sets up a CanvasFrame onto which the camera feed will be drawn.
     * @param prefHeight preferred window height.
     * @param prefWidth preferred window width.
     */
    public void createCaptureDisplay(int prefHeight, int prefWidth) {
        captureCanvasFrame = new CanvasFrame("Face recognition service", CanvasFrame.getDefaultGamma() / cameraFrameGrabber.getGamma());
        captureCanvasFrame.setVisible(false);
        captureCanvasFrame.setCanvasSize(prefWidth, prefHeight);
        captureCanvasFrame.setLocation(prefWidth / 4 + 30, 0);
        captureCanvasFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    }

    /**
     * Modify how much time is waited in between taking snapshots of the
     * camera feed.
     * @param captureIntervalInSeconds the new waiting period in seconds.
     */
    public void setCaptureIntervalInSeconds(double captureIntervalInSeconds) {
        this.captureIntervalInSeconds = captureIntervalInSeconds;
    }

    /**
     * Invokes a SwingWorker to initiate the camera feed.
     */
    public void startCapture() {
        windowRenderer = new WindowRenderer();
        windowRenderer.execute();
    }

    /**
     * Stop the camera feed.
     */
    public void stopCapture() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                captureCanvasFrame.setVisible(false);
                windowRenderer.cancel(true);
            }
        };
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }


    }


    /**
     * Start the camera capture and render the camera feed onto
     * the CanvasFrame. A snapshot is captured from the camera feed
     * every X-seconds, where X is specified by the user and
     * defaults to 0.5seconds. The snapshot is sent to a face recognition
     * service.
     * @throws FrameGrabber.Exception
     */
    private void startCameraCapture() throws FrameGrabber.Exception, InterruptedException {
        cameraFrameGrabber.start();
        org.bytedeco.javacv.Frame grabbedFrame;
        long timeSinceSnapshot = System.currentTimeMillis();
        long fpsTime = System.currentTimeMillis();
        int nGrabbedFrames = 0;
        long prevTime = System.currentTimeMillis();

        while (captureCanvasFrame.isVisible() && (grabbedFrame = cameraFrameGrabber.grab()) != null) {
            IplImage iplImage = ((IplImage) grabbedFrame.opaque);
            cvFlip(iplImage, iplImage, 1);

            BufferedImage bfimg = frame2bufferedImageConverter.convert(grabbedFrame);
            captureCanvasFrame.showImage(bfimg);

            cvClearMemStorage(storage);

            nGrabbedFrames++;
            if (System.currentTimeMillis() - fpsTime > 1000) {
                Log.info("Capture frame rate: " + nGrabbedFrames);
                nGrabbedFrames = 0;
                fpsTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - timeSinceSnapshot > ((long) (captureIntervalInSeconds * 1000))) {
                Log.info("Captured image size: <" + grabbedFrame.imageWidth + "x" + grabbedFrame.imageHeight + ">, Depth: " + grabbedFrame.imageDepth + ", Channels: " + grabbedFrame.imageChannels + " prev. capture(ms): " + (System.currentTimeMillis() - prevTime));
                capturedImageQueue.put(bfimg);
                timeSinceSnapshot = System.currentTimeMillis();
                prevTime = timeSinceSnapshot;
            }
        }
        Log.info("Camera frame grabber stopped");
        cameraFrameGrabber.stop();
        setChanged();
        notifyObservers("STOPPED");
    }

    /**
     * A SwingWorker which captures the camera feed and
     * renders the result to a CanvasFrame.
     */
    private class WindowRenderer extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            captureCanvasFrame.setVisible(true);

            try {
                startCameraCapture();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

    }
}
