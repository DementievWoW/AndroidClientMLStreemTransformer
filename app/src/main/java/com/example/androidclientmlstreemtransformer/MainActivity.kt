package com.example.androidclientmlstreemtransformer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutionException
import javax.net.ssl.*


class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CAMERA = 89045

    private lateinit var preview: ImageView

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val translator = YUVtoRGB()

//    private val okHttpClient = getUnsafeOkHttpClient()
    private val okHttpClient = OkHttpClient()

    private lateinit var wsListener : WebSocketListener

    private lateinit var webSocket : WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dexOutputDir: File = codeCacheDir
        dexOutputDir.setReadOnly()
        setContentView(R.layout.activity_main)

        wsListener = WebSocketListener()

//        webSocket = okHttpClient.newWebSocket(createRequest(bitmap.height, bitmap.width), wsListener)
        webSocket = okHttpClient.newWebSocket(createRequest(1,1), wsListener)
        Log.e("WS", ""+webSocket)

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
            initializeCamera()
        }
    }

    private fun createRequest(height : Int, width : Int): Request {
//            val wsUrl = "wss://free.blr2.piesocket.com/v3/1?api_key=Yd2mlnVXl5VFcIquYbqOvyt7ckkLoIi5nAy5F4Hq&notify_self=1"

//        val wsUrl = "ws://192.168.42.155:8000/ws"
        val wsUrl = "ws://192.168.0.45:8000/ws/?height=$height&width=$width"
//        val wsUrl = "ws://192.168.0.45:8000/ws/"
//        val wsUrl = "ws://10.0.2.2:8000/ws"
        return Request.Builder()
            .url(wsUrl)
            .build()
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
                    ContextCompat.getMainExecutor(this)
                ) { image ->
                    Log.e("4", "4")

                    val byteString = image.image?.toByteString()
                    if (byteString != null) {
                        webSocket.send(byteString)
                    }
                    byteString?.toBitmap()
//                    val bitmap = translator.translateYUV(img!!, this)
//                    val size = bitmap.rowBytes * bitmap.height
//                    val pixels = IntArray(size)
//
//                    bitmap.getPixels(
//                        pixels, 0, bitmap.width, 0, 0,
//                        bitmap.width, bitmap.height
//                    )
//
//
//                    Log.e("bitmap", java.lang.StringBuilder().append(bitmap).toString())
//                    Log.e("size", java.lang.StringBuilder().append(size).toString())
//                    Log.e("bitmap.height", java.lang.StringBuilder().append(bitmap.height).toString())
//                    Log.e("bitmap.width", java.lang.StringBuilder().append(bitmap.width).toString())
//                    Log.e("pixels.size", java.lang.StringBuilder().append(pixels.size).toString())
//
////                    var bitmapString = bitmap.toByteString()
////                    var stringBitmap = bitmapString.toBitmap()
//
////                    Log.e("stringBitmap", "$stringBitmap")
////                    webSocket.send(bitmap.toByteString())
//                    var bitmapString = bitmap.toByteString(size = size)
////                    webSocket.send(tmp)
//                    Log.e("bitmapString","$bitmapString")
//                    bitmapString.toBitmap(bitmap)
//                    Log.e("bool",wsListener._liveDataByteString.toString())
//                    Log.e("bool",bitmapString.toBitmap(bitmap).toString())
//                    Log.e("bool",wsListener._liveDataByteString?.toBitmap(bitmap).toString())
////                    Log.e("tmp",java.lang.StringBuilder().append(tmp).toString())
////                    Log.e("newBitmap",java.lang.StringBuilder().append(newBitmap).toString())
//
////                    Log.e("wsListener.liveData","")
////                    Log.e("StringBytes",java.lang.StringBuilder().append(byteString).toString())
//////                    var StringBitmap = wsListener.liveDataByteString.value?.toBitmap()
//
////                    bitmap.setPixels(
////                        tmp3, 0, bitmap.width, 0, 0,
////                        bitmap.width, bitmap.height
////                    )

                    preview.rotation = image.imageInfo.rotationDegrees.toFloat()
//                    preview.setImageBitmap(bitmap)

//                    preview.setImageBitmap(byteString?.toBitmap() ?: image.image?.toBitmap())
                    preview.setImageBitmap(byteString?.toBitmap())
                    image.close()
                }


                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))

    }

//    private fun Bitmap.toByteString(size: Int): ByteString {
//        Log.e("Bitmap.toByteString", "$size")
//        var buffer = ByteBuffer.allocate(size)
//        this.copyPixelsToBuffer(buffer)
//        Log.e("Bitmap.toByteString", "$buffer")
//        return buffer.toByteString()
////        val baos = ByteArrayOutputStream()
////        this.compress(Bitmap.CompressFormat.JPEG, 100, baos)
////        val b = baos.toByteArray()
////        return b.toByteString()
//    }


//    private fun Bitmap.toByteString(): String {
//        val baos = ByteArrayOutputStream()
//        this.compress(Bitmap.CompressFormat.JPEG, 40, baos)
//        val b = baos.toByteArray()
//        return Base64.encodeToString(b, Base64.DEFAULT)
//    }
private fun Image.toByteString(): ByteString? {
    val planes: Array<Image.Plane> = this.planes
    val yBuffer: ByteBuffer = planes[0].buffer
    val uBuffer: ByteBuffer = planes[1].buffer
    val vBuffer: ByteBuffer = planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    //U and V are swapped
    yBuffer[nv21, 0, ySize]
    vBuffer[nv21, ySize, vSize]
    uBuffer[nv21, ySize + vSize, uSize]
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
    val imageBytes = out.toByteArray()
    return imageBytes.toByteString()
}
    private fun Image.toBitmap(): Bitmap? {
        val planes: Array<Image.Plane> = this.planes
        val yBuffer: ByteBuffer = planes[0].buffer
        val uBuffer: ByteBuffer = planes[1].buffer
        val vBuffer: ByteBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    fun ByteString.toBitmap(bitmap : Bitmap): Boolean {
        return try {
//            this.toByteArray()
            var tmp = this.asByteBuffer()
            Log.e("String.toBitmap().this",java.lang.StringBuilder().append(this).toString())
            Log.e("String.toBitmap().byteBuffer",java.lang.StringBuilder().append(this).toString())
            bitmap.copyPixelsFromBuffer(tmp)
            true
//            val encodeByte: ByteArray =
//                Base64.decode(this, Base64.DEFAULT)
//            Log.e("String.toBitmap().encodeByte",java.lang.StringBuilder().append(encodeByte).toString())
        } catch (e: Exception) {
            Log.e("String.toBitmap()",java.lang.StringBuilder().append(e.message).toString())

            return false
        }
    }
    fun ByteString.toBitmap(): Bitmap? {
        return try {

            BitmapFactory.decodeByteArray(this.toByteArray(), 0, this.toByteArray().size)
        } catch (e: Exception) {
            Log.e("String.toBitmap()",java.lang.StringBuilder().append(e.message).toString())

            return null
        }
    }
//    private fun ByteString?.toBitmap(): Bitmap? {
//        return try {
//
//            var tmp = Base64.encode(this?.base64(), Base64.DEFAULT)
//            val encodeByte: ByteArray =
//
//                Base64.decode()
//            Log.e("this?.base64()",java.lang.StringBuilder().append(this?.base64()).toString())
//            BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
//        } catch (e: Exception) {
//            Log.e("ByteString?.toBitmap()",java.lang.StringBuilder().append(e.message).toString())
//            return null
//        }
//    }
    override fun onDestroy() {
        super.onDestroy()
    }
}



