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
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_contrib.*;
import org.bytedeco.javacpp.opencv_core.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.IntBuffer;
import java.util.AbstractMap;

import static org.bytedeco.javacpp.opencv_contrib.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.*;

public class FaceRecogniser {

    private FaceRecognizer faceRecognizer;
    private String trainResultsStoragePath;
    private File absoluteTrainingSetPath;

    public FaceRecogniser(String trainingSetDirRelative, String trainResultsStoragePath) throws FileNotFoundException {
        this.trainResultsStoragePath = trainResultsStoragePath;

        String rootPath = Util.getProjectRootDir(FaceRecogniser.class);
        Log.info("Loading training-images '" + trainingSetDirRelative + "' @ " + rootPath + "\n" + rootPath + trainingSetDirRelative);
        absoluteTrainingSetPath = new File(rootPath + trainingSetDirRelative);

        setLBPHAlgorithm();
    }

    public void setLBPHAlgorithm() {
        faceRecognizer = createLBPHFaceRecognizer();
        train(absoluteTrainingSetPath.getAbsolutePath());
    }

    public void setEigenAlgorithm() {
        faceRecognizer = createEigenFaceRecognizer();
        train(absoluteTrainingSetPath.getAbsolutePath());
    }

    public void setFisherAlgorithm() {
        faceRecognizer = createFisherFaceRecognizer();
        train(absoluteTrainingSetPath.getAbsolutePath());
    }


    private void train(String trainingImagesDir) {
        File[] trainingImages = Util.findImagesInDirectory(trainingImagesDir);
        MatVector grayscaledTrainingImages = Util.loadImages(trainingImages, CV_LOAD_IMAGE_GRAYSCALE);
        AbstractMap.SimpleEntry<Mat, IntStringMap> labelNameMap = createTrainingLabels(trainingImages);

        Log.info("Training recognizer");
        faceRecognizer.setLabelsInfo(labelNameMap.getValue());
        faceRecognizer.train(grayscaledTrainingImages, labelNameMap.getKey());

        Log.info("Training done.");
    }

    public String predictPerson(Mat imgMat) {
        int prediction = faceRecognizer.predict(imgMat);
        BytePointer bp = faceRecognizer.getLabelInfo(prediction);
        return bp.getString();
    }


    private AbstractMap.SimpleEntry<Mat, IntStringMap> createTrainingLabels(File[] imageFiles) {
        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();

        IntStringMap intStringMap = new IntStringMap();

        //Example: a file called 2-Gustav_3 is split into -> id = 2[0], name = Gustav[1], entry = 3[2]
        for (int i = 0; i < imageFiles.length; i++) {
            String[] imgNameParts = imageFiles[i].getName().split("\\-|_|\\.");

            String personId = imgNameParts[0];
            String personName = imgNameParts[1];

            int label = Integer.parseInt(personId);

            BytePointer namePointer = new BytePointer(personName);

            intStringMap.put(label, namePointer);
            labelsBuf.put(i, label);

        }
        Log.info("Training images loaded.");

        return new AbstractMap.SimpleEntry<Mat, IntStringMap>(labels, intStringMap);
    }

    public void save() {
        FileStorage fileStorage = new FileStorage(trainResultsStoragePath, FileStorage.WRITE);
        faceRecognizer.save(fileStorage);
    }

    public void load() {
        FileStorage fileStorage = new FileStorage(trainResultsStoragePath, FileStorage.READ);
        faceRecognizer.load(fileStorage);
    }

}


