package com.ucm.tfg.cinema

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ucm.tfg.cinema.Tests.CloudRecoActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arButton.setOnClickListener {
            val intent = Intent(this, CloudRecoActivity::class.java)
            startActivity(intent)
        }

        openGLButton.setOnClickListener {
            val intent = Intent(this, OpenglActivity::class.java)
            startActivity(intent)
        }
    }
}
