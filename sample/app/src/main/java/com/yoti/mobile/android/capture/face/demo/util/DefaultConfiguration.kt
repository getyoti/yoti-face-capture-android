package com.yoti.mobile.android.capture.face.demo.util

import com.yoti.mobile.android.capture.face.ui.models.camera.CameraConfiguration
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureConfiguration
import com.yoti.mobile.android.capture.face.ui.models.face.ImageQuality

val faceCaptureConfiguration = FaceCaptureConfiguration(
        faceCenter = FaceCaptureConfiguration.FACE_CENTER,
        imageQuality = ImageQuality.default,
        requireValidAngle = true,
        requireEyesOpen = true,
        requireBrightEnvironment = true,
        requiredStableFrames = 3,
        provideLandmarks =  false,
        provideSmileScore = false
)

val cameraConfiguration = CameraConfiguration()