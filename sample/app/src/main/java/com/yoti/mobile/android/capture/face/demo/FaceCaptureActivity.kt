package com.yoti.mobile.android.capture.face.demo

import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.yoti.mobile.android.capture.face.demo.config.FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA
import com.yoti.mobile.android.capture.face.demo.config.FaceCaptureActivityConfiguration
import com.yoti.mobile.android.capture.face.demo.config.FaceCaptureActivityConfiguration.CaptureMode
import com.yoti.mobile.android.capture.face.demo.config.FaceCaptureActivityConfiguration.CaptureMode.AUTOMATIC
import com.yoti.mobile.android.capture.face.demo.config.FaceCaptureActivityConfiguration.CaptureMode.MANUAL
import com.yoti.mobile.android.capture.face.demo.databinding.ActivityFaceCaptureBinding
import com.yoti.mobile.android.capture.face.demo.util.ByteArrayToBitmapConverter
import com.yoti.mobile.android.capture.face.demo.util.ImageBufferToBitmapConverter
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraConfiguration
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError.IllegalState
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraError.UnableToResolveCamera
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraFacing.BACK
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraFacing.FRONT
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraState
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraState.CameraInitializationError
import com.yoti.mobile.android.capture.face.ui.models.camera.CameraState.MissingPermissions
import com.yoti.mobile.android.capture.face.ui.models.face.FaceCaptureConfiguration
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

/**
 * Sample for Yoti Face Capture Module implementation. Please, use this code just as guidance. For final implementation,
 * we encourage you to use MVVM or another architectural pattern.
 *
 * Here you can check the following use cases:
 *
 * - Automatic capture: every valid frame will be delivered, we recommend just to pick the first one you receive if you use this mode
 *
 * - Manual capture: useful if you prefer the user to pick a picture of themselves when the face is valid instead to pick an automatic one
 *
 * - Fallback to Manual capture: if an Analysis error happens (not common, but worth to handle due possible device incompatibility issues) we
 *   recommend to use this method as fallback, so the user is able to pick a selfie even if their device is not compatible.
 *   The delivered image will be the original picture which was analysed.
 *
 *  If you want to check how the face capture bounding box works with your UI setup, pls check [com.yoti.mobile.android.capture.face.demo.debug.DebugFaceCaptureActivity]
 */
class FaceCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceCaptureBinding
    private lateinit var configuration: FaceCaptureConfiguration
    private lateinit var cameraConfiguration: CameraConfiguration
    private lateinit var captureMode: CaptureMode

    private var isManualCaptureButtonClicked: Boolean = false
    private var isInvalidFaceAllowed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val faceCaptureActivityConfig = (intent.getParcelableExtra<FaceCaptureActivityConfiguration>(FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA)!!)

        captureMode = if (faceCaptureActivityConfig.isManualCapture) MANUAL else AUTOMATIC
        cameraConfiguration = CameraConfiguration(facing = if (faceCaptureActivityConfig.isBackCamera) BACK else FRONT)
        configuration = FaceCaptureConfiguration(
                faceCenter = faceCaptureActivityConfig.faceCenter,
                imageQuality = faceCaptureActivityConfig.imageQuality,
                requireValidAngle = faceCaptureActivityConfig.requireValidAngle,
                requireEyesOpen = faceCaptureActivityConfig.requireEyesOpen,
                requireBrightEnvironment = faceCaptureActivityConfig.requireBrightEnvironment,
                requiredStableFrames = if (faceCaptureActivityConfig.requireStableFrames) 3 else 1,
                provideLandmarks =  faceCaptureActivityConfig.provideLandmarks,
                provideSmileScore = faceCaptureActivityConfig.provideSmileScore
        )

        binding.manualCaptureButton.setOnClickListener {
            isManualCaptureButtonClicked = true
        }
    }

    override fun onResume() {
        super.onResume()
        clearState()
        startCamera()
        binding.faceCaptureView.startAnalysing(configuration, ::onFaceCaptureResult)
    }

    override fun onPause() {
        stopAnalyzing()
        clearState()
        super.onPause()
    }

    private fun onFaceCaptureResult(result: FaceCaptureResult) {
        when (val state = result.state) {
            is InvalidFace -> handleInvalidFace(state.cause, result.originalImage)
            is ValidFace -> handleValidFace(state)
        }
    }

    private fun handleValidFace(result: ValidFace) {
        logState(R.string.yoti_fcm_demo_result_valid_face)
        when {
            isManualCaptureButtonClicked -> setValidImageAsResult(result.croppedImage, finishCapture = true)
            captureMode == AUTOMATIC -> setValidImageAsResult(result.croppedImage, finishCapture = false)
            captureMode == MANUAL -> binding.manualCaptureButton.isInvisible = false
        }
    }

    private fun handleInvalidFace(cause: FaceCaptureInvalid, analysedImage: ImageBuffer? = null) {
        if (isInvalidFaceAllowed && isManualCaptureButtonClicked && analysedImage != null) {
            setInvalidImageAsResult(analysedImage)
            return
        }

        binding.manualCaptureButton.isInvisible = true
        val detectionState = when (cause) {
            is AnalysisError -> {
                // Enable manual capture if this happens to allow the user to pick a picture
                isInvalidFaceAllowed = true
                binding.manualCaptureButton.isInvisible = false
                R.string.yoti_fcm_demo_result_error_analysis
            }
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

    private fun setInvalidImageAsResult(image: ImageBuffer) {
        lifecycleScope.launch {
            ImageBufferToBitmapConverter().convert(image)?.let { setImageAsResult(it, true) }
        }
    }

    private fun setValidImageAsResult(image: ByteArray, finishCapture: Boolean) {
        lifecycleScope.launch {
            setImageAsResult(ByteArrayToBitmapConverter().convert(image), finishCapture)
        }
    }

    private fun setImageAsResult(image: Bitmap, finishCapture: Boolean = false) {
        binding.captureResult.setImageBitmap(image)
        if (finishCapture) {
            binding.manualCaptureButton.isInvisible = true
            stopAnalyzing()
            clearState()
        }
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

