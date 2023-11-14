package com.sdbk.volumechanger.base

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.AppCompatButton
import com.sdbk.volumechanger.R

class ScaleButton: AppCompatButton {
    private lateinit var scaleAnim: Animation

    constructor(context: Context): super(context) { initAnim() }
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) { initAnim() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr) { initAnim() }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event?.action == MotionEvent.ACTION_UP) startAnimation(scaleAnim)
        return super.onTouchEvent(event)
    }

    private fun initAnim() {
        scaleAnim = AnimationUtils.loadAnimation(context.applicationContext, R.anim.anim_scale)
    }
}