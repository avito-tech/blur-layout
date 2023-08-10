package com.avito.android.blur_layout.demo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.avito.android.blur_layout.BlurLayout
import com.google.android.renderscript.Toolkit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BlurLayout.init(
            onApplyBlur = { bitmap, blurRadius ->
                Toolkit.blur(inputBitmap = bitmap, radius = blurRadius)
            },
        )

        val buttons = mutableListOf<View>()

        fun hideButtons() {
            buttons.forEach { it.isVisible = false }
        }

        val exampleRocks = findViewById<View>(R.id.example_rocks)
        val exampleFruits = findViewById<View>(R.id.example_fruits)

        findViewById<View>(R.id.button_rocks)
            .apply(buttons::add)
            .setOnClickListener {
                hideButtons()
                exampleRocks.isVisible = true
            }

        findViewById<View>(R.id.button_fruits)
            .apply(buttons::add)
            .setOnClickListener {
                hideButtons()
                exampleFruits.isVisible = true
            }
    }
}