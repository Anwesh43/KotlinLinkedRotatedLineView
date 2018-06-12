package com.anwesh.uiprojects.linkedrotatedlineview

/**
 * Created by anweshmishra on 12/06/18.
 */

import android.app.Activity
import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.graphics.*

private val LRL_NODES = 5
class LinkedRotatedLineView (ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var prevScale : Float = 0f, var dir : Float = 0f, var j : Int = 0) {

        val scales : Array<Float> = arrayOf(0f, 0f, 0f)

        fun update(stopcb : (Float) -> Unit) {
            scales[j] += 0.1f * dir
            if (Math.abs(scales[j] - prevScale) > 1) {
                scales[j] = prevScale + dir
                j += dir.toInt()
                if (j == scales.size || j == -1) {
                    j -= dir.toInt()
                    dir = 0f
                    prevScale = scales[j]
                    stopcb(prevScale)
                }

            }
        }

        fun startUpdating(startcb : () -> Unit) {
            if (dir == 0f) {
                dir = 1 - 2 * prevScale
                startcb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class RotatedLineNode(var i : Int, val state : State = State()) {

        var next : RotatedLineNode? = null

        var prev : RotatedLineNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if(i < LRL_NODES - 1) {
                next = RotatedLineNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            val w : Float = canvas.width.toFloat()
            val h : Float = canvas.height.toFloat()
            val gap : Float = w / LRL_NODES
            val k : Int = (i + 1) % 2
            prev?.draw(canvas, paint)
            paint.color = Color.WHITE
            paint.strokeWidth = Math.min(w, h) / 60
            paint.strokeCap = Paint.Cap.ROUND
            canvas.save()
            canvas.translate(i * gap - gap + gap * state.scales[0], h/2)
            canvas.rotate(90f * (1 - k) + 90f * (2 * k - 1) * state.scales[1])
            canvas.drawLine(0f, -gap/2, 0f, gap/2, paint)
            canvas.restore()
        }

        fun update(stopcb : (Float) -> Unit) {
            state.update(stopcb)
        }

        fun startUpdating(startcb : () -> Unit) {
            state.startUpdating(startcb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : RotatedLineNode {
            var curr : RotatedLineNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LinkedRotatedLine(var i : Int) {

        private var curr : RotatedLineNode = RotatedLineNode(0)

        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(stopcb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                stopcb(it)
            }
        }

        fun startUpdating(startcb : () -> Unit) {
            curr.startUpdating(startcb)
        }
    }

    data class Renderer(var view : LinkedRotatedLineView) {

        private val animator : Animator = Animator(view)

        private val linkedRotatedLine : LinkedRotatedLine = LinkedRotatedLine(0)

        fun render(canvas : Canvas, paint : Paint) {
            linkedRotatedLine.draw(canvas, paint)
            animator.animate {
                linkedRotatedLine?.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            linkedRotatedLine.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) {
            val view : LinkedRotatedLineView = LinkedRotatedLineView(activity)
            activity.setContentView(view)
        }
    }
}