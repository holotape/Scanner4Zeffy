package com.example.scanner4zeffy

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import androidx.camera.view.PreviewView


class MainActivity : AppCompatActivity() {
    // ...lifecycle methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // Load classic XML layout
        // Init PreviewView
        val previewView: PreviewView = findViewById(R.id.preview_view)

        // Initialize WebView
        val webView: WebView = findViewById(R.id.web_view)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

        // Initialize Camera
        startCamera()
    }

    private fun startCamera() {
        // Setup image analysis to scan QR codes
        val imageAnalysis = ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeAnalyzer { url ->
                    // This will be called when a QR code is detected
                    val webView: WebView = findViewById(R.id.web_view)
                    webView.loadUrl(url)
                })
            }

    }
}
class QRCodeAnalyzer(private val onQRCodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()

    override fun analyze(image: ImageProxy) {
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
        } catch (e: NotFoundException) {
            // No QR code found
        } finally {
            image.close()
        }
    }
}