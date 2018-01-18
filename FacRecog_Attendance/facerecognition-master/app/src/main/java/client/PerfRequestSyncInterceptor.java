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

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PerfRequestSyncInterceptor implements ClientHttpRequestInterceptor {

    private final static Logger LOG = LoggerFactory.getLogger(PerfRequestSyncInterceptor.class);
    private final static Stopwatch stopwatch = new Stopwatch();
    @Override
    public ClientHttpResponse intercept(HttpRequest hr, byte[] bytes, ClientHttpRequestExecution chre) throws IOException {
        stopwatch.start();
        long time = System.currentTimeMillis();
        ClientHttpResponse response = chre.execute(hr, bytes);
        stopwatch.stop();

        LOG.info(hr.getMethod() + "@ uri="+hr.getURI() + " payload(kB)= "+ (bytes.length/1024) + ", response_time=" + stopwatch.elapsedTime(TimeUnit.MILLISECONDS)+  ", response_code=" + response.getStatusCode().value());

        return response;
    }
}