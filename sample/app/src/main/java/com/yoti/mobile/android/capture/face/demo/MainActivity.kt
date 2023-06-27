package com.yoti.mobile.android.capture.face.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yoti.mobile.android.capture.face.demo.config.FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA
import com.yoti.mobile.android.capture.face.demo.config.FaceCaptureActivityConfiguration
import com.yoti.mobile.android.capture.face.demo.databinding.ActivityMainBinding
import com.yoti.mobile.android.capture.face.demo.debug.DebugFaceCaptureActivity
import com.yoti.mobile.android.capture.face.ui.models.face.ImageQuality

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    private var imageQuality: ImageQuality? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this@MainActivity, REQUIRED_PERMISSIONS, 1)
        }

        with(binding) {
            debugMode.setOnCheckedChangeListener { _, isChecked ->
                manualCaptureMode.isEnabled = !isChecked
                manualCaptureMode.isChecked = !isChecked
            }

            startFaceCapture.setOnClickListener {
                val activity = if (debugMode.isChecked) DebugFaceCaptureActivity::class.java else FaceCaptureActivity::class.java
                startActivity(Intent(this@MainActivity, activity).apply {
                    putExtra(
                            FACE_CAPTURE_ACTIVITY_CONFIGURATION_EXTRA,
                            getFaceCaptureActivityConfiguration()
                    )
                })
            }
        }

        populateImageQualitySpinner()
    }

    private fun getFaceCaptureActivityConfiguration(): FaceCaptureActivityConfiguration = with(binding) {
            FaceCaptureActivityConfiguration(
                    isBackCamera = backCamera.isChecked,
                    imageQuality = imageQuality ?: ImageQuality.default,
                    isManualCapture = manualCaptureMode.isChecked,
                    requireValidAngle = validAngle.isChecked,
                    requireEyesOpen = eyesOpen.isChecked,
                    requireBrightEnvironment = luminosity.isChecked,
                    requireStableFrames = stability.isChecked,
                    provideLandmarks = landmark.isChecked,
                    provideSmileScore = smileScore.isChecked
            )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults:
            IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun populateImageQualitySpinner() {
        val spinner = binding.quality.also {
            it.onItemSelectedListener = this
        }

        ArrayAdapter.createFromResource(
                this,
                R.array.yoti_fcm_demo_config_image_quality_array,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.setSelection(ImageQuality.MEDIUM.ordinal)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        imageQuality = ImageQuality.values()[position]
    }
}

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
