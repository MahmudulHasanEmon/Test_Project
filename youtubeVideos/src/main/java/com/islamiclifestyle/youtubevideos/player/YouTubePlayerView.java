package com.islamiclifestyle.youtubevideos.player;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.islamiclifestyle.youtubevideos.player.playerUtils.FullScreenHelper;
import com.islamiclifestyle.youtubevideos.player.playerUtils.PlaybackResumer;
import com.islamiclifestyle.youtubevideos.ui.DefaultPlayerUIController;
import com.islamiclifestyle.youtubevideos.ui.PlayerUIController;
import com.islamiclifestyle.youtubevideos.utils.NetworkReceiver;
import com.islamiclifestyle.youtubevideos.utils.Utils;

import java.util.concurrent.Callable;

public class YouTubePlayerView extends FrameLayout implements NetworkReceiver.NetworkListener, LifecycleObserver {

    @NonNull
    private final WebViewYouTubePlayer youTubePlayer;
    @Nullable
    private DefaultPlayerUIController defaultPlayerUIController;

    @NonNull private final NetworkReceiver networkReceiver;
    @NonNull private final PlaybackResumer playbackResumer;
    @NonNull private final FullScreenHelper fullScreenHelper;
    @Nullable private Callable asyncInitialization;

    public YouTubePlayerView(Context context) {
        this(context, null);
    }

    public YouTubePlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YouTubePlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        youTubePlayer = new WebViewYouTubePlayer(context);
        addView(youTubePlayer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        defaultPlayerUIController = new DefaultPlayerUIController(this, youTubePlayer);

        playbackResumer = new PlaybackResumer();
        networkReceiver = new NetworkReceiver(this);
        fullScreenHelper = new FullScreenHelper();

        fullScreenHelper.addFullScreenListener(defaultPlayerUIController);
        addYouTubePlayerListeners(youTubePlayer);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // if height == wrap content make the view 16:9
        if(getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            int sixteenNineHeight = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(widthMeasureSpec) * 9 / 16, View.MeasureSpec.EXACTLY);
            super.onMeasure(widthMeasureSpec, sixteenNineHeight);
        } else
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Initialize the player
     * @param youTubePlayerInitListener lister for player init events
     * @param handleNetworkEvents if <b>true</b> a broadcast receiver will be registered.<br/>If <b>false</b> you should handle network events with your own broadcast receiver. See {@link YouTubePlayerView#onNetworkAvailable()} and {@link YouTubePlayerView#onNetworkUnavailable()}
     */
    public void initialize(@NonNull final YouTubePlayerInitListener youTubePlayerInitListener, boolean handleNetworkEvents) throws Exception {
        if(handleNetworkEvents)
            getContext().registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        asyncInitialization = new Callable() {
            @Override
            public Object call() {
                youTubePlayer.initialize(new YouTubePlayerInitListener() {
                    @Override
                    public void onInitSuccess(YouTubePlayer youTubePlayer) {
                        youTubePlayerInitListener.onInitSuccess(youTubePlayer);
                    }
                });
                return null;
            }
        };

        if(Utils.isOnline(getContext()))
            asyncInitialization.call();
    }

    /**
     * Calls {@link WebView#destroy()} on the player. And unregisters the broadcast receiver (for network events), if registered.
     * Call this method before destroying the host Fragment/Activity, or register this View as an observer of its host lifcecycle
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void release() {
        youTubePlayer.destroy();
        try {
            getContext().unregisterReceiver(networkReceiver);
        } catch (Exception ignore) {
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onStop() {
        youTubePlayer.pause();
    }

    @Override
    public void onNetworkAvailable() throws Exception {
        if(asyncInitialization != null)
            asyncInitialization.call();
        else
            playbackResumer.resume(youTubePlayer);
    }

    @Override
    public void onNetworkUnavailable() {
    }

    @NonNull
    public PlayerUIController getPlayerUIController() {
        if(defaultPlayerUIController == null)
            throw new RuntimeException("You have inflated a custom player UI. You must manage it with your own controller.");

        return defaultPlayerUIController;
    }

    /**
     * Replaces the default UI of the player with a custom UI.<br/>
     * You will have to control the custom UI in your application,
     * the default controller obtained through {@link YouTubePlayerView#getPlayerUIController()} won't be available anymore.
     * @param customPlayerUILayoutID the ID of the layout defining the custom UI.
     * @return The inflated View
     */
    public View inflateCustomPlayerUI(@LayoutRes int customPlayerUILayoutID) {
        removeViews(1, this.getChildCount()-1);

        if (defaultPlayerUIController != null) {
            youTubePlayer.removeListener(defaultPlayerUIController);
            fullScreenHelper.removeFullScreenListener(defaultPlayerUIController);
        }

        defaultPlayerUIController = null;

        return View.inflate(getContext(), customPlayerUILayoutID, this);
    }

    public void enterFullScreen() {
        fullScreenHelper.enterFullScreen(this);
    }

    public void exitFullScreen() {
        fullScreenHelper.exitFullScreen(this);
    }

    public boolean isFullScreen() {
        return fullScreenHelper.isFullScreen();
    }

    public void toggleFullScreen() {
        fullScreenHelper.toggleFullScreen(this);
    }

    public boolean addFullScreenListener(@NonNull YouTubePlayerFullScreenListener fullScreenListener) {
        return fullScreenHelper.addFullScreenListener(fullScreenListener);
    }

    public boolean removeFullScreenListener(@NonNull YouTubePlayerFullScreenListener fullScreenListener) {
        return fullScreenHelper.removeFullScreenListener(fullScreenListener);
    }

    private void addYouTubePlayerListeners(final YouTubePlayer youTubePlayer) {
        if (defaultPlayerUIController != null) youTubePlayer.addListener(defaultPlayerUIController);
        youTubePlayer.addListener(playbackResumer);
        youTubePlayer.addListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady() {
                asyncInitialization = null;
                //youTubePlayer.removeListener(this);
            }
        });
    }
}
