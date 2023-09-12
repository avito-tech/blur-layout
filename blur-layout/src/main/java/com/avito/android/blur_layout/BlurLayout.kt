package com.avito.android.blur_layout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.Px
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.withTranslation
import androidx.core.view.children
import androidx.core.view.doOnLayout
import com.avito.android.blur_layout.BlurLayout.Companion.onApplyBlur

/**
 * Helper container that is able to blur background for a [targetChild] taking into account
 * all the views behind the [targetChild].
 *
 * The problem without using this container is that when you set the blur effect to a certain view's background,
 * this blur effect doesn't blur anything that is behind this view – which is not an expected design pattern when you
 * use semitransparent background.
 *
 * The main logic of this container is that it creates a bitmap, draws all the views behind the [targetChild],
 * then draws the [targetChild]'s background, blurs that bitmap via [onApplyBlur],
 * crops it to the size of [targetChild] and sets this bitmap as the [targetChild]'s background.
 *
 * Note:
 * 1. All the views that must be accounted when applying blur should be included as the children of this container.
 * 2. [targetChild]'s background should be set before applying blur with this container, as it uses the
 * preset background as the base for the blurred background.
 */
class BlurLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    /** Use this provider with custom views in your project */
    fun interface CornerRadiusProvider {
        fun provideCornerRadius(): Float
    }

    /** Use this provider with custom views in your project */
    fun interface BlurRadiusProvider {
        fun provideBlurRadius(): Int
    }

    companion object {
        private lateinit var onApplyBlur: (bitmap: Bitmap, blurRadius: Int) -> Bitmap

        /**
         * @param onApplyBlur Transformer to apply blur effect to a bitmap with certain blur radius.
         * External blur effect applier is used as there are several ways to apply blur to a bitmap
         * (like the Google's lib com.google.android.renderscript.Toolkit).
         */
        fun init(
            onApplyBlur: (bitmap: Bitmap, blurRadius: Int) -> Bitmap,
        ) {
            this.onApplyBlur = onApplyBlur
        }
    }

    private var targetChildId: Int? = null
    private lateinit var targetChild: View

    private var blurRadius: Int? = null
        get() = (targetChild as? BlurRadiusProvider)?.provideBlurRadius() ?: field

    @Px
    private var targetChildBackgroundCornerRadius: Float = 0f
        get() = (targetChild as? CornerRadiusProvider)?.provideCornerRadius() ?: field

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.BlurLayout)
        targetChildId = array.getResourceId(
            R.styleable.BlurLayout_blurLayout_targetChildId,
            0
        ).takeIf { it != 0 }
        blurRadius = array.getInt(
            R.styleable.BlurLayout_blurLayout_blurRadius,
            -1
        ).takeIf { it != -1 }
        targetChildBackgroundCornerRadius = array.getDimension(
            R.styleable.BlurLayout_blurLayout_targetChildBackgroundCornerRadius,
            0f
        )
        array.recycle()

        applyBlurIfSingleChild()
    }

    /**
     * @param targetChild view to apply background blur to. Can be omitted if this container has single child.
     */
    fun setBlurredBackgroundForTargetChild(
        targetChild: View? = null,
        blurRadius: Int? = null,
        @Px targetChildBackgroundCornerRadius: Float = 0f,
    ) {
        this.targetChild = targetChild ?: children.singleOrNull()
                ?: error("targetChild can be omitted only when there's a single child in BlurLayout")
        this.blurRadius = blurRadius
        this.targetChildBackgroundCornerRadius = targetChildBackgroundCornerRadius

        applyBlur()
    }

    fun setBlurredBackgroundForTargetChild(
        @IdRes targetChildId: Int,
        blurRadius: Int? = null,
        @Px targetChildBackgroundCornerRadius: Float = 0f,
    ) {
        setBlurredBackgroundForTargetChild(
            targetChild = findViewById(targetChildId),
            blurRadius = blurRadius,
            targetChildBackgroundCornerRadius = targetChildBackgroundCornerRadius
        )
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)

        // This logic is used to apply blur when targetChildId is specified via XML.
        if (child.id == targetChildId) {
            targetChild = child
            applyBlur()
        }
    }

    private fun applyBlurIfSingleChild() {
        doOnLayout {
            val singleChild = children.singleOrNull()
            if (singleChild != null && !this::targetChild.isInitialized) {
                targetChild = singleChild
                applyBlur()
            }
        }
    }

    private fun applyBlur() = targetChild.doOnLayout { targetChild ->
        val blurRadius = blurRadius
            ?: error("Blur radius must be specified explicitly or should be provided by child implementing BlurRadiusProvider")

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw BlurLayout's background first
        background?.apply {
            setBounds(0, 0, width, height)
        }?.draw(canvas)

        for (child in children) {
            val isTargetChild = child == targetChild
            canvas.withTranslation(child.left.toFloat(), child.top.toFloat()) {
                if (!isTargetChild) {
                    // Draw all the views that are behind targetView
                    child.draw(canvas)
                } else {
                    // Draw background only for the targetChild to avoid targetChild's content blurring
                    targetChild.background?.apply {
                        setBounds(0, 0, targetChild.width, targetChild.height)
                    }?.draw(canvas)
                }
            }
            if (isTargetChild) break // Don't take into account views that are in front of targetChild
        }

        val croppedBitmap =
            Bitmap.createBitmap(
                bitmap,
                targetChild.left,
                targetChild.top,
                targetChild.width,
                targetChild.height
            )
        bitmap.recycle()
        val blurredBackgroundBitmap = onApplyBlur(croppedBitmap, blurRadius)
        croppedBitmap.recycle()

        targetChild.background = if (targetChildBackgroundCornerRadius > 0) {
            RoundedBitmapDrawableFactory.create(context.resources, blurredBackgroundBitmap).apply {
                cornerRadius = targetChildBackgroundCornerRadius
            }
        } else {
            BitmapDrawable(context.resources, blurredBackgroundBitmap)
        }
    }
}
