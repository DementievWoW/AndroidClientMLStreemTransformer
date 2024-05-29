package com.example.androidclientmlstreemtransformer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.io.File
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
                    val img = image.image
                    val bitmap = translator.translateYUV(img!!, this)
                    val size = bitmap.width * bitmap.height
                    val pixels = IntArray(size)

                    bitmap.getPixels(
                        pixels, 0, bitmap.width, 0, 0,
                        bitmap.width, bitmap.height
                    )


                    Log.e("bitmap", java.lang.StringBuilder().append(bitmap).toString())
                    Log.e("pixels", java.lang.StringBuilder().append(pixels).toString())
                    Log.e("bitmap.height", java.lang.StringBuilder().append(bitmap.height).toString())
                    Log.e("bitmap.width", java.lang.StringBuilder().append(bitmap.width).toString())
                    Log.e("pixels.size", java.lang.StringBuilder().append(pixels.size).toString())

                    var bitmapString = bitmap.toByteString()
                    var StringBitmap = bitmapString.toBitmap()


                    Log.e("bitmap.toByteString()", bitmap.toByteString())

                    webSocket.send(bitmapString)
//                    bitmap.setPixels(
//                        tmp3, 0, bitmap.width, 0, 0,
//                        bitmap.width, bitmap.height
//                    )
                    preview.rotation = image.imageInfo.rotationDegrees.toFloat()
                    preview.setImageBitmap(StringBitmap)
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
    private fun Bitmap.toByteString(): String {
        val baos = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }
    fun String.toBitmap(): Bitmap? {
        return try {
            val encodeByte: ByteArray =
                Base64.decode(this, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
        } catch (e: Exception) {
            e.message
            return null
        }
    }
    private fun toBytes(i: Int): ByteArray {
        val result = ByteArray(4)
        result[0] = (i shr 24).toByte()
        result[1] = (i shr 16).toByte()
        result[2] = (i shr 8).toByte()
        result[3] = i /*>> 0*/.toByte()
        return result
    }

//    private fun getUnsafeOkHttpClient(): OkHttpClient? {
//
//            // Create a trust manager that does not validate certificate chains
//            val trustAllCerts = arrayOf<TrustManager>(
//                object : X509TrustManager {
//                    @Throws(CertificateException::class)
//                    override fun checkClientTrusted(
//                        chain: Array<X509Certificate?>?,
//                        authType: String?
//                    ) {
//                    }
//
//                    @Throws(CertificateException::class)
//                    override fun checkServerTrusted(
//                        chain: Array<X509Certificate?>?,
//                        authType: String?
//                    ) {
//                    }
//
//                    override fun getAcceptedIssuers(): Array<X509Certificate?>? {
//                        return arrayOf()
//                    }
//                }
//            )

            // Install the all-trusting trust manager
//            val sslContext = SSLContext.getInstance("SSL")
//            sslContext.init(null, trustAllCerts, SecureRandom())
//            // Create an ssl socket factory with our all-trusting manager
//            val sslSocketFactory = sslContext.socketFactory
//            val builder = OkHttpClient.Builder()
//            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
//            builder.hostnameVerifier(HostnameVerifier { hostname, session -> true })
//
//            return  builder.build()
//    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
