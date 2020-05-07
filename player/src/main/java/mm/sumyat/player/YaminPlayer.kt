package mm.sumyat.player

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.GestureDetectorCompat
import com.google.android.exoplayer2.ui.PlayerView
import mm.com.player.PlayerHelper

/**
 * YaminPlayer is view used to place in recyclerview item.
 *
 */
class YaminPlayer : FrameLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
    )

    private var gestureActionListener: GestureActionListener? = null

    var url: String? = ""
    var imageView: ImageView? = null
    var pauseOverlay: View? = null

    var isPause: Boolean = true
        set(value) {
            field = value
            val player = player()
            player?.actionPlayPause(value)
            hidePauseOverlay(isPause)
        }

    fun isPlaying(): Boolean {
        val view = player()
        if (view != null) {
            return view.isPlaying()
        }
        return false
    }

    fun stopPlayback() {
        player()?.stop()
        removePlayer()
    }

    fun player(): PlayerHelper? {
        if (playerView != null && playerView!!.tag != null && playerView!!.tag is YaminPlayerHelper) {
            val masterExoPlayerHelper = (playerView!!.tag as YaminPlayerHelper)
            return masterExoPlayerHelper.playerHelper
        }
        return null
    }

    fun duration(): Int {
        val view = player()
        if (view != null) return view.getDuration()
        return 0
    }

    fun currentPosition(): Int {
        val view = player()
        if (view != null) return view.getCurrentPosition()
        return 0
    }

    var playerView: PlayerView? = null

    fun addPlayer(playerView: PlayerView, autoPlay: Boolean) {
        if (this.playerView == null) {
            this.playerView = playerView
            addView(playerView)
            val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent?): Boolean {
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    isPause = !isPause
                    if (!isPause) gestureActionListener?.onPauseVideo()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    gestureActionListener?.onDoubleTap()
                    return false
                }
            })
            playerView.setOnTouchListener { v, event ->
                gestureDetector.onTouchEvent(event)
                return@setOnTouchListener false
            }

            // Progressbar will always be displayed
            playerView.controllerShowTimeoutMs = -1
            playerView.controllerHideOnTouch = false
        }
    }


    fun removePlayer() {
        if (playerView != null) {
            removeView(playerView)
            playerView = null
            imageView?.visibility = View.VISIBLE
            imageView?.animate()?.setDuration(500)?.alpha(1f)
        }
    }

    override fun removeView(view: View?) {
        super.removeView(view)
        if (view is PlayerView) {
            playerView = null
            imageView?.visibility = View.VISIBLE
            imageView?.animate()?.setDuration(500)?.alpha(1f)
        }
    }

    fun addGestureActionListener(listener: GestureActionListener) {
        this.gestureActionListener = listener
    }

    interface GestureActionListener {
        fun onPauseVideo()
        fun onDoubleTap()
    }

    fun hideThumbImage(thumbHideDelay: Long) {
        imageView?.animate()?.setStartDelay(thumbHideDelay)?.setDuration(0)?.alpha(0f)
    }

    fun hidePauseOverlay(isPause: Boolean) {
        if (isPause)
            pauseOverlay?.visibility = View.GONE
        else
            pauseOverlay?.visibility = View.VISIBLE
    }

    var listener: PlayerHelper.Listener? = null

}