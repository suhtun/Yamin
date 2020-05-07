Yamin Player with RecyclerView (Build with Kotlin)
===========================================================
This will support you to play video inside recyclerview using exoplayer. 
You only need to initialize 3 lines of code to play tons of video.

### Features
* just need 3 lines of code
* not necessary viewholder to support playing video
* play video inside recyclerview item like tiktok, instagram
* include pause/play/mute handling

### Setup
To get a Git project into your build:
```
// Step 1. Add the JitPack repository to root build.gradle build file
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

// Step 2. Add the dependency to app level build.gradle
dependencies {
    implementation 'com.github.pankaj89:MasterExoPlayer:1.4.4'
}

```
### How to use
### 1. Add YaminPlayer inside RecyclerView Item
<mm.sumyat.player.YaminPlayer
    android:id="@+id/player"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />

### 2. Set url of your video view to YaminPlayer inside RecyclerView Adapter onBindViewHolder
binding.frame.url = obj.movie_url

### 3. Attach YaminPlayerHelper to RecyclerView
```kotlin
val playerHelper = YaminPlayerHelper(mContext = this, id = R.id.masterExoPlayer)
playerHelper.makeLifeCycleAware(this)
playerHelper.attachToRecyclerView(recyclerView)
```

### Libraries
* [RecyclerView][recyclerview]
* [Android Lifecycle][android-lifecycle]
* [Exoplayer][exo-player]

[recyclerview]: https://developer.android.com/reference/kotlin/androidx/recyclerview/widget/RecyclerView
[android-lifecycle]: https://developer.android.com/topic/libraries/architecture/lifecycle
[exo-player]: https://developer.android.com/guide/topics/media/exoplayer
