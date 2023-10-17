package com.example.scanner4zeffy

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 1
        private const val TAG = "MainActivity"
    }

    private fun playSound(resourceId: Int) {
        val mediaPlayer = MediaPlayer.create(this, resourceId)
        mediaPlayer.setOnCompletionListener {
            it.release()
            Log.d(TAG, "Media player released.")
    }
        mediaPlayer.start()  // This will start the media player immediately after creation
        Log.d(TAG, "Attempting to play sound.")
}

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val webView: WebView = findViewById(R.id.web_view)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                webView.evaluateJavascript("document.body.innerText") { result ->
                    // check for specific text in the html
                    if (result.contains("Already Validated")) {
                        playSound(R.raw.chip_negative)
                    }
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        val previewView: PreviewView = findViewById(R.id.preview_view)
        startCamera(previewView)

        val playSoundButton: Button = findViewById(R.id.play_sound_button)
        playSoundButton.setOnClickListener {
            playSound(R.raw.chip_positive)
        }
    }

    private fun startCamera(previewView: PreviewView) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "We need camera access to scan QR codes", Toast.LENGTH_LONG).show()
            }
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                MY_PERMISSIONS_REQUEST_CAMERA)
            return
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setImageQueueDepth(3) // Number of frames of images in the queue
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeAnalyzer(
                    onQRCodeDetected = { url ->
                    val webView: WebView = findViewById(R.id.web_view)
                    webView.loadUrl(url)
                },
                playSound = ::playSound
            ))
            }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            cameraProvider.unbindAll()

            try {
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    val previewView: PreviewView = findViewById(R.id.preview_view)
                    startCamera(previewView)  // <-- Here you call startCamera without the previewView argument
                } else {
                    // Permission was denied. Disable the functionality that depends on this permission.
                    Toast.makeText(this, "Camera permission is required to use the scanner", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                // Handle other permissions if any
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
class QRCodeAnalyzer(private val onQRCodeDetected: (String) -> Unit, private val playSound: (Int) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()
    private var lastScanTime = 0L
    companion object {
        private const val TAG = "QRCodeAnalyzer"
    }
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < 1500) { //
            // Skip this frame
            return
        }

        Log.d(TAG, "Analyzing frame...")

        val yBuffer = image.planes[0].buffer
        val ySize = yBuffer.remaining()
        val yArray = ByteArray(ySize)
        yBuffer[yArray]

        val source = PlanarYUVLuminanceSource(
            yArray,
            image.width,
            image.height,
            0,
            0,
            image.width,
            image.height,
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decode(binaryBitmap)
            onQRCodeDetected(result.text)
            playSound(R.raw.chip_mouseover2)
            lastScanTime = currentTime
            Log.d(TAG, "QR code detected: ${result.text}")
        } catch (e: NotFoundException) {
            Log.e(TAG, "QR code not found", e)
            // No QR code found
        } finally {
            image.close()
        }
    }
}


