# yoti-face-capture-android

yoti-face-capture-android provides a simplified way of capturing a face. It performs face detection from the front facing camera, analyses those frames and produces an optimised cropped image of the captured face.

This library leverage [Google ML Kit](https://firebase.google.com/docs/ml-kit/detect-faces) to perform the face detection.

## Requirements
- Android 21+

##  Dependencies

In your `gradle.properties` add one of the following dependency
```
implementation 'com.yoti.mobile.android:face-capture-bundled:1.0.0'
```

```
implementation 'com.yoti.mobile.android:face-capture-unbundled:1.0.0'
```

#### Bundled VS Unbundled

We offer two options to add this library to your app, bundled and unbundled.

The bundled version embeds a 16Mb AI model for face detection.

The unbundled version will manage the download of the AI model via Google Play Services the first time you start using the AI model. Additionally you can add the following metadata to your `manifest.xml` to get the model downloaded as soon as the app is installed.
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
val scanning_region = Rect(20, 200, 700, 800)
val configuration = FaceCaptureConfiguration(scanning_region, ImageQuality.MEDIUM)

```

#### Image Quality
This is the image quality of the cropped image after it has been compressed and converted to JPEG. It can be either `ImageQuality.LOW` or `ImageQuality.MEDIUM` or `ImageQuality.HIGH`

#### Scanning Area
The scanning area is a `Rect` that represent the region in which the face can only be detected. If the face is outside of this region it will not be considered a valid face. A default will be applied for this.


### 3. Retreive your view
```
val faceCapture = findViewById<FaceCapture>(R.id.faceCapture)
```


### 4. Start the camera
```
faceCapture.startCamera(this, ::onCameraState)
```

#### Camera States

There are a few states that can be returned to allow the integrator to know what the current state of the Face Capture is. these are:
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

#### Output Information

Result of the face capture containing the following:
  - Original Image. This will be a 1280x720 YUV image
  - State of the face capture:
    - Invalid Face containing the reason it is invalid
    - Valid Face containing:
      - Cropped Image - Byte array representation of a compressed JPEG image based on the configured image quality
      - The bounding box of the face inside the cropped image
      - The bounding box of the face inside the original image

If the cropping of the face did not meet the requirements then the Invalid Face will be returned. This will not contain any cropped image.

##### Error States

The error states and validation states are in a specific order. For example, the FaceTooSmall check will be performed before the FaceNotCentered check. As such here are the states that can be returned in order of the checks that are done:
- AnalysisError
- NoFaceDetected
- MultipleFacesDetected
- FaceTooSmall
- FaceTooBig
- FaceNotCentered

### 6. Stop the library
```
faceCapture.stopAnalysing()
faceCapture.stopCamera()
```
