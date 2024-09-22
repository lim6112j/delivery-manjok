package com.example.jijigi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.media.Image.Plane
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjection.Callback
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MyService : Service() {
    private lateinit var mHandler: Handler
    private val TAG = "MyService"
    private var resultCode=0
    private var data: Intent? = null
    private  lateinit var mStoreDir: String
    private var vdisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null
    private var mMediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var virtualDisplayCallback: VirtualDisplay.Callback? = null
    private var screenShotUri: String? = null
    private var DISPLAY_WIDTH: Int = 720
    private var DISPLAY_HEIGHT: Int = 1280
    private val videoTime: Long = 5000
    private lateinit var imageReader: ImageReader
    private var previousImage: Bitmap? = null
    companion object {
        val EXTRA_RESULT_CODE = "resultCode"
        val EXTRA_DATA = "data"
        var IMAGES_PRODUCED = 0

        fun newIntent(context: Context, resultCode: Int, data: Intent): Intent {
            val intent = Intent(context, MyService::class.java)
            intent.putExtra(EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(EXTRA_DATA, data)
            return intent
        }
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    private inner class ImageAvailableListener : OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null
            val mWidth = DISPLAY_WIDTH
            val mHeight = DISPLAY_HEIGHT
            try {
                reader.acquireLatestImage().use { image ->
                    if (image != null) {
                        Log.e(TAG, "captured image: $IMAGES_PRODUCED")
                        val planes: Array<Plane> = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding: Int = rowStride - pixelStride * mWidth

                        // create bitmap
                        bitmap = Bitmap.createBitmap(
                            mWidth + rowPadding / pixelStride,
                            mHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        bitmap!!.copyPixelsFromBuffer(buffer)


                     //   var  ocrText = ocrImage(bitmap!!)
//                             write bitmap to a file
                        val filename= (mStoreDir + "/myscreen_" + IMAGES_PRODUCED).toString() + ".png"
                          fos =
                                FileOutputStream(filename)
                            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fos!!)
                        var ocrText = ocrImage(File(filename))
                        Log.e(TAG, "### ocr_text : $ocrText")

                        IMAGES_PRODUCED++
                        Log.e(TAG, "captured image: $IMAGES_PRODUCED")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (fos != null) {
                    try {
                        fos!!.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }
                }

                if (bitmap != null) {
                    bitmap!!.recycle()
                }
            }
        }
    }

    private fun ocrImage(bitmap: File): String {
        val baseApi = TessBaseAPI()
        val datapath = "${applicationContext.filesDir}/tesseract/"
        val lang = arrayOf("kor", "eng")
        val trainedDataPaths = lang.map{"$it.traineddata"}
        for (trainedDataPath in trainedDataPaths) {
            val trainedDataFile = File("$datapath/tessdata/$trainedDataPath")
            if (!trainedDataFile.exists()) {
                try {
                    val dir = File("$datapath/tessdata/")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val inputStream = applicationContext.assets.open(trainedDataPath)
                    val outputStream = FileOutputStream(trainedDataFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        baseApi.init(datapath, lang.joinToString("+") )
        baseApi.setImage(bitmap)
        val recognizedText = baseApi.utF8Text
        baseApi.end()
        Log.d("OCR Result", recognizedText)
        return recognizedText
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resultCode = intent!!.getIntExtra(EXTRA_RESULT_CODE, 1337)
        data = intent.getParcelableExtra(EXTRA_DATA)
        Log.d(TAG, "on start Command with resultCode ${resultCode}")
        initRunningTipNotification()
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mMediaProjectionManager!!.getMediaProjection(resultCode, data!!)
        imageReader = ImageReader.newInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT, PixelFormat.RGBA_8888, 2)
        mediaProjectionCallback = object : Callback() {
            override fun onStop() {
                Log.d(TAG, "mediaprojectioncallback stop called!")
                virtualDisplay!!.release()
                super.onStop()
            }
        }

        virtualDisplayCallback = object : VirtualDisplay.Callback() {
            override fun onPaused() {
                super.onPaused()
            }

            override fun onResumed() {
                super.onResumed()
            }

            override fun onStopped() {
                super.onStopped()
            }
        }

        mediaProjection!!.registerCallback(mediaProjectionCallback!!, mHandler)
        virtualDisplay = mediaProjection!!.createVirtualDisplay("andshooter",
            DISPLAY_WIDTH, DISPLAY_HEIGHT,
            resources.displayMetrics.densityDpi,
            flags,imageReader.surface, virtualDisplayCallback, mHandler
        )

        getScreenshot(imageReader)
        return super.onStartCommand(intent, flags, startId)
    }


    private fun getScreenshot(imageReader: ImageReader) {
        Log.d(TAG, "Starting take screenshot")
        imageReader.setOnImageAvailableListener(ImageAvailableListener(), mHandler)
    }

    private fun initRunningTipNotification() {
        val builder = Notification.Builder(this, "running")
        builder.setContentText("running application")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "running",
            "running notification",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        builder.setChannelId("running")
        startForeground(100, builder.build())
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MyService OnCreated")
        DISPLAY_WIDTH = resources.displayMetrics.widthPixels
        DISPLAY_HEIGHT = resources.displayMetrics.heightPixels
        val inflate = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val externalFilesDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (externalFilesDir != null) {
            mStoreDir = externalFilesDir.absolutePath + "/screenshots/"
            val storeDirectory = File(mStoreDir)
            if (!storeDirectory.exists()) {
                val success = storeDirectory.mkdirs()
                if (!success) {
                    Log.e(TAG, "failed to create file storage directory.")
                    stopSelf()
                }
            }
        } else {
            Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.")
            stopSelf()
        }
        val params = WindowManager.LayoutParams( /*ViewGroup.LayoutParams.MATCH_PARENT*/
            wm.defaultDisplay.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.LEFT or Gravity.TOP
        val mView: android.view.View = inflate.inflate(R.layout.view_in_service,   null)
        val bt = mView.findViewById(R.id.button) as Button
        bt.setOnClickListener {
            (mView.findViewById(R.id.textView) as TextView) .text = "on click!!"
        }
        wm.addView(mView, params)
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()
    }
}
