package com.mirror.news;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.SeekBar;

import com.brightcove.player.model.Video;
import com.brightcove.player.view.BrightcovePlayer;
import com.brightcove.player.view.BrightcoveVideoView;
import com.rbelchior.brightcoveimasample.R;

public class BrightcoveFullScreenVideoActivity extends BrightcovePlayer {

    /**
     * this should be a {@link Video} object already loaded from a brightcove videoplayer
     */
    public static final String EXTRA_VIDEO_OBJECT = "video";
    public static final String EXTRA_VIDEO_ID = "extra_video_id";
    public static final String EXTRA_VIDEO_PLAY_POSITION = "video_play_position";
    public static final String EXTRA_VIDEO_TITLE = "video_title";
    public static final String EXTRA_ARTICLE_SHORT_ID = "article_short_id";
    public static final String EXTRA_ARTICLE_HEADLINE = "article_headline";
    public static final String EXTRA_ORDER_IN_PARENT = "order_in_parent";
    public static final String EXTRA_TOPIC_NAME = "topic_name";

    private View decorView;

    protected BrightcoveVideoView brightcoveVideoView;
    protected VideoLoadingView videoLoadingView;

    private BrightcoveFullScreenController controller;


    public static Intent newIntent(Context ctx, String videoId) {

        return new Intent(ctx, BrightcoveFullScreenVideoActivity.class)
                .putExtra(BrightcoveFullScreenVideoActivity.EXTRA_VIDEO_ID, videoId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // this ordering is required by Brightcove reference
        setContentView(R.layout.activity_brightcove_full_screen_video);

        decorView = getWindow().getDecorView();
        videoLoadingView = (VideoLoadingView) findViewById(R.id.video_loading_view);
        brightcoveVideoView = (BrightcoveVideoView) findViewById(R.id.fullscreen_brightcove_view);

        controller = new BrightcoveFullScreenController(this);

        super.onCreate(savedInstanceState);

        configureSeekBar();

        controller.create(getIntent().getExtras());
    }

    @Override
    protected void onStop() {
        super.onStop();
        controller.stop();

        brightcoveVideoView.stopPlayback();
    }

    private void configureSeekBar() {

        ((SeekBar) brightcoveVideoView.findViewById(R.id.seek_bar)).getProgressDrawable()
                .setColorFilter(
                        ResourcesCompat.getColor(getResources(), android.R.color.holo_red_dark, getTheme()),
                        PorterDuff.Mode.SRC_IN);
    }

    public void onVideoLoaded(Video video, int positionMillis) {
        brightcoveVideoView.setVisibility(View.VISIBLE);
        brightcoveVideoView.add(video);
        brightcoveVideoView.start();
        brightcoveVideoView.seekTo(positionMillis);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void hideLoadingView() {
        videoLoadingView.hide();
    }

    public void showLoadingView() {
        videoLoadingView.show();
    }

    @Override
    public BrightcoveVideoView getBrightcoveVideoView() {
        return brightcoveVideoView;
    }


    // This snippet hides the system bars.
    public void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    public void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public View getDecorView() {
        return decorView;
    }
}