package com.yoti.mobile.android.capture.face.demo.automatic

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yoti.mobile.android.capture.face.demo.R
import com.yoti.mobile.android.capture.face.demo.databinding.ActivityAutomaticFaceCaptureBinding
import com.yoti.mobile.android.capture.face.demo.util.ByteArrayToBitmapConverter
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
import kotlinx.coroutines.launch


class AutomaticFaceCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAutomaticFaceCaptureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutomaticFaceCaptureBinding.inflate(layoutInflater)
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

    private fun stopAnalyzing() = with(binding.faceCaptureView) {
        if (isAnalysing()) {
            stopAnalysing()
        }
    }

    private fun onFaceCaptureResult(result: FaceCaptureResult) {
        when (val state = result.state) {
            is InvalidFace -> handleInvalidFace(state.cause)
            is ValidFace -> handleValidFace(state)
        }
    }

    private fun handleValidFace(result: ValidFace) {
        logState(R.string.yoti_fcm_demo_result_valid_face)
        setValidImageAsResult(result.croppedImage)
    }

    private fun handleInvalidFace(cause: FaceCaptureInvalid) {
        val detectionState = when (cause) {
            is AnalysisError -> R.string.yoti_fcm_demo_result_error_analysis
            is NoFaceDetected -> R.string.yoti_fcm_demo_result_error_no_face_detected
            is MultipleFacesDetected -> R.string.yoti_fcm_demo_result_error_multiple_faces
            is FaceTooBig -> R.string.yoti_fcm_demo_result_error_too_big
            is FaceTooSmall -> R.string.yoti_fcm_demo_result_error_too_small
            is FaceNotCentered -> R.string.yoti_fcm_demo_result_error_not_centered
            is FaceNotStraight -> R.string.yoti_fcm_demo_result_error_bad_angle
            is EyesClosed -> R.string.yoti_fcm_demo_result_error_eyes_closed
            is FaceNotStable -> R.string.yoti_fcm_demo_result_error_not_stable
            is EnvironmentTooDark -> R.string.yoti_fcm_demo_result_error_too_dark
        }
        logState(detectionState)
    }

    private fun setValidImageAsResult(image: ByteArray) {
        lifecycleScope.launch {
            binding.captureResult.setImageBitmap(ByteArrayToBitmapConverter().convert(image))
        }
    }

    private fun logState(@StringRes state: Int) {
        binding.userFeedback.setText(state)
    }

    private fun clearState() {
        binding.userFeedback.text = ""
    }

    private fun onCameraState(state: CameraState) {
        when (state) {
            is CameraInitializationError -> logState(handleCameraInitError(state.cause))
            is MissingPermissions -> logState(R.string.yoti_fcm_demo_camera_error_permissions)
            else -> {}
        }
    }

    private fun handleCameraInitError(error: CameraError) = when (error) {
        IllegalState -> R.string.yoti_fcm_demo_camera_error_illegal_state
        UnableToResolveCamera -> R.string.yoti_fcm_demo_camera_error_unable_to_resolve
        else -> R.string.yoti_fcm_demo_camera_error_unknown
    }
}
