package com.yoti.mobile.android.capture.face.demo.debug

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Size
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.yoti.mobile.android.capture.face.demo.R.string
import com.yoti.mobile.android.capture.face.demo.databinding.ActivityDebugFaceCaptureBinding
import com.yoti.mobile.android.capture.face.demo.util.cameraConfiguration
import com.yoti.mobile.android.capture.face.demo.util.faceCaptureConfiguration
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError.IllegalState
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError.UnableToResolveCamera
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraState
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraState.CameraInitializationError
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraState.MissingPermissions
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.AnalysisError
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.EnvironmentTooDark
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.EyesClosed
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.FaceNotCentered
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.FaceNotStable
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.FaceNotStraight
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.FaceTooBig
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.FaceTooSmall
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.MultipleFacesDetected
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureInvalid.NoFaceDetected
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureResult
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureState.InvalidFace
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureState.ValidFace
import com.yoti.mobile.android.commons.image.ImageBuffer
import kotlin.math.roundToInt


class DebugFaceCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugFaceCaptureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugFaceCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        clearState()
        startCamera()
        binding.faceCaptureView.startAnalysing(faceCaptureConfiguration, ::onFaceCaptureResult)
    }

    override fun onPause() {
        stopAnalyzing()
        clearState()
        super.onPause()
    }

    private fun startCamera() {
        binding.faceCaptureView.startCamera(this, ::onCameraState, cameraConfiguration)
    }

    private fun stopAnalyzing() = with(binding) {
        if (faceCaptureView.isAnalysing()) {
            faceCaptureView.stopAnalysing()
        }
    }

    private fun logState(@StringRes state: Int) {
        binding.userFeedback.setText(state)
    }

    private fun clearState() {
        binding.userFeedback.text = ""
        clearOverlay()
    }

    private fun onCameraState(state: CameraState) {
        when (state) {
            is CameraInitializationError -> logState(handleCameraInitError(state.cause))
            is MissingPermissions -> logState(string.yoti_fcm_demo_camera_error_permissions)
            else -> {
            }
        }
    }

    private fun handleCameraInitError(error: CameraError) = when (error) {
        IllegalState -> string.yoti_fcm_demo_camera_error_illegal_state
        UnableToResolveCamera -> string.yoti_fcm_demo_camera_error_unable_to_resolve
        else -> string.yoti_fcm_demo_camera_error_unknown
    }

    private fun onFaceCaptureResult(result: FaceCaptureResult) {
        when (val state = result.state) {
            is InvalidFace -> handleInvalidFace(state.cause)
            is ValidFace -> result.originalImage?.let {  handleValidFace(state, it) }
        }
    }

    private fun handleValidFace(result: ValidFace, analysedImage: ImageBuffer) {
        logState(string.yoti_fcm_demo_result_valid_face)
        updateFaceOutline(
                result.faceBoundingBox,
                result.croppedFaceBoundingBox,
                Size(analysedImage.width, analysedImage.height)
        )
    }

    private fun handleInvalidFace(cause: FaceCaptureInvalid) {
        val detectionState = when (cause) {
            is AnalysisError -> string.yoti_fcm_demo_result_error_analysis
            is NoFaceDetected -> string.yoti_fcm_demo_result_error_no_face_detected
            is MultipleFacesDetected -> string.yoti_fcm_demo_result_error_multiple_faces
            is FaceTooBig -> string.yoti_fcm_demo_result_error_too_big
            is FaceTooSmall -> string.yoti_fcm_demo_result_error_too_small
            is FaceNotCentered -> string.yoti_fcm_demo_result_error_not_centered
            is FaceNotStraight -> string.yoti_fcm_demo_result_error_bad_angle
            is EyesClosed -> string.yoti_fcm_demo_result_error_eyes_closed
            is FaceNotStable -> string.yoti_fcm_demo_result_error_not_stable
            is EnvironmentTooDark -> string.yoti_fcm_demo_result_error_too_dark
        }
        logState(detectionState)
        clearOverlay()
    }

    private fun updateFaceOutline(
            boundingBox: Rect,
            croppedBoundingBox: Rect,
            resolution: Size,
    ) {
        clearOverlay()
        drawFaceOutline(boundingBox, resolution)
        drawCroppedFaceOutline(croppedBoundingBox, resolution)
    }

    private fun drawFaceCenterPoint() {
        val centerX =
                (cameraConfiguration.targetResolution.width * faceCaptureConfiguration.faceCenter.x).roundToInt()
        val centerY =
                (cameraConfiguration.targetResolution.height * faceCaptureConfiguration.faceCenter.y).roundToInt()
        val outlineThickness = 2
        val outline = Rect(
                centerX - outlineThickness,
                centerY - outlineThickness,
                centerX + outlineThickness,
                centerY + outlineThickness

        )
        binding.debugModeOverlay.drawBoundingBox(
                outline,
                Color.RED,
                cameraConfiguration.targetResolution
        )
    }

    private fun drawFaceOutline(boundingBox: Rect, resolution: Size) {
        binding.debugModeOverlay.drawBoundingBox(boundingBox, Color.GREEN, resolution)
    }

    private fun drawCroppedFaceOutline(boundingBox: Rect, resolution: Size) {
        binding.debugModeOverlay.drawBoundingBox(boundingBox, Color.BLUE, resolution)
    }

    private fun clearOverlay() {
        binding.debugModeOverlay.clearBoundingBox()
        drawFaceCenterPoint()
    }
}
