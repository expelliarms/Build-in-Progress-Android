package com.example.helperClass;

import android.os.Bundle;
import android.util.Log;

import com.bip_public_android.DeveloperKey;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

/**
 * Created by victo_000 on 7/9/2015.
 */
public class YTPlayerFragment extends YouTubePlayerSupportFragment {
    String TAG = "YTPLAYERFRAGMENT";
    private YouTubePlayer ytPlayer;

    public static YTPlayerFragment newInstance(String ytID) {

        YTPlayerFragment ytPlayerFragment = new YTPlayerFragment();

        Bundle bundle = new Bundle();
        bundle.putString("ytID", ytID);

        ytPlayerFragment.setArguments(bundle);
        ytPlayerFragment.init();

        return ytPlayerFragment;
    }

    private void init() {

        initialize(DeveloperKey.DEVELOPER_KEY, new YouTubePlayer.OnInitializedListener() {

            @Override
            public void onInitializationFailure(Provider arg0, YouTubeInitializationResult arg1) {
            }

            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
                Log.d(TAG, "VCJ : yt player " + player + "");
                ytPlayer = player;
                ytPlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT); // timebar
                ytPlayer.cueVideo(getArguments().getString("ytID")); // loads and prepares, but doesn't play
                //ytPlayer.loadVideo(getArguments().getString("ytID")); // loads and plays the specified video
                ytPlayer.setShowFullscreenButton(false); //don't show the fullscreen button
/*                if (!wasRestored) {
                    ytPlayer.loadVideo(getArguments().getString("url"), 0);

                }*/

                ytPlayer.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
                    @Override
                    public void onLoading() {
                    }

                    @Override
                    public void onLoaded(String s) {
                        // plays automatically, if the youtube video was selected
                        Log.d(TAG, "VCJ : whatever s is : " + s + " the ytID : " + getArguments().getString("ytID"));
                        Log.d(TAG, "VCJ player : " + ytPlayer + " current : " + ytPlayer.getCurrentTimeMillis() + " duration: " + ytPlayer.getDurationMillis());
                        ytPlayer.play();
                    }

                    @Override
                    public void onAdStarted() {
                    }

                    @Override
                    public void onVideoStarted() {
                    }

                    @Override
                    public void onVideoEnded() {
                        //listens for changes on all instances
                        // each player will start the video over again
                        // When the video ended, the thumbnail would become every youtube videos thumbnail for some reason
                        // When this happens, playing other videos doesn't work properly
                    }

                    @Override
                    public void onError(YouTubePlayer.ErrorReason errorReason) {
                    }
                });
            }
        });

    }

    @Override
    public void onDestroy() {
        ytPlayer.release();
        super.onDestroy();

    }

}
