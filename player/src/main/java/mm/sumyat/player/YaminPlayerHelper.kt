package mm.sumyat.player

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import mm.com.player.PlayerHelper
import java.lang.RuntimeException

/**
 * MasterExoPlayerHelper lightweight utility for playing video using ExoPlayer inside RecyclerView,
 * With this you can set
 * @param id Id of MasterExoPlayer which is placed inside RecyclerView Item
 * @param playStrategy Used to decide when video will play, this will be value between 0 to 1, if 0.5 set means when view has 50% visibility it will start play. Default is PlayStrategy.DEFAULT i.e. 0.75
 * @param autoPlay Used to device we need to autoplay video or not., Default value is true
 * @param muteStrategy Used to decide whether mute one player affects other player also or not, values may be MuteStrategy.ALL, MuteStrategy.INDIVIDUAL, if individual user need to manage isMute flag with there own
 * @param defaultMute Used to decide whether player is mute by default or not, Default Value is false
 * @param loop Used whether need to play video in looping or not, if 0 then no looping will be there, Default is Int.MAX_VALUE
 */
class YaminPlayerHelper(
        mContext: Context,
        app_name: String,
        private val id: Int,
        private val autoPlay: Boolean = true,
        val thumbHideDelay: Long = 0,
        private val loop: Int = Int.MAX_VALUE
) {
    private val playerView: PlayerView
    val playerHelper: PlayerHelper

    init {
        val view: View = LayoutInflater.from(mContext).inflate(R.layout.custom_playerview, null, false)
        playerView = view.rootView as PlayerView
        playerView.controllerHideOnTouch = false
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        playerHelper = PlayerHelper(
                mContext = mContext,
                app_name = app_name,
                playerView = playerView,
                enableCache = true,
                loopVideo = loop > 0,
                loopCount = loop
        )
        playerHelper.setListener(false, object : PlayerHelper.Listener {
            override fun onStart() {
                super.onStart()
                playerView.getPlayerParent()?.hideThumbImage(thumbHideDelay)
                playerView.getPlayerParent()?.listener?.onStart()
            }

            override fun onBuffering(isBuffering: Boolean) {
                super.onBuffering(isBuffering)
                playerView.getPlayerParent()?.listener?.onBuffering(isBuffering)
            }

            override fun onError(error: ExoPlaybackException?) {
                super.onError(error)
                playerView.getPlayerParent()?.listener?.onError(error)
            }

            override fun onPlayerReady() {
                super.onPlayerReady()
                playerView.getPlayerParent()?.listener?.onPlayerReady()
            }

            override fun onProgress(positionMs: Long) {
                super.onProgress(positionMs)
                playerView.getPlayerParent()?.listener?.onProgress(positionMs)
            }

            override fun onStop() {
                super.onStop()
                playerView.getPlayerParent()?.listener?.onStop()
            }

            override fun onToggleControllerVisible(isVisible: Boolean) {
                super.onToggleControllerVisible(isVisible)
                playerView.getPlayerParent()?.listener?.onToggleControllerVisible(isVisible)
            }
        })
        playerView.tag = this

    }

    /**
     * Make this helper lifecycler aware so it will stop player when activity goes to background.
     */
    fun makeLifeCycleAware(activity: Fragment) {
        playerHelper.makeLifeCycleAware(activity)
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        var firstVisibleItem: Int = 0
        var lastVisibleItem: Int = 0
        var visibleCount: Int = 0

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> {
                    for (i in 0 until visibleCount) {
                        val view = recyclerView.getChildAt(i) ?: continue
                        val masterExoPlayer = view.findViewById<View>(id)
                        if (masterExoPlayer != null && masterExoPlayer is YaminPlayer) {
                            play(view)
                        } else {
                            playerHelper.stop()
                            playerView.getPlayerParent()?.removePlayer()
                        }
                        break
                    }
                }
            }
            super.onScrollStateChanged(recyclerView, newState)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            firstVisibleItem = layoutManager?.findFirstVisibleItemPosition() ?: 0;
            lastVisibleItem = layoutManager?.findLastVisibleItemPosition() ?: 0;
            visibleCount = (lastVisibleItem - firstVisibleItem) + 1;

            if (dx == 0 && dy == 0) {
                play(recyclerView.getChildAt(0))
            }
        }
    }

    private val onChildAttachStateChangeListener = object : RecyclerView.OnChildAttachStateChangeListener {
        override fun onChildViewDetachedFromWindow(view: View) {
            releasePlayer(view)
        }

        override fun onChildViewAttachedToWindow(view: View) {
        }
    }

    /**
     * Used to attach this helper to recycler view. make call to this after setting LayoutManager to your recycler view
     */
    fun attachToRecyclerView(recyclerView: RecyclerView) {
        if (recyclerView.layoutManager != null) {
            recyclerView.removeOnScrollListener(onScrollListener)
            recyclerView.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener)

            recyclerView.addOnScrollListener(onScrollListener)
            recyclerView.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)
        } else {
            throw(RuntimeException("call attachToRecyclerView() after setting RecyclerView.layoutManager"))
        }
    }

    fun clearCahce() {
        playerHelper.removeCache()
    }

    fun play(view: View) {
        val yaminPlayer = view.findViewById<View>(id)
        if (yaminPlayer != null && yaminPlayer is YaminPlayer) {
            if (yaminPlayer.playerView == null) {
                playerView.getPlayerParent()?.removePlayer()
                yaminPlayer.addPlayer(playerView, autoPlay)
                if (yaminPlayer.url?.isNotBlank() == true) {
                    playerHelper.setUrl(yaminPlayer.url!!, autoPlay)
                }
            }
        }
    }

    private fun releasePlayer(view: View) {
        val yaminPlayer = view.findViewById<View>(id)
        if (yaminPlayer != null && yaminPlayer is YaminPlayer) {
            if (yaminPlayer.playerView != null) {
                playerHelper.stop()
                yaminPlayer.removePlayer()
            }
        }
    }

    private fun PlayerView.getPlayerParent(): YaminPlayer? {
        if (this.parent != null && this.parent is YaminPlayer) {
            return this.parent as YaminPlayer
        }
        return null
    }

}