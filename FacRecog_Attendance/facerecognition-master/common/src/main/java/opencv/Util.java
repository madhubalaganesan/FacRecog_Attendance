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
import dto.RecognitionDTO;
import org.bytedeco.javacpp.BytePointer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.security.CodeSource;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class Util {

    private static final FilenameFilter imgFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            name = name.toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
        }
    };

    public static File[] findImagesInDirectory(String imageDirectory) {
        File imageFileDir = new File(imageDirectory);

        File[] imageFiles = imageFileDir.listFiles(imgFilter);
        Log.info("Found " + imageFiles.length + " images in " + imageDirectory);

        return imageFiles;
    }

    public static IplImage cvArrToGray(CvArr matColored) {
        IplImage grayIplImage = IplImage.create(matColored.arrayWidth(), matColored.arrayHeight(), IPL_DEPTH_8U, 1);
        cvCvtColor(matColored, grayIplImage, CV_RGB2GRAY);
        return grayIplImage;
    }

    public static IplImage iplImage2gray(IplImage image) {
        if (image.nChannels() == 1) {
            System.err.println("n channels 1 rturn original");
            return image;
        }
        IplImage grayIplImage = IplImage.create(image.width(), image.height(), IPL_DEPTH_8U, 1);
        if (image.nChannels() == 3) {
            cvCvtColor(image, grayIplImage, CV_RGB2GRAY);
        } else if (image.nChannels() == 4) {
            cvCvtColor(image, grayIplImage, CV_RGB2GRAY);
        }
        return grayIplImage;
    }

    /**
     * Resize a BufferedImage to specified width and height.
     * @param image the image to resize.
     * @param width the new image width.
     * @param height the new image height.
     * @return resized image.
     */
    public static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TRANSLUCENT);
        Graphics2D g2d = (Graphics2D) bi.createGraphics();
        g2d.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
        return bi;
    }

    public static MatVector loadImages(File[] imageFiles, int CV_LOAD_MODE) {
        MatVector imgMatVector = new MatVector(imageFiles.length);
        for (int i = 0; i < imageFiles.length; i++) {
            Mat img = imread(imageFiles[i].getAbsolutePath(), CV_LOAD_MODE);
            cvFlip(img.asCvMat(), img.asCvMat(), 1);
            Mat matGraySmall = new Mat(img.rows() / 4, img.cols() / 4, CV_8UC1);
            cvResize(img.asCvMat(), matGraySmall.asCvMat(), CV_INTER_AREA);
            imgMatVector.put(i, matGraySmall);
            //imgMatVector.put(i, img);
        }
        return imgMatVector;
    }

    public static Mat loadImage(File imageFile, int CV_LOAD_MODE) {
        return imread(imageFile.getAbsolutePath(), CV_LOAD_MODE);
    }

    public static byte[] image2Bytes(BufferedImage image) {
        return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    }

    public static byte[] matToBytes(Mat mat) {
        int cols = mat.cols();
        int rows = mat.rows();
        int elemSize = (int) mat.elemSize();
        byte[] data = new byte[cols * rows * elemSize];
        BytePointer dataPtr = mat.ptr();
        dataPtr.get(data);
        return data;
    }

    /**
     * Converts/writes a Mat into a BufferedImage.
     *
     * @param mat Mat of type CV_8UC3 or CV_8UC1
     * @return BufferedImage of type TYPE_INT_RGB or TYPE_BYTE_GRAY
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        int cols = mat.cols();
        int rows = mat.rows();
        int elemSize = (int) mat.elemSize();
        byte[] data = new byte[cols * rows * elemSize];
        BytePointer dataPtr = mat.ptr();
        dataPtr.get(data);
        int type;

        switch (mat.channels()) {
            case 1:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;
                // bgr to rgb
                byte b;
                for (int i = 0; i < data.length; i = i + 3) {
                    b = data[i];
                    data[i] = data[i + 2];
                    data[i + 2] = b;
                }
                break;

            default:
                return null;
        }


        BufferedImage image = new BufferedImage(cols, rows, type);
        image.getRaster().setDataElements(0, 0, cols, rows, data);

        return image;
    }

    public static void displayImage(Image image) {
        ImageIcon icon = new ImageIcon(image);
        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(image.getWidth(null) + 25, image.getHeight(null) + 50);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public static String getProjectRootDir(Class c) throws FileNotFoundException {
        CodeSource codeSource = c.getProtectionDomain().getCodeSource();
        File file = new File(codeSource.getLocation().getPath());
        //Log.info("Locating resource folder");
        //Log.info("Full: " + file.toString());

        if(file.toString().contains("!")) {
            String jarFolder = file.toString();
            jarFolder = jarFolder.substring(0, jarFolder.indexOf("!"));
            jarFolder = jarFolder.substring(0,jarFolder.lastIndexOf("/"));
            //Log.info("jar-folder: " + jarFolder);
            file = new File(jarFolder);
            //Log.info("Parent: " + file.getParentFile().getPath());

            file = new File(file.getParentFile().getPath().replaceAll("file:", ""));
        } else {
            //Log.info("Parent: " + file.getParentFile().toString());
            //Log.info("Parent-parent: " + file.getParentFile().getParentFile().toString());
            file = new File(file.getParentFile().getParentFile().getAbsolutePath().replaceAll("file:",""));
        }
        boolean foundResources = false;
        for(String fileName : file.list()) {
            if(fileName.equals("resources")) {
                foundResources = true;
            }
        }
        if(!foundResources) {
            throw new FileNotFoundException("Resource folder not found!");
        }
        return file.getAbsolutePath();
    }

    public static  BufferedImage identificationDtoToBufferedImage(RecognitionDTO entityDTO) {
        return matBytesToBufferedImage(entityDTO.getBytes(), entityDTO.getCols(), entityDTO.getRows(), entityDTO.getType());
    }

    public static BufferedImage matBytesToBufferedImage(byte[] data, int cols, int rows, int type) {
        int bufferedType = 10;

        switch (type) {
            case 0:
                bufferedType = BufferedImage.TYPE_BYTE_GRAY;
                break;
            case 16:
                bufferedType = BufferedImage.TYPE_3BYTE_BGR;
                break;
        }

        BufferedImage image = new BufferedImage(cols, rows, bufferedType);
        image.getRaster().setDataElements(0, 0, cols, rows, data);
        return image;
    }
}
