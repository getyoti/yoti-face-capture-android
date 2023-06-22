# yoti-face-capture-android

yoti-face-capture-android provides a simplified way of capturing a face. It performs face detection from the front facing or back camera, analyses those frames and produces an optimised cropped image of the captured face.

This library leverages on [Google ML Kit](https://firebase.google.com/docs/ml-kit/detect-faces) to perform face detection.

## Requirements
- Android 21+

##  Dependencies

In your `gradle.properties` add one of the following dependency
```
implementation 'com.yoti.mobile.android:face-capture-bundled:4.2.0'
```

```
implementation 'com.yoti.mobile.android:face-capture-unbundled:4.2.0'
```

#### Bundled VS Unbundled

We offer two options to add this library to your app, bundled and unbundled.

The bundled version embeds a 16Mb AI model for face detection with about 23.1Mb total SDK size.

The unbundled version which has an estimated SDK size of 2.4Mb, will manage the download of the AI model via Google Play Services the first time you start using the AI model. It is recommended to add the following metadata to your `manifest.xml` to get the model downloaded as soon as the app is installed.
```
<application ...>
  ...
  <meta-data
      android:name="com.google.firebase.ml.vision.DEPENDENCIES"
      android:value="face" />

</application>

```

## Usage

### 1. Add the view to your layout
Add the `FaceCapture` View to your layout

```
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">


    <com.yoti.mobile.android.capture.face.ui.FaceCapture
        android:id="@+id/faceCapture"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    ...

</androidx.constraintlayout.widget.ConstraintLayout>        
```

### 2. Create your configuration

```
val faceCenter = PointF(0.5F, 0.5F)
val configuration = FaceCaptureConfiguration(
                        faceCenter,
                        ImageQuality.MEDIUM,
                        requireValidAngle = true,
                        requireEyesOpen = true,
                        requireBrightEnvironment = true,
                        requiredStableFrames = 3,
                        provideLandmarks = true,
                        provideSmileScore = true
                    )

```

#### Face center
The face center is a `PointF` representing the expected position of the centre of the user's face inside the `FaceCapture` view. In the samples below, it is represented by the intersection of the red and blue lines in the sample images.
If the actual face center is not near this point it will not be considered a valid face.

- In this sample we set as faceCenter a point which has to match with where the face silhouette is inside the `FaceCapture` view. This is the place where the user's face will be. In this case, the faceCenter is set to `PointF(.5F, .45F)`, which means, a 50% of the horizontal axis and 45% of the vertical one.

<p align="center">
<img width="30%" src="https://github.com/getyoti/yoti-face-capture-android/assets/33830959/f5979597-2fc5-4dd1-9533-20c2b24bc8b2">
</p>

- In this other sample, we have a face silhouette which is closer to the top of the `FaceCapture` view, so we should move the vertical axis point towards the upper part, so in this case the FaceCenter configuration is set to `PointF(.5F, .35F)`:

<p align="center">
<img width="30%" src="https://github.com/getyoti/yoti-face-capture-android/assets/33830959/1bd0cd68-41aa-40c1-8e1a-2c33110d153c">
</p>


#### Image Quality
This is the image quality of the cropped image after it has been compressed and converted to JPEG. It can be either `ImageQuality.LOW` or `ImageQuality.MEDIUM` or `ImageQuality.HIGH`

### Require Valid Angle
This boolean if true, will require the picture to be taken with a tilt angle no bigger than 30 degrees.
When this requirement is not met `FaceNotStraight` error is returned.

### Require Eyes Open
This boolean if true it will require the eyes to be opened.
When this requirement is not met `EyesClosed` error is returned.

### Require Bright Environment
If true it will require the environment luminosity to be above a pre-determined threshold.
When this requirement is not met `EnvironmentTooDark` error is returned.

### Require Stable Frames
This integer will require "n" number of frames to be as similar as possible in terms of width/hight and x/y position.
The purpose of this is to avoid capturing blurry images.
When this requirement is not met `FaceNotStable` error is returned.

### Provide Landmarks
If set to true, SDK will return facial landmark points for both original and cropped images on a valid face. These set of points are nullable.

### Provide Smile Score
If set to true, SDK will return smile score on a valid face. This score is a nullable value.

### 3. Retreive your view
```
val faceCapture = findViewById<FaceCapture>(R.id.faceCapture)
```


### 4. Start the camera
There are two ways to start the camera:

```
faceCapture.startCamera(this, ::onCameraState)
```

Or you could also do:
```
faceCapture.cameraState.observe(this, ::onCameraState)
faceCaputure.startCamera(this)
```


#### Camera States

There are a few states that can be returned to allow the integrator to know what the current state of the Face Capture is. These are:
- CameraReady - The Face Capture has connected to the camera and the preview is available, but no analyzing is happening
- CameraStopped - The camera has stopped and no analyzing is happening.
- Analyzing - The camera is ready and the Face Capture is analyzing frames to detect faces.
- CameraInitializationError - There was an error initialzing the camera.
- MissingPermissions - The Face Capture does not have sufficient permissions to caccess the camera.

The following CameraErrors can be returned when the state is CameraInitializationError
- IllegalState
- UnableToResolveCamera
- Unknown


### 5. Start analyzing

Start the detection and listen for incoming `FaceCaptureResult`
```
faceCapture.startAnalysing(configuration, ::onFaceCaptureResult)
```
This can be called straight after startCamera(), no need to wait for `CameraReady`

#### Output Information

Result of the face capture containing the following:
  - Original Image. This will be a 1280x720 YUV image
  - State of the face capture:
    - Invalid Face containing the reason it is invalid
    - Valid Face containing:
      - Cropped Image - Byte array representation of a compressed JPEG image based on the configured image quality
      - The bounding box of the face inside the cropped image
      - The bounding box of the face inside the original image
      - Facial landmark points for the original image
      - Facial landmark points for the cropped image
      - Smile score between 0.0 (unlikely smiling) and 1.0 (more likely smiling)

If the cropping of the face did not meet the requirements then the Invalid Face will be returned. This will not contain any cropped image.

##### Error States

The error states and validation states are in a specific order. For example, the FaceTooSmall check will be performed before the FaceNotCentered check. As such here are the states that can be returned in order of the checks that are done:
- AnalysisError
- NoFaceDetected
- MultipleFacesDetected
- FaceTooSmall
- FaceTooBig
- FaceNotCentered
- EnvironmentTooDark

Optional errors (depending on the configuration passed):
- FaceNotStraight
- EyesClosed
- FaceNotStable

### 6. Stop the library
```
faceCapture.stopAnalysing()
faceCapture.stopCamera()
```
This is only required if it is part of your camera flow. Not required in response of lifecycle changes.

### Implementation sample
You can find a sample App [here](https://github.com/getyoti/yoti-face-capture-android/sample) 

### Support
If you have any other questions please do not hesitate to contact [clientsupport@yoti.com](clientsupport@yoti.com)
Once we have answered your question we may contact you again to discuss Yoti products and services. If you'd prefer us not to do this, please let us know when you e-mail.

