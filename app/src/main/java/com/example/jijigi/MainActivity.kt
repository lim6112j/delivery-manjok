package com.example.jijigi

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import java.io.OutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
