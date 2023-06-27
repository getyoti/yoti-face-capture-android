package com.yoti.mobile.android.capture.face.demo.config

import android.graphics.PointF
import android.os.Parcelable
import com.yoti.mobile.android.capture.face.ui.models.face.ImageQuality
import kotlinx.parcelize.Parcelize

val FACE_CENTER = PointF(.5F, .45F)

const val FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA = "FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA"
@Parcelize
data class FaceCaptureActivityConfiguration(
        val isBackCamera: Boolean = false,
        val isManualCapture: Boolean = false,
        val faceCenter: PointF = FACE_CENTER,
        val imageQuality: ImageQuality = ImageQuality.default,
        val requireValidAngle: Boolean = true,
        val requireEyesOpen: Boolean = true,
        val requireBrightEnvironment: Boolean = true,
        val requireStableFrames: Boolean = true,
        val provideLandmarks: Boolean = true,
        val provideSmileScore: Boolean = true
): Parcelable {

    enum class CaptureMode {
        AUTOMATIC,
        MANUAL
    }
}