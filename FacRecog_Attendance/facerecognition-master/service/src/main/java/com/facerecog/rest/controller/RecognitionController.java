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

package com.facerecog.rest.controller;

import api.ApiUrls;
import dto.RecognitionDTO;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.facerecog.rest.service.RecognitionService;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(value = ApiUrls.ROOT_URL_RECOG)
public class RecognitionController {

    @Autowired
    RecognitionService recognitionService;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(RecognitionController.class);


    /**
     * Handle requests to /recog/detectIdentify - uses face recognition algorithm to
     * identify the person in an uploaded image file.
     *
     * @return DTO containing predicted name of person and down-scaled image with rect around face
     */
    @RequestMapping(value = ApiUrls.URL_RECOG_DETECT_IDENTIFY, method = RequestMethod.POST)
    public
    Callable<RecognitionDTO>
    identifyAndDetectAsync(final HttpEntity<byte[]> requestEntity,
                        @RequestHeader(value = "imageType") int imageType,
                        @RequestHeader(value = "imageWidth") int imageWidth,
                        @RequestHeader(value = "imageHeight") int imageHeight)
            throws ExecutionException, InterruptedException {
        logger.info("Detection & identification (async). Image type: " + imageType + ", width: " + imageWidth+ ", height: " + imageHeight);

        return recognitionService.detectedAndIdentifyAsync(requestEntity.getBody(), imageType, imageWidth, imageHeight);
    }

    /**
     * Handle requests to /recog/detect - drawing a box around every detected face
     * in the image.
     * @return DTO containing the modified picture
     */
    @RequestMapping(value = ApiUrls.URL_RECOG_DETECT, method = RequestMethod.POST)
    public
    RecognitionDTO
    detect(final HttpEntity<byte[]> requestEntity,
                       @RequestHeader(value = "imageType") int imageType,
                       @RequestHeader(value = "imageWidth") int imageWidth,
                       @RequestHeader(value = "imageHeight") int imageHeight)
            throws ExecutionException, InterruptedException {
        logger.info("Detection only. Image type: " + imageType + ", width: " + imageWidth+ ", height: " + imageHeight);

        return recognitionService.detect(requestEntity.getBody(), imageType, imageWidth, imageHeight);
    }


    @RequestMapping(value = ApiUrls.URL_RECOG_UPLOAD_IMAGE, method = RequestMethod.POST)
    public
    String
    handleFileUpload(@RequestParam("file") MultipartFile file) {
        logger.info("File upload");

        if (!file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                BufferedOutputStream stream =
                        new BufferedOutputStream(new FileOutputStream(new File(file.getOriginalFilename())));
                stream.write(bytes);
                stream.close();
                return "File uploaded: " + file.getOriginalFilename();
            } catch (Exception e) {
                return "Failed to upload image!";
            }
        } else {
            return "Failed to upload file because the file was empty.";
        }
    }



}