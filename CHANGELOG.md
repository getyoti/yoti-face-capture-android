# ChangeLog
## Version 4.7.0
### Added
**Self-checkout mode**:
This new feature matches with `self-checkout` EP behaviour and should be activated when FCM is used in environments where it is common to have people around while the capture is on going, such a self-checkout point in a supermarket.
It can be activated by setting `FaceCaptureConfiguration::isSelfCheckOutMode` to true.
- When it is `true`, if several faces are detected and if there is **only one** valid, FCM will return `ValidFace`. 
If several faces are detected and none of them are valid or there are more than one valid faces, FCM will continue returning `MultipleFacesDetected`.
- If it is set to `false`, whenever FCM detects more than one face, FCM will return `MultipleFacesDetected`
**By default it is set to false.**

## Version 4.6.0
### Added
- Ability to select external camera: set `CameraFacing.EXTERNAL` in `CameraConfiguration`
- Ability to customise the Zoom level: set `zoomlevel` in `CameraConfiguration`

## Version 4.5.0
Changed: New `CameraState`: `CameraFacingSwapped`

New `CameraFacingSwapped` is sent by the SDK when the configured facing camera is not available and the SDK has swapped automatically to the other facing camera (if available) 

## Version 4.4.1
Fixed: Issue when publishing the FCM library to Maven

## Version 4.4.0
Changed: Upgrade CameraX libraries to version 1.2.3

## Version 4.3.0
Changed: Updated com.google.mlkit:face-detection to version 16.1.5
Updated com.google.android.gms:play-services-mlkit-face-detection to version 17.1.0

## Version 4.2.1
Changed: Update to kotlin 1.6. Remove requirement for jetifier in consuming apps.

## Version 4.1.2
Fix: face capture view was not render in Android Studio preview.

## Version 4.1.1
Fix: Face capture module will return `AnalysisError` if libraries for image treatment are not found.

## Version 4.1.0
CHANGE: New FaceCaptureConfiguration requirements

New configurable properties `provideLandmarks` and `provideSmileScore` have been added to the SDK entry point.
If these parameters are set to `true` (by default `false`), the SDK will return facial landmarks points for original and the cropped image and smile score (both values are nullable).
Please check [README.md](https://github.com/getyoti/yoti-face-capture-android/blob/main/README.md) for more details.

## Version 4.0.0
BREAKING CHANGE: New FaceCaptureConfiguration requirements

The configuration has been updated and expects a `faceCenter (PointF)` representing the expected center of the captured face.
This parameter is a percentage value (x, y). E.g.: (0,0) - top left; (0.5, 0.5) - center of the screen; (1,1) - bottom right; 

Please, check [README.md](https://github.com/getyoti/yoti-face-capture-android/blob/main/README.md) for more details.

## Version 3.0.0

BREAKING CHANGE: Low light detection

New configurable property `requireBrightEnvironment` has been added to the SDK entry point. If it is activated (it is by default), the SDK will require a bright environment to take the selfie. If the environment where the face is being captured is not meeting the brightness threshold value, the SDK will return a new error called `environmentTooDark`.
To migrate from previous versions, it is needed to include `requireBrightEnvironment` setup on the SDK setup.
Please, check [README.md](https://github.com/getyoti/yoti-face-capture-android/blob/main/README.md) for more details.

## Version 2.3.0

BREAKING CHANGE: capture requirements

val configuration = FaceCaptureConfiguration(scanning_region, ImageQuality.MEDIUM, requireValidAngle = true, requireEyesOpen = true, requiredStableFrames = 3)

Configurable properties `requireValidAngle`, `requireEyesOpen` and `requiredStableFrames` have been added to the SDK entry point. 
Please, check [README.md](https://github.com/getyoti/yoti-face-capture-android/blob/main/README.md) for more details.


## Version 1.0.0

Face capture first release
