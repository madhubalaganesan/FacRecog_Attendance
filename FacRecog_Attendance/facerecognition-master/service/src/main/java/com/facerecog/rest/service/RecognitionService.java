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

package com.facerecog.rest.service;

import dto.RecognitionDTO;
import opencv.FaceDetector;
import opencv.FaceRecogniser;
import opencv.Util;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

@Service
public class RecognitionService {

    private FaceDetector detector;
    private FaceRecogniser recogniser;
    private Logger logger = LoggerFactory.getLogger(RecognitionService.class);

    public RecognitionService() throws FileNotFoundException, URISyntaxException {

        String sep = "/";
        if (System.getProperty("os.name").startsWith("Windows")) {
            sep = "\\";
            logger.info("Detected windows system, using file-separator: '" + sep + "'");
        }

        String trainingSetDirRelative = sep + "resources" + sep + "main" + sep + "recognition" + sep + "training";
        String trainResultsStoragePath = sep + "Users" + sep + "username" + sep + "facerecog" + sep + "storage";

        recogniser = new FaceRecogniser(trainingSetDirRelative, trainResultsStoragePath);

        String cascadeResourcePath = sep + "resources" + sep + "main" + sep + "detection" + sep + "haar" + sep + "frontalface_alt.xml";
        detector = new FaceDetector(cascadeResourcePath);

    }


    public Callable<RecognitionDTO> detectedAndIdentifyAsync(final byte[] byteImage, final int imageType, final int imageWidth, final int imageHeight) {
        return new Callable<RecognitionDTO>() {
            @Override
            public RecognitionDTO call() throws Exception {
                return detectAndIdentify(byteImage, imageType, imageWidth, imageHeight);
            }
        };
    }

    public RecognitionDTO detect(byte[] byteImage, int type, int width, int height) {
        long t1 = System.currentTimeMillis();
        Mat imageMat = convertBytesToImage(byteImage, type, width, height);

        Mat imageMatResized = new Mat(imageMat.rows() / 4, imageMat.cols() / 4, imageMat.type());
        cvResize(imageMat.asCvMat(), imageMatResized.asCvMat(), CV_INTER_AREA);
        detector.detectFaces(imageMatResized);
        RecognitionDTO response = createIdentificationResponse("", imageMatResized);
        logger.info("Request completed after: " + (System.currentTimeMillis() - t1) + "ms");

        return response;

    }
    public RecognitionDTO detectAndIdentify(byte[] byteImage, int type, int width, int height) {
        long t1 = System.currentTimeMillis();
        Mat imageMat = convertBytesToImage(byteImage, type, width, height);

        Mat imageMatResized = new Mat(imageMat.rows() / 4, imageMat.cols() / 4, imageMat.type());
        cvResize(imageMat.asCvMat(), imageMatResized.asCvMat(), CV_INTER_AREA);

        //cvEqualizeHist(imageMatResized.asCvMat(), imageMatResized.asCvMat());

        String predictedPerson = recogniser.predictPerson(imageMatResized);
        detector.detectFaces(imageMatResized);

        RecognitionDTO response = createIdentificationResponse(predictedPerson, imageMatResized);

        logger.info("Request completed after: " + (System.currentTimeMillis() - t1) + "ms (" + predictedPerson + ")");

        return response;
    }

    private Mat convertBytesToImage(byte[] byteImage, int type, int width, int height) {
        int matType = -1;

        switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR:
                matType = CV_8UC3;
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
                matType = CV_8UC1;
                break;
            default:
                throw new IllegalArgumentException("Unrecognized type");
        }
        Mat imageMat = new Mat(height, width, matType);
        imageMat.ptr().put(byteImage);

        if (matType != CV_8UC1) {
            Mat matGray = new Mat();
            cvtColor(imageMat, matGray, CV_RGB2GRAY);
            imageMat = matGray;
        }
        return imageMat;
    }

    private static RecognitionDTO createIdentificationResponse(String predictedPerson, Mat mat) {
        RecognitionDTO recognitionDTO = new RecognitionDTO();
        recognitionDTO.setPredictedPerson(predictedPerson);
        recognitionDTO.setBytes(Util.matToBytes(mat));
        recognitionDTO.setCols(mat.cols());
        recognitionDTO.setRows(mat.rows());
        recognitionDTO.setType(mat.type());
        return recognitionDTO;
    }

}




