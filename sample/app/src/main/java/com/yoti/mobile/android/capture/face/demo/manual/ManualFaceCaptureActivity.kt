package com.yoti.mobile.android.capture.face.demo.manual

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.yoti.mobile.android.capture.face.demo.R.string
import com.yoti.mobile.android.capture.face.demo.databinding.ActivityManualFaceCaptureBinding
import com.yoti.mobile.android.capture.face.demo.util.ByteArrayToBitmapConverter
import com.yoti.mobile.android.capture.face.demo.util.cameraConfiguration
import com.yoti.mobile.android.capture.face.demo.util.faceCaptureConfiguration
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError.IllegalState
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError.UnableToResolveCamera
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraState
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraState.CameraFacingSwapped
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
import kotlinx.coroutines.launch


class ManualFaceCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManualFaceCaptureBinding

    private var isManualCaptureButtonClicked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualFaceCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.manualCaptureButton.setOnClickListener {
            isManualCaptureButtonClicked = true
        }
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

    private fun onFaceCaptureResult(result: FaceCaptureResult) {
        when (val state = result.state) {
            is InvalidFace -> handleInvalidFace(state.cause)
            is ValidFace -> handleValidFace(state)
        }
    }

    private fun handleValidFace(result: ValidFace) {
        logState(string.yoti_fcm_demo_result_valid_face)

        binding.manualCaptureButton.isInvisible = false
        if (isManualCaptureButtonClicked) {
            isManualCaptureButtonClicked = false
            setValidImageAsResult(result.croppedImage)
        }
    }

    private fun handleInvalidFace(cause: FaceCaptureInvalid) {
        binding.manualCaptureButton.isInvisible = true
        isManualCaptureButtonClicked = false
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
    }

    private fun setValidImageAsResult(image: ByteArray) {
        lifecycleScope.launch {
            binding.captureResult.setImageBitmap(ByteArrayToBitmapConverter().convert(image))
        }
        binding.manualCaptureButton.isInvisible = true
        stopAnalyzing()
        clearState()
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
        isManualCaptureButtonClicked = false
        binding.userFeedback.text = ""
    }

    private fun onCameraState(state: CameraState) {
        when (state) {
            is CameraInitializationError -> logState(handleCameraInitError(state.cause))
            is MissingPermissions -> logState(string.yoti_fcm_demo_camera_error_permissions)
            is CameraFacingSwapped -> logState(string.yoti_fcm_demo_camera_facing_camera_swapped)
            else -> {}
        }
    }

    private fun handleCameraInitError(error: CameraError) = when (error) {
        IllegalState -> string.yoti_fcm_demo_camera_error_illegal_state
        UnableToResolveCamera -> string.yoti_fcm_demo_camera_error_unable_to_resolve
        else -> string.yoti_fcm_demo_camera_error_unknown
    }
}

