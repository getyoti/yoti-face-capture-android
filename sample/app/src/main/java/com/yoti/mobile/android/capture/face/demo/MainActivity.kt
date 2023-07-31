package com.yoti.mobile.android.capture.face.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yoti.mobile.android.capture.face.demo.automatic.AutomaticFaceCaptureActivity
import com.yoti.mobile.android.capture.face.demo.databinding.ActivityMainBinding
import com.yoti.mobile.android.capture.face.demo.debug.DebugFaceCaptureActivity
import com.yoti.mobile.android.capture.face.demo.manual.ManualFaceCaptureActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this@MainActivity, REQUIRED_PERMISSIONS, 1)
        }

        with(binding) {
            startAutomaticFaceCaptureButton.setOnClickListener {
                startActivity(Intent(this@MainActivity, AutomaticFaceCaptureActivity::class.java))
            }
            startManualFaceCaptureButton.setOnClickListener {
                startActivity(Intent(this@MainActivity, ManualFaceCaptureActivity::class.java))
            }
            startDebugFaceCaptureButton.setOnClickListener {
                startActivity(Intent(this@MainActivity, DebugFaceCaptureActivity::class.java))
            }
        }

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

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

}

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
