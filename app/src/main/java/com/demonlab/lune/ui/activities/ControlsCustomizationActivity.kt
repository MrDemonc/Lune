package com.demonlab.lune.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ControlsCustomizationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, PlayerCustomizationActivity::class.java))
        finish()
    }
}
