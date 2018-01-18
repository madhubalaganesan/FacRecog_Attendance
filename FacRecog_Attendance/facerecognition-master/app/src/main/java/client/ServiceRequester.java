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

import com.esotericsoftware.minlog.Log;
import controll.ServiceController;
import dto.RecognitionDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServiceRequester implements Runnable {

    private LinkedBlockingQueue<BufferedImage> imageQueue;
    private String serviceUrl;
    private final ServiceController serviceController;
    private volatile boolean running;
    private RestTemplate restTemplate;
    private Thread runningThread;
    private static int QUEUE_POLL_RATE = 50; //milliseconds
    private final List<ClientHttpRequestInterceptor> requestInterceptors = new ArrayList<ClientHttpRequestInterceptor>();


    public ServiceRequester(LinkedBlockingQueue<BufferedImage> imageQueue, String serviceUrl, ServiceController serviceController) {
        this.imageQueue = imageQueue;
        this.serviceUrl = serviceUrl;
        this.serviceController = serviceController;
        this.restTemplate = new RestTemplate();

        //this.requestInterceptors.add(new PerfRequestSyncInterceptor());
        //this.restTemplate.setInterceptors(requestInterceptors);
    }

    public HttpEntity<byte[]> createRequestHeaders(BufferedImage image) {
        byte[] imageBytes = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();

        headers.add("imageType", String.valueOf(image.getType()));
        headers.add("imageWidth", String.valueOf(image.getWidth()));
        headers.add("imageHeight", String.valueOf(image.getHeight()));

        return new HttpEntity<byte[]>(imageBytes, headers);
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Override
    public void run() {
        runningThread = Thread.currentThread();
        running = true;

        while (running) {
            try {

                BufferedImage image = imageQueue.poll(QUEUE_POLL_RATE, TimeUnit.MILLISECONDS);

                if (!running && imageQueue.isEmpty())
                    return;
                if (image != null) {

                    executeRequest(image);


                }

            } catch (InterruptedException e) {
                shutdown();
            }
        }

    }

    public void executeRequest(BufferedImage image) {
        long requestStartTime = System.currentTimeMillis();

        HttpEntity<byte[]> request = createRequestHeaders(image);

        RecognitionDTO responseDto = restTemplate.postForObject(serviceUrl, request, RecognitionDTO.class);
        if(responseDto.getPredictedPerson().length() > 0) {
            Log.info("Identified person: " + responseDto.getPredictedPerson());
        }
        Log.info("Total request time: " + (System.currentTimeMillis() - requestStartTime));
        Log.info("--------------------------------------");
        serviceController.receivedRecognitionDto(responseDto);
    }

    public void shutdown() {
        Log.info("ServiceRequester shutting down.");
        this.running = false;
        try {
            runningThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
