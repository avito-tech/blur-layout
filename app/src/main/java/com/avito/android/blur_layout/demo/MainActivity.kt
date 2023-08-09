package com.avito.android.blur_layout.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.avito.android.blur_layout.BlurLayout
import com.google.android.renderscript.Toolkit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BlurLayout.onApplyBlur = { bitmap, blurRadius ->
            Toolkit.blur(inputBitmap = bitmap, radius = blurRadius)
        }
    }
}