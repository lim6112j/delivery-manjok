package com.example.jijigi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjection.Callback
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Handler
import android.os.IBinder
import android.util.Log
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random


class MyService : Service() {
    private lateinit var imageReader: ImageReader
    private val TAG = "MyService"
    private var resultCode = 0
    private var data: Intent? = null
    private var mediaProjection: MediaProjection? = null
    private var mMediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private val mediaRecorder: MediaRecorder = MediaRecorder()
    private var virtualDisplay: VirtualDisplay? = null
    private var screenShotUri: String? = null
    private var DISPLAY_WIDTH: Int = 720
    private var DISPLAY_HEIGHT: Int = 1280
    private val videoTime: Long = 5000
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "on start command")
        resultCode = intent!!.getIntExtra(EXTRA_RESULT_CODE, 1337);
        data = intent.getParcelableExtra(EXTRA_DATA);
        initRunningTipNotification()
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mMediaProjectionManager!!.getMediaProjection(resultCode, data!!)

        imageReader = ImageReader.newInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT, ImageFormat.RGB_565, 2)
        virtualDisplay=mediaProjection!!.createVirtualDisplay("andshooter",
            300, 300,
            getResources().getDisplayMetrics().densityDpi,
            VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.surface, null, null);
        mediaProjection!!.registerCallback(mediaProjectionCallback!!,  null )
        takeScreenShot()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun takeScreenShot() {
        val image = imageReader.acquireLatestImage()
        if( image != null) {
            Log.d(TAG, "image is not null")
        }
        else {
            Log.d(TAG, "image is null")
        }
    }

    private fun initRunningTipNotification() {
        val builder = Notification.Builder(this, "running")

        builder.setContentText("running notification")
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
    companion object {
        val EXTRA_RESULT_CODE = "resultcode"
        val EXTRA_DATA = "data"
        fun newIntent(context: Context, resultCode: Int, data: Intent): Intent {
            val intent = Intent(context, MyService::class.java)
            intent.putExtra(EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(EXTRA_DATA, data)
            return intent
        }
    }

    private fun recordScreen() {

            mediaRecorder.start()
            Handler().postDelayed(Runnable {
                mediaRecorder.stop()
                mediaRecorder.reset()
                stopRecordScreen()
                destroyMediaProjection()
                Handler().postDelayed(Runnable { getPathScreenShot(screenShotUri) }, 2000)
            }, videoTime)
    }
    private fun stopRecordScreen() {
        if (virtualDisplay == null) {
            virtualDisplay!!.release()
            if (mediaProjection != null) {
                destroyMediaProjection()
            }
            return
        }
    }
    private fun destroyMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection!!.unregisterCallback(mediaProjectionCallback!!)
            mediaProjection!!.stop()
            mediaProjection = null
        }
    }
    fun getPathScreenShot(filePath: String?) {
        val med: FFmpegMediaMetadataRetriever = FFmpegMediaMetadataRetriever()

        med.setDataSource(filePath)
        val bmp: Bitmap =
            med.getFrameAtTime(2 * 1000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST)
        val myPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
            .toString() + java.lang.StringBuilder("/screenshot").append(".bmp").toString()

        val myDir = File(myPath)
        myDir.mkdirs()
        val generator: Random = Random.Default
        var n = 10000
        n = generator.nextInt(n)
        val fname = "Image-$n.jpg"
        val file = File(myDir, fname)
        Log.i(TAG, "" + myDir)
        if (myDir.exists()) myDir.delete()
        try {
            val out = FileOutputStream(myDir)
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "on creating service")
    }
   /* override fun onCreate() {

        super.onCreate()
        val inflate = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams( *//*ViewGroup.LayoutParams.MATCH_PARENT*//*
            600,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER
        val mView: android.view.View = inflate.inflate(R.layout.view_in_service,   null)
        val bt = mView.findViewById(R.id.button) as Button
        bt.setOnClickListener {
            (mView.findViewById(R.id.textView) as TextView).text = "on click!!"
            val bitmap = screenShot(mView.rootView)
        }
        wm.addView(mView, params)
    }
    fun screenShot(view: View): Bitmap {
        resultCode = intent!!.getIntExtra(EXTRA_RESULT_CODE, 1);
        resultData = intent.getParcelableExtra(EXTRA_DATA);
        initRunningTipNotification()

        mediaProjection = getMediaProjection()!!
        window = FloatingWindow(this, this)
        window.drawFloatingWindow()
        val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        var mediaProjection : MediaProjection

*//*        val startMediaProjection = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                mediaProjection = mediaProjectionManager
                    .getMediaProjection(result.resultCode, result.data!!)
            }
        }

        startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())*//*
        return TODO("Provide the return value")
    }
    @SuppressLint("ForegroundServiceType")
    private fun initRunningTipNotification() {
        val builder = Notification.Builder(this, "running")

        builder.setContentText("running notification")
            .setSmallIcon(R.drawable.ic_launcher_foreground)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "running",
            "running notification",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        builder.setChannelId("running")
        startForeground(100, builder.build())
    }
    companion object {
        val EXTRA_RESULT_CODE = "resultcode"
        val EXTRA_DATA = "data"
        fun newIntent(context: Context, resultCode: Int, data: Intent): Intent {
            val intent = Intent(context, MyService::class.java)
            intent.putExtra(EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(EXTRA_DATA, data)
            return intent
        }
    }*/
}
