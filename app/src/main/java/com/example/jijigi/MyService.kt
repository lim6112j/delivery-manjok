package com.example.jijigi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjection.Callback
import android.media.projection.MediaProjectionManager
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


class MyService : Service() {
    private lateinit var mHandler: Handler
    private val TAG = "MyService"
    private var resultCode=0
    private var data: Intent? = null
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
    companion object {
        val EXTRA_RESULT_CODE = "resultCode"
        val EXTRA_DATA = "data"
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resultCode = intent!!.getIntExtra(EXTRA_RESULT_CODE, 1337)
        data = intent.getParcelableExtra(EXTRA_DATA)
        Log.d(TAG, "on start Command with resultCode ${resultCode}")
        initRunningTipNotification()
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mMediaProjectionManager!!.getMediaProjection(resultCode, data!!)
        imageReader = ImageReader.newInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT,ImageFormat.JPEG, 2)
        mediaProjectionCallback = object : Callback() {
            override fun onStop() {
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

        getScreenshot()
        return super.onStartCommand(intent, flags, startId)
    }


    private fun getScreenshot() {
        Log.d(TAG, "Starting take screenshot")
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
        val inflate = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

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
