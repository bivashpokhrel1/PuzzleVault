package com.jeiu.puzzlevault

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.roundToInt

/**
 * Implements drag-and-drop with smooth snap-to-grid animations.
 * Attach via view.setOnTouchListener(SnapToGridTouchListener(gridSizePx)).
 */
class SnapToGridTouchListener(
    private val gridSizePx: Int = 100,
    private val onSnapComplete: ((view: View, gridX: Int, gridY: Int) -> Unit)? = null
) : View.OnTouchListener {

    private var initialX = 0f
    private var initialY = 0f
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = view.x
                initialY = view.y
                offsetX = event.rawX - view.x
                offsetY = event.rawY - view.y
                view.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(100)
                    .start()
                view.parent.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - offsetX
                val dy = event.rawY - offsetY
                view.x = dx
                view.y = dy
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val targetX = (view.x / gridSizePx).roundToInt() * gridSizePx
                val targetY = (view.y / gridSizePx).roundToInt() * gridSizePx

                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 200
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { animator ->
                        val fraction = animator.animatedFraction
                        view.x = initialX + (targetX - initialX) * fraction
                        view.y = initialY + (targetY - initialY) * fraction
                    }
                }.start()

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()

                view.parent.requestDisallowInterceptTouchEvent(false)

                onSnapComplete?.invoke(view, targetX / gridSizePx, targetY / gridSizePx)
                return true
            }
        }
        return false
    }
}
