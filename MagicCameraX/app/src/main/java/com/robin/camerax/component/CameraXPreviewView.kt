package com.robin.camerax.component

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.camera.view.PreviewView

class CameraXPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : PreviewView(context, attrs, defStyleAttr, defStyleRes) {
    private val mGestureDetector: GestureDetector

    /**
     * 缩放相关
     */
    private var currentDistance = 0f
    private var lastDistance = 0f
    private val mScaleGestureDetector: ScaleGestureDetector

    /**
     * 缩放监听
     */
    interface CustomTouchListener {
        /**
         * 放大
         */
        fun zoom(delta: Float)

        /**
         * 点击
         */
        fun click(x: Float, y: Float)

        /**
         * 双击
         */
        fun doubleClick(x: Float, y: Float)

        /**
         * 长按
         */
        fun longClick(x: Float, y: Float)
    }

    private var mCustomTouchListener: CustomTouchListener? = null
    fun setCustomTouchListener(customTouchListener: CustomTouchListener?) {
        mCustomTouchListener = customTouchListener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleGestureDetector.onTouchEvent(event)
        if (!mScaleGestureDetector.isInProgress) {
            mGestureDetector.onTouchEvent(event)
        }
        return true
    }

    /**
     * 缩放监听
     */
    var onScaleGestureListener: OnScaleGestureListener = object : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val delta = detector.scaleFactor
            mCustomTouchListener?.zoom(delta)
            return true
        }
    }
    private var onGestureListener: SimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            mCustomTouchListener?.longClick(e.x, e.y)
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            currentDistance = 0f
            lastDistance = 0f
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            mCustomTouchListener?.click(e.x, e.y)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            mCustomTouchListener?.doubleClick(e.x, e.y)
            return true
        }
    }

    init {
        mGestureDetector = GestureDetector(context, onGestureListener)
        mScaleGestureDetector = ScaleGestureDetector(context, onScaleGestureListener)
    }
}