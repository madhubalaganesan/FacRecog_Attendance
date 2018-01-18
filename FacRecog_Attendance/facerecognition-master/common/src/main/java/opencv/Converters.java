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

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

public class Converters {

    public static final OpenCVFrameConverter.ToIplImage cvImageConverter = new OpenCVFrameConverter.ToIplImage();
    public static final OpenCVFrameConverter.ToMat cvMatConverter = new OpenCVFrameConverter.ToMat();
    public static final Java2DFrameConverter javaImageConverter = new Java2DFrameConverter();

}
