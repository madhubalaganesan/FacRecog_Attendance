# Face Recognition and Detection in Java with OpenCV
Deploys a web service & perform face recogntion by calling the service through a supplied client application.


The project consists of three modules:
* Common: all JavaCV related functionality is contained here, i.e. face recognition & detection, as well as camera capturing capabilities.
* Service: web server with REST interface for face recogntion & detection.
* App: uses camera device to capture images of user & sends recognition requests to the service.


## Setup


To use this program you must first add pictures to the database. Add pictures (preferably 1080x720) into the directory 'common/src/main/resources/recognition/training' (create this directory if needed) named in the following manner:

\<person-id>-\<person-name>_\<picture-id>.\<file-extension>

e.g.

1-Alice_1.jpg

1-Alice_2.jpg

1-Alice_3.jpg

2-Bob_1.jpg

2-Bob_2.jpg

...

### Generate Intellij Project:
./gradlew ideaModule


### Run the REST-service:
./gradlew service:run

###  Run the client:
./gradlew app:run


### Using jar files:
./gradlew assemble

java -jar service/build/libs/facerecog-service.jar

With GUI:

java -jar app/build/libs/facerecog-app.jar

Without GUI:

java -jar app/build/libs/facerecog-app.jar --nogui --path /Users/john/Pictures/johnProfile.jpg --url http://localhost:8080 --type /recog/detectIdentify --outdir /Users/john/recogPictures

type: [ /recog/detectIdentify | /recog/detect ]

