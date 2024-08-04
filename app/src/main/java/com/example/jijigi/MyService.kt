package com.example.jijigi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
import android.hardware.display.VirtualDisplay
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
    private val TAG = "MyService"
    private var resultCode = 0
    private var data: Intent? = null
    private var vdisplay: VirtualDisplay? = null
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
        Log.d(TAG, "MyService binding")
        return null
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "on start command")
        resultCode = intent!!.getIntExtra(EXTRA_RESULT_CODE, 1337)
        data = intent.getParcelableExtra(EXTRA_DATA)
        initRunningTipNotification()
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mMediaProjectionManager!!.getMediaProjection(resultCode, data!!)

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        screenShotUri = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).toString() + StringBuilder("/screenshot").append(".mp4").toString()
        mediaRecorder.setOutputFile(screenShotUri)
        mediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder.setVideoEncodingBitRate(512 * 1000)
        mediaRecorder.setVideoFrameRate(5)
        mediaRecorder.prepare()
        mediaProjectionCallback = object : Callback() {
            override fun onStop() {
                vdisplay!!.release()
            }
        }
        virtualDisplay=mediaProjection!!.createVirtualDisplay("andshooter",
            300, 300,
            getResources().getDisplayMetrics().densityDpi,
            VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.surface, null, null);
        mediaProjection!!.registerCallback(mediaProjectionCallback!!,  null )
        recordScreen()
        return super.onStartCommand(intent, flags, startId)
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
