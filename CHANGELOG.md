# ChangeLog
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
