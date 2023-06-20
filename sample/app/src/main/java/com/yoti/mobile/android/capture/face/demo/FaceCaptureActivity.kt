package com.yoti.mobile.android.capture.face.demo

import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.yoti.mobile.android.capture.face.demo.CaptureMode.AUTOMATIC
import com.yoti.mobile.android.capture.face.demo.CaptureMode.MANUAL
import com.yoti.mobile.android.capture.face.demo.databinding.ActivityFaceCaptureBinding
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
import com.yoti.mobile.android.capture.face.ui.models.face.ImageQuality
import com.yoti.mobile.android.commons.image.ImageBuffer
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

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
 * - Debug mode: activate it if you have doubts of what's the faceCenter position you need to set given your custom layout
 *
 */
class FaceCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceCaptureBinding
    private lateinit var configuration: FaceCaptureConfiguration
    private lateinit var cameraConfiguration: CameraConfiguration

    private var isDebugMode: Boolean = false
    private var captureMode: CaptureMode = AUTOMATIC
    private var isFirstFrameValid: Boolean = false
    private var isInvalidFaceAllowed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val captureConfigExtra = (intent.getParcelableExtra(FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA) as? FaceCaptureActivityConfiguration)!!

        isDebugMode = captureConfigExtra.isDebugMode
        captureMode = if (captureConfigExtra.isManualCapture) MANUAL else AUTOMATIC
        cameraConfiguration = CameraConfiguration(facing = if (captureConfigExtra.isBackCamera) BACK else FRONT)
        configuration = FaceCaptureConfiguration(
                faceCenter = captureConfigExtra.faceCenter,
                imageQuality = captureConfigExtra.imageQuality,
                requireValidAngle = captureConfigExtra.requireValidAngle,
                requireEyesOpen = captureConfigExtra.requireEyesOpen,
                requireBrightEnvironment = captureConfigExtra.requireBrightEnvironment,
                requiredStableFrames = if (captureConfigExtra.requireStableFrames) 3 else 1,
                provideLandmarks =  captureConfigExtra.provideLandmarks,
                provideSmileScore = captureConfigExtra.provideSmileScore
        )


        with(binding) {
            manualCaptureButton.setOnClickListener {
                isFirstFrameValid = true
            }
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
        if (isDebugMode) {
            clearOverlay()
        }
    }

    private fun onCameraState(state: CameraState) {
        when (state) {
            is CameraInitializationError -> logState(handleCameraInitError(state.cause))
            is MissingPermissions -> logState(R.string.yoti_fcm_demo_camera_error_permissions)
            else -> {
            }
        }
    }

    private fun handleCameraInitError(error: CameraError) = when (error) {
        IllegalState -> R.string.yoti_fcm_demo_camera_error_illegal_state
        UnableToResolveCamera -> R.string.yoti_fcm_demo_camera_error_unable_to_resolve
        else -> R.string.yoti_fcm_demo_camera_error_unknown
    }

    private fun onFaceCaptureResult(result: FaceCaptureResult) {
        when (val state = result.state) {
            is InvalidFace -> handleInvalidFace(state.cause, result.originalImage)
            is ValidFace -> handleValidFace(state, result.originalImage)
        }
    }

    private fun handleValidFace(result: ValidFace, originalImage: ImageBuffer? = null) {
        logState(R.string.yoti_fcm_demo_result_valid_face)

        if (isFirstFrameValid) {
            Glide.with(this@FaceCaptureActivity).load(result.croppedImage).into(binding.captureResult)

            isFirstFrameValid = false
            binding.manualCaptureButton.isInvisible = true
            stopAnalyzing()
            clearState()

            return
        }

        if (captureMode == MANUAL) {
            binding.manualCaptureButton.isInvisible = false
        }
        else {
            Glide.with(this@FaceCaptureActivity).load(result.croppedImage).into(binding.captureResult)
        }

        if (isDebugMode && originalImage != null) {
            updateFaceOutline(
                    result.faceBoundingBox,
                    result.croppedFaceBoundingBox,
                    Size(originalImage.width, originalImage.height)
            )
        }
    }

    private fun handleInvalidFace(cause: FaceCaptureInvalid, originalImage: ImageBuffer? = null) {
        if (isInvalidFaceAllowed && isFirstFrameValid && originalImage != null) {
            binding.manualCaptureButton.isInvisible = true
            stopAnalyzing()
            clearState()
            isFirstFrameValid = false
            lifecycleScope.launch {
                Glide.with(this@FaceCaptureActivity).load(ImageBufferToBitmapConverter().convert(imageBuffer = originalImage)).into(binding.captureResult)
            }

            return
        }

        if (captureMode == MANUAL) {
            binding.manualCaptureButton.isInvisible = true
        }

        val detectionState = when (cause) {
            is AnalysisError -> {
                // Enable manual capture if this happens to allow to pick the first frame
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

        if (isDebugMode) {
            clearOverlay()
        }
    }

    // Debug mode
    private fun updateFaceOutline(
            boundingBox: Rect,
            croppedBoundingBox: Rect,
            resolution: Size,
    ) {
        clearOverlay()
        drawFaceCenterPoint()
        drawFaceOutline(boundingBox, resolution)
        drawCroppedFaceOutline(croppedBoundingBox, resolution)
    }

    private fun drawFaceCenterPoint() {
        val centerX =
                (cameraConfiguration.targetResolution.width * configuration.faceCenter.x).roundToInt()
        val centerY =
                (cameraConfiguration.targetResolution.height * configuration.faceCenter.y).roundToInt()
        val outlineThickness = 5
        val outline = Rect(
                centerX - outlineThickness,
                centerY - outlineThickness,
                centerX + outlineThickness,
                centerY + outlineThickness

        )
        binding.debugModeOverlay.drawBoundingBox(
                outline,
                Color.BLUE,
                cameraConfiguration.targetResolution
        )
    }

    private fun drawFaceOutline(boundingBox: Rect, resolution: Size) {
        binding.debugModeOverlay.drawBoundingBox(boundingBox, Color.RED, resolution)
    }

    private fun drawCroppedFaceOutline(boundingBox: Rect, resolution: Size) {
        binding.debugModeOverlay.drawBoundingBox(boundingBox, Color.GREEN, resolution)
    }

    private fun clearOverlay() {
        binding.debugModeOverlay.clearBoundingBox()
    }
}

private val FACE_CENTER = PointF(.5F, .45F)

private enum class CaptureMode {
    AUTOMATIC,
    MANUAL
}

const val FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA = "FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA"
@Parcelize
data class FaceCaptureActivityConfiguration(
        val isBackCamera: Boolean = false,
        val isDebugMode: Boolean = false,
        val isManualCapture: Boolean = false,
        val faceCenter: PointF = FACE_CENTER,
        val imageQuality: ImageQuality = ImageQuality.default,
        val requireValidAngle: Boolean = true,
        val requireEyesOpen: Boolean = true,
        val requireBrightEnvironment: Boolean = true,
        val requireStableFrames: Boolean = true,
        val provideLandmarks: Boolean = true,
        val provideSmileScore: Boolean = true
): Parcelable
