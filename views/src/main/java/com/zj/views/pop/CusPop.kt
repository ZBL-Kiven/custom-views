package com.zj.views.pop

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.annotation.AnimRes
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.zj.views.R

@SuppressLint("ResourceType")
@Suppress("unused", "MemberVisibilityCanBePrivate", "InflateParams")
class CusPop private constructor(private val popConfig: PopConfig) : PopupWindow(popConfig.v, popConfig.w, popConfig.h) {

    companion object {
        fun create(v: View): PopConfig {
            return PopConfig(v)
        }
    }

    private var rootView: View
    private var vParent: FrameLayout
    private var vAnim: ValueAnimator? = null

    override fun dismiss() {
        val animE = popConfig.animInRes
        if (animE != 0) {
            val animOut = AnimationUtils.loadAnimation(popConfig.getContext(), popConfig.animOutRes)
            (rootView as? ViewGroup)?.getChildAt(0)?.startAnimation(animOut)
            withAnim(false, animOut, popConfig.dimColor)
            animOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    superDisMiss()
                }

                override fun onAnimationStart(animation: Animation?) {
                    contentView.isEnabled = false
                }
            })
        } else {
            setBgColor(true, 0.0f, popConfig.dimColor)
            superDisMiss()
        }
    }

    fun show(init: (root: View, pop: CusPop) -> Unit) {
        this.show(Gravity.NO_GRAVITY, init = init)
    }

    fun show(showGravity: Int, init: (root: View, pop: CusPop) -> Unit) {
        try {
            startAnim()
            showAtLocation(popConfig.v, showGravity, popConfig.xOffset, popConfig.yOffset)
        } catch (e: Exception) {
            Log.e("CusPop.show", "unable to show cus pop view , the error case: ${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("CusPop.show", "unable to show cus pop view , the error case: ${e.message}")
        }
        @Suppress("LeakingThis") init(rootView, this)
    }

    fun showIn(showGravity: Int, init: (root: View, pop: CusPop) -> Unit) {
        try {
            startAnim()
            showAsDropDown(popConfig.v, popConfig.xOffset, popConfig.yOffset, showGravity)
        } catch (e: Exception) {
            Log.e("CusPop.show", "unable to show cus pop view , the error case: ${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("CusPop.show", "unable to show cus pop view , the error case: ${e.message}")
        }
        @Suppress("LeakingThis") init(rootView, this)
    }

    fun superDisMiss() {
        try {
            super@CusPop.dismiss()
        } catch (e: Exception) {
            Log.e("CusPop.show", "unable to show cus pop view , the error case: ${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("CusPop.show", "unable to show cus pop view , the error case: ${e.message}")
        }
    }

    init {
        val context = popConfig.getContext()
        isOutsideTouchable = popConfig.outsideTouchAble
        isFocusable = popConfig.focusAble
        isClippingEnabled = false
        vParent = LayoutInflater.from(context).inflate(R.layout.cus_pop_parent, null, false) as FrameLayout
        contentView = vParent
        contentView.setPadding(0, 0, 0, 0)
        vParent.removeAllViews()
        rootView = inflate(context, popConfig.contentId, vParent)
        vParent.setBackgroundColor(Color.TRANSPARENT)
        initView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        if (!isOutsideTouchable) vParent.setOnTouchListener { _, _ ->
            return@setOnTouchListener if (isOutsideTouchable) {
                if (popConfig.outsideTouchDismiss) dismiss()
                false
            } else true
        }
    }

    enum class DimMode {
        CONTENT, FULL_STATUS, FULL_SCREEN, NONE
    }

    private fun startAnim() {
        val animIn = popConfig.animInRes
        if (animIn != 0) {
            val animEnter = AnimationUtils.loadAnimation(popConfig.getContext(), animIn)
            (rootView as? ViewGroup)?.getChildAt(0)?.startAnimation(animEnter)
            withAnim(true, animEnter, popConfig.dimColor)
        } else {
            setBgColor(true, 1.0f, popConfig.dimColor)
        }
    }

    private fun withAnim(show: Boolean, anim: Animation, targetColor: Int) {
        vAnim?.end()
        vAnim?.cancel()
        vAnim = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(anim.duration)
        vAnim?.addUpdateListener {
            setBgColor(show, it.animatedFraction, targetColor)
        }
        vAnim?.start()
    }

    private fun setBgColor(show: Boolean, @FloatRange(from = 0.0, to = 1.0) fraction: Float, target: Int) {
        val ce = ArgbEvaluator()
        val f = if (show) fraction else 1.0f - fraction
        val color = ce.evaluate(f, Color.TRANSPARENT, target) as Int
        vParent.setBackgroundColor(color)
    }

    class PopConfig(val v: View) {
        internal var w = 0; private set
        internal var h = 0; private set
        internal var xOffset = 0; private set
        internal var yOffset = 0; private set
        private var dimMode: DimMode = DimMode.FULL_STATUS
        internal var dimColor: Int = Color.parseColor("#50000000"); private set
        internal var animInRes: Int = R.anim.cus_pop_animation_in; private set
        internal var animOutRes: Int = R.anim.cus_pop_animation_out; private set
        internal var contentId: Int = -1; private set
        internal var focusAble = true; private set
        internal var outsideTouchAble = false; private set
        internal var outsideTouchDismiss = true; private set

        fun dimMode(m: DimMode): PopConfig {
            this.dimMode = m
            return this
        }

        fun dimColor(colorString: String): PopConfig {
            this.dimColor = Color.parseColor(colorString)
            return this
        }

        fun dimColor(@ColorRes colorRes: Int): PopConfig {
            this.dimColor = ContextCompat.getColor(getContext(), colorRes)
            return this
        }

        fun animStyleRes(@AnimRes animIn: Int, @AnimRes animOut: Int): PopConfig {
            this.animInRes = animIn
            this.animOutRes = animOut
            return this
        }

        fun contentId(@LayoutRes contentId: Int): PopConfig {
            this.contentId = contentId
            return this
        }

        fun focusAble(focusAble: Boolean): PopConfig {
            this.focusAble = focusAble
            return this
        }

        fun outsideTouchAble(touchAble: Boolean): PopConfig {
            this.outsideTouchAble = touchAble
            return this
        }

        fun outsideTouchDismiss(dismiss: Boolean): PopConfig {
            this.outsideTouchDismiss = dismiss
            return this
        }

        fun offset(x: Int, y: Int): PopConfig {
            this.xOffset = x
            this.yOffset = y
            return this
        }

        internal fun getContext(): Context {
            return v.context
        }

        fun instance(): CusPop {
            val act = (getContext() as? Activity)
            val p: Point
            if (act != null) {
                p = when (dimMode) {
                    DimMode.CONTENT -> {
                        val r = Rect()
                        v.getGlobalVisibleRect(r)
                        Point(r.right, r.bottom)
                    }
                    DimMode.FULL_STATUS -> {
                        val r = Rect()
                        act.window.decorView.getWindowVisibleDisplayFrame(r)
                        Point(r.right, r.bottom)
                    }
                    DimMode.FULL_SCREEN -> {
                        val dm = DisplayMetrics()
                        act.windowManager.defaultDisplay.getRealMetrics(dm)
                        Point(dm.widthPixels, dm.heightPixels)
                    }
                    DimMode.NONE -> {
                        Point(-2, -2)
                    }
                }
            } else {
                val dm = DisplayMetrics()
                val wm = getContext().getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.defaultDisplay?.getRealMetrics(dm)
                p = Point(dm.widthPixels, dm.heightPixels)
            }
            w = p.x
            h = p.y
            return CusPop(this)
        }

        fun show(init: (root: View, pop: CusPop) -> Unit) {
            this.show(Gravity.NO_GRAVITY, init = init)
        }

        fun show(showGravity: Int, init: (root: View, pop: CusPop) -> Unit) {
            instance().show(showGravity, init = init)
        }

        fun showIn(init: (root: View, pop: CusPop) -> Unit) {
            this.showIn(Gravity.NO_GRAVITY, init = init)
        }

        fun showIn(showGravity: Int, init: (root: View, pop: CusPop) -> Unit) {
            instance().showIn(showGravity, init = init)
        }
    }
}