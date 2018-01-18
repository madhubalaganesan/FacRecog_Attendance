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
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_objdetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;

public class FaceDetector {

    private CvHaarClassifierCascade classifier = null;
    private CvMemStorage memStorage;
    CvFont mCvFont = new CvFont();

    public FaceDetector(String cascadeResourcePath) throws FileNotFoundException, URISyntaxException {
        Loader.load(opencv_objdetect.class);

        String rootPath = Util.getProjectRootDir(FaceDetector.class);
        Log.info("Loading feature-cascade '" + cascadeResourcePath + "' @ " + rootPath);
        File file = new File(rootPath + cascadeResourcePath);

        classifier = new CvHaarClassifierCascade(cvLoad(file.getAbsolutePath()));

        if (classifier.isNull()) {
            Log.error("Error loading classifier file " + cascadeResourcePath + " @ " + file.getAbsolutePath());
            throw new FileNotFoundException(cascadeResourcePath);
        }
        cvInitFont(mCvFont, CV_FONT_HERSHEY_SIMPLEX, 0.7, 0.7);

        memStorage = CvMemStorage.create();
    }


    public void detectFaces(Mat grayImageMat) {
        CvSeq faces = cvHaarDetectObjects(grayImageMat.asCvMat(), classifier, memStorage,
                1.1, 4, CV_HAAR_DO_CANNY_PRUNING);
        int total = faces.total();

        for (int i = 0; i < total; i++) {
            CvRect r = new CvRect(cvGetSeqElem(faces, i));
            int x = r.x(), y = r.y(), w = r.width(), h = r.height();
            cvRectangle(grayImageMat.asCvMat(), cvPoint(x, y), cvPoint(x + w, y + h), CvScalar.GRAY, 2, 4, 0);
        }

        cvClearMemStorage(memStorage);
    }

    public void drawString(String text, Mat imageMat, CvPoint orig) {
        cvPutText(imageMat.asCvMat(), text, orig, mCvFont, CvScalar.BLACK);
    }

}
