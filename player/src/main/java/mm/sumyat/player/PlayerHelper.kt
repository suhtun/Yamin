package mm.com.player

import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import java.io.File

class PlayerHelper(val mContext: Context,
                   val app_name:String,
                   private val playerView: PlayerView,
                   enableCache: Boolean = true,
                   private val loopVideo: Boolean = false,
                   val loopCount: Int = Integer.MAX_VALUE) :
    LifecycleObserver {

    private var mPlayer: SimpleExoPlayer
    var cacheSizeInMb: Long = 500

    var progressRequired: Boolean = false

    companion object {
        private var simpleCache: SimpleCache? = null
        var mLoadControl: DefaultLoadControl? = null
        var mDataSourceFactory: DataSource.Factory? = null
        var mCacheEnabled = false

        private var currentWindow = 0
        private var playbackPosition: Long = 0
        private var fileName = "media"
    }

    init {
        if (mCacheEnabled != enableCache || mDataSourceFactory == null) {

            mDataSourceFactory = null

            val bandwidthMeter = DefaultBandwidthMeter.Builder(mContext).build()
            mDataSourceFactory = DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext, app_name), bandwidthMeter)

            // LoadControl that controls when the MediaSource buffers more media, and how much media is buffered.
            // LoadControl is injected when the player is created.
            val builder = DefaultLoadControl.Builder()
            builder.setAllocator(DefaultAllocator(true, 2 * 1024 * 1024))
            builder.setBufferDurationsMs(5000, 5000, 5000, 5000)
            builder.setPrioritizeTimeOverSizeThresholds(true)
            mLoadControl = builder.createDefaultLoadControl()

            if (enableCache) {
                val evictor = LeastRecentlyUsedCacheEvictor(cacheSizeInMb * 1024 * 1024)
                val file = File(mContext.cacheDir, fileName)

                if (simpleCache == null)
                    simpleCache = SimpleCache(file, evictor)

                mDataSourceFactory = CacheDataSourceFactory(
                    simpleCache,
                    mDataSourceFactory,
                    FileDataSource.Factory(),
                    CacheDataSinkFactory(simpleCache, (2 * 1024 * 1024).toLong()),
                    CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                    object : CacheDataSource.EventListener {
                        override fun onCacheIgnored(reason: Int) {
                            Log.d("ZAQ", "onCacheIgnored")
                        }

                        override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
                            Log.d("ZAQ", "onCachedBytesRead , cacheSizeBytes: $cacheSizeBytes   cachedBytesRead: $cachedBytesRead")
                        }
                    })
            }
        }
        mCacheEnabled = enableCache

        mPlayer = SimpleExoPlayer.Builder(/* context= */ mContext)
                .build()
        playerView.setShutterBackgroundColor(Color.TRANSPARENT)
        playerView.player = mPlayer

    }

    fun removeCache() {
        val file = File(mContext.cacheDir, fileName)
        file.delete()
    }

    private var mediaSource: MediaSource? = null
    private var isPreparing = false //This flag is used only for callback

    /**
     * Sets the url to play
     *
     * @param url url to play
     * @param autoPlay whether url will play as soon it Loaded/Prepared
     */
    private var url: String = ""

    fun setUrl(url: String, autoPlay: Boolean = false) {
        if (lifecycle?.currentState == Lifecycle.State.RESUMED) {
            this.url = url
            mediaSource = buildMediaSource(Uri.parse(url))
            loopIfNecessary()
            mPlayer.playWhenReady = autoPlay
            isPreparing = true
            mPlayer.prepare(mediaSource!!)
        }
    }

    var lifecycle: Lifecycle? = null
    fun makeLifeCycleAware(activity: Fragment) {
        lifecycle = activity.lifecycle
        activity.lifecycle.addObserver(this)
    }

    /**
     * Trim or clip media to given start and end milliseconds,
     * Ensure you must call this method after [setUrl] method call
     * You Make sure start time < end time ( Something you do :) )
     *
     * @param start starting time in millisecond
     * @param end ending time in millisecond
     */
    fun clip(start: Long, end: Long) {
        if (mediaSource != null) {
            mediaSource = ClippingMediaSource(mediaSource, start * 1000, end * 1000)
            loopIfNecessary()
        }
        mPlayer.prepare(mediaSource!!)
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val type = Util.inferContentType(uri)
        when (type) {
            C.TYPE_SS -> return SsMediaSource.Factory(mDataSourceFactory!!).createMediaSource(uri)
            C.TYPE_DASH -> return DashMediaSource.Factory(mDataSourceFactory!!).createMediaSource(uri)
            C.TYPE_HLS -> return HlsMediaSource.Factory(mDataSourceFactory!!).createMediaSource(uri)
            C.TYPE_OTHER -> return ProgressiveMediaSource.Factory(mDataSourceFactory!!).createMediaSource(uri)
            else -> {
                throw IllegalStateException("Unsupported type: $type") as Throwable
            }
        }
    }

    /**
     * Looping if user set if looping necessary
     */
    private fun loopIfNecessary() {
        if (loopVideo) {
            mediaSource = LoopingMediaSource(mediaSource, loopCount)
        }
    }

    /**
     * Used to start player
     * Ensure you must call this method after [setUrl] method call
     */
    fun play() {
        mPlayer.playWhenReady = true
        mPlayer.seekTo(currentWindow, playbackPosition)
    }

    /**
     * Used to pause player
     * Ensure you must call this method after [setUrl] method call
     */
    fun actionPlayPause(state: Boolean) {
        mPlayer.playWhenReady = state
    }

    /**
     * Used to stop player
     * Ensure you must call this method after [setUrl] method call
     */
    fun stop() {
        playbackPosition = mPlayer.currentPosition
        currentWindow = mPlayer.currentWindowIndex
        mPlayer.stop()
    }

    fun getCurrentPosition() : Int {
        return mPlayer.currentPosition.toInt()
    }

    fun getDuration() : Int {
        return mPlayer.duration.toInt()
    }
    /**
     * Used to seek player to given position(in milliseconds)
     * Ensure you must call this method after [setUrl] method call
     */
    fun seekTo(positionMs: Long) {
        mPlayer.seekTo(positionMs)
    }

    /**
     * Returns whether player is playing
     */
    fun isPlaying(): Boolean {
        return mPlayer.isPlaying
    }

    //Life Cycle
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    protected fun onPause() {
        mPlayer.playWhenReady = false
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    protected fun onResume() {
        mPlayer.playWhenReady = true
    }

    /**
     * Listener that used for most popular callbacks
     */
    fun setListener(progressRequired: Boolean = false, listener: Listener) {
        this.progressRequired = progressRequired
        mPlayer.addListener(object : Player.EventListener {

            override fun onPlayerError(error: ExoPlaybackException) {
                listener.onError(error)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (isPreparing && playbackState == Player.STATE_READY) {
                    isPreparing = false
                    listener.onPlayerReady()
                }
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        listener.onBuffering(true)
                    }
                    Player.STATE_READY -> {
                        listener.onBuffering(false)
                        if (playWhenReady) {
                            listener.onStart()
                        } else {
                            listener.onStop()
                        }
                    }

                    Player.STATE_IDLE -> {
                        listener.onBuffering(false)
                        listener.onError(null)
                    }
                    Player.STATE_ENDED -> {
                        listener.onBuffering(false)
                        listener.onStop()
                    }
                }
            }
        })
    }

    interface Listener {
        fun onPlayerReady() {}
        fun onStart() {}
        fun onStop() {}
        fun onProgress(positionMs: Long) {}
        fun onError(error: ExoPlaybackException?) {}
        fun onBuffering(isBuffering: Boolean) {}
        fun onToggleControllerVisible(isVisible: Boolean) {}
    }
}