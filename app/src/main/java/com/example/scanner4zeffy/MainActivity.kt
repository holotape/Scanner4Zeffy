package com.example.scanner4zeffy

// import android.widget.Button
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var preview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var vibrator: Vibrator
    companion object {
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 1
        private const val TAG = "MainActivity"
    }

    private fun playSound(resourceId: Int) {
        val mediaPlayer = MediaPlayer.create(this, resourceId)
        mediaPlayer.setOnCompletionListener {
            it.release()
//          Log.d(TAG, "Media player released.")
    }
        mediaPlayer.start()
//      Log.d(TAG, "Attempting to play sound.")
}

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        cameraExecutor = Executors.newSingleThreadExecutor()

        val webView: WebView = findViewById(R.id.web_view)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                webView.evaluateJavascript("document.body.innerText") { result ->
                    // check for specific text in the html document
                    if (result.contains("Already Validated")) {
                        playSound(R.raw.chip_negative)
                    }
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        val previewView: PreviewView = findViewById(R.id.preview_view)
        startCamera(previewView)

        /*val playSoundButton: Button = findViewById(R.id.play_sound_button)
        playSoundButton.setOnClickListener {
            playSound(R.raw.chip_positive)
        }*/
    }

    private fun vibrate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
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
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setImageQueueDepth(3) // Number of frames of images in the queue
            .build()
            .also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(this),
                    QRCodeAnalyzer(
                        onQRCodeDetected = { url ->
                        val webView: WebView = findViewById(R.id.web_view)
                        webView.loadUrl(url)
                    },
                    playSound = ::playSound,
                        vibrate = ::vibrate,
                        rebuildQueue = ::rebuildImageAnalysisQueueDepth // Pass the function to rebuild the queue
                )
            )
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
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
                    startCamera(previewView)
                } else {
                    // Permission was denied. Disable the functionality that depends on this permission.
                    Toast.makeText(this, "Camera permission is required to use the scanner", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    fun rebuildImageAnalysisQueueDepth(newDepth: Int) {
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        cameraProvider.unbind(imageAnalysis)

        val newImageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setImageQueueDepth(newDepth)
            .build()

        newImageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(this),
            QRCodeAnalyzer(
                onQRCodeDetected = { url ->
                    val webView: WebView = findViewById(R.id.web_view)
                    webView.loadUrl(url)
                },
                playSound = ::playSound,
                vibrate = ::vibrate,
                rebuildQueue = ::rebuildImageAnalysisQueueDepth
            )
        )

        try {
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, newImageAnalysis)
        } catch (exc: Exception) {
//          Log.e(TAG, "Use case binding failed", exc)
        }
    }

}
class QRCodeAnalyzer(
    private val onQRCodeDetected: (String) -> Unit,
    private val playSound: (Int) -> Unit,
    private val vibrate: () -> Unit,
    private val rebuildQueue: (Int) -> Unit,
) : ImageAnalysis.Analyzer {

    private var isCooldown = false
    private var frameCounter = 0
    private val reader = MultiFormatReader()

    companion object {
        private const val TAG = "QRCodeAnalyzer"
    }
    override fun analyze(image: ImageProxy) {
        if (isCooldown) {
            frameCounter++
            if (frameCounter >= 60) {  // reset after skipping 60 frames
                isCooldown = false
                frameCounter = 0
                rebuildQueue(3)
            }
            image.close()
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
            if (result != null) {
                onQRCodeDetected(result.text)
                playSound(R.raw.chip_mouseover2)
                vibrate()
                Log.d(TAG, "QR code detected: ${result.text}")  // debug
                isCooldown = true
                frameCounter = 0
                rebuildQueue(60)  // Set to a queue depth of 60
            }

        } catch (e: NotFoundException) {
            Log.e(TAG, "QR code not found", e)
            // No QR code found
        } finally {
            image.close()
        }
    }
}


