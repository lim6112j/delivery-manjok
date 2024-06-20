package com.example.jijigi
import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.jijigi.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE: Int = 5469
    private lateinit var binding: ActivityMainBinding

    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // TODO 동의를 얻지 못했을 경우의 처리
            } else {
                startService(Intent(this@MainActivity, MyService::class.java))
            }
        }
    }
    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 체크
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            } else {
                startService(Intent(this@MainActivity, MyService::class.java))
            }
        } else {
            startService(Intent(this@MainActivity, MyService::class.java))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bt_start = findViewById<Button>(R.id.bt_start) as Button
        bt_start.setOnClickListener {
            checkPermission()
            //startService(new Intent(MainActivity.this, AlwaysOnTopService.class));
        }

        val bt_stop = findViewById<Button>(R.id.bt_stop) as Button
        bt_stop.setOnClickListener {
            stopService(
                Intent(
                    this@MainActivity,
                    MyService::class.java
                )
            )
        }
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        ocr()
    }
    private fun ocr() {
        val datapath = "${applicationContext.filesDir}/tesseract/"
        val listOfImagePath = assets.list("images")
        if (listOfImagePath != null) {
            for (imagePath in listOfImagePath.iterator()) {
                val testImagePath = "images/${imagePath}"
                val testImageFile = File("$datapath/${testImagePath}")
                if(!testImageFile.exists()) {
                    try {
                        val dir = File("$datapath/images/")
                        if(!dir.exists()) {
                            dir.mkdirs()
                        }
                        val inputStream = applicationContext.assets.open(testImagePath)
                        val outputStream = FileOutputStream(testImageFile)
                        inputStream.copyTo(outputStream)
                        inputStream.close()
                        outputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                ocrImage(testImageFile)
            }
        }

    }
    private fun ocrImage(imageFile: File): String {
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
        baseApi.setImage(imageFile)
        val recognizedText = baseApi.utF8Text
        baseApi.end()
        Log.d("OCR Result", recognizedText)
        return recognizedText
    }
}
