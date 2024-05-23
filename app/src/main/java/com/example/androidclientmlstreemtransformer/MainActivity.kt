package com.example.androidclientmlstreemtransformer

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import com.arhiser.photoappmin.YUVtoRGB
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CAMERA = 89045

    private lateinit var preview: ImageView

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    var translator = YUVtoRGB()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preview = findViewById(R.id.preview)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CAMERA
            )

        }
        else{
            initializeCamera();
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_CAMERA
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
            initializeCamera()
        }
    }
    private fun initializeCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                //Preview preview = new Preview.Builder().build();

                //ImageCapture imageCapture = new ImageCapture.Builder().build();
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1024, 768))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(this@MainActivity)
                ) { image ->
                    val img = image.image
                    val bitmap = translator.translateYUV(img!!, this@MainActivity)
//                    val size = bitmap.width * bitmap.height
//                    val pixels = IntArray(size)
//                    bitmap.getPixels(
//                        pixels, 0, bitmap.width, 0, 0,
//                        bitmap.width, bitmap.height
//                    )
//                    for (i in 0 until size) {
//                        val color = pixels[i]
//                        val r = color shr 16 and 0xff
//                        val g = color shr 8 and 0xff
//                        val b = color and 0xff
//                        val gray = (r + g + b) / 3
//                        pixels[i] = -0x1000000 or (gray shl 16) or (gray shl 8) or gray
//                    }
//                    bitmap.setPixels(
//                        pixels, 0, bitmap.width, 0, 0,
//                        bitmap.width, bitmap.height
//                    )
                    preview!!.rotation = image.imageInfo.rotationDegrees.toFloat()
                    preview!!.setImageBitmap(bitmap)
                    image.close()
                }
                cameraProvider.bindToLifecycle(this@MainActivity, cameraSelector, imageAnalysis)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
}