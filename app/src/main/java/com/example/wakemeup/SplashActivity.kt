package com.example.wakemeup

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    var progressStatus = 0
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val progressBar = findViewById<ProgressBar>(R.id.determinateBar)

        Thread(Runnable {
            while (progressStatus < 100) {
                progressStatus++
                android.os.SystemClock.sleep(20)
                handler.post { progressBar.setProgress(progressStatus) }
            }
            if (progressStatus === 100) {
                val intent = Intent(this@SplashActivity, MapActivity::class.java)
                startActivity(intent)
                finish()
            }
        }).start()

    }
}