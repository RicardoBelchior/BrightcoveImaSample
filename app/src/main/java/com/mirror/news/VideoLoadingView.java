package com.mirror.news;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.rbelchior.brightcoveimasample.R;

import me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable;

/**
 * Created by eduardosantos on 20/03/2016.
 */
public class VideoLoadingView extends LinearLayout {

    ProgressBar videoLoadingProgressView;
    LinearLayout videoLoadingContainerView;

    public VideoLoadingView(Context context) {
        super(context);
        init();
    }

    public VideoLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoLoadingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.video_loading_placeholder, this);
        videoLoadingProgressView = (ProgressBar) findViewById(R.id.video_loading_progressbar);
        videoLoadingContainerView = (LinearLayout) findViewById(R.id.video_loading_progressbar_container);

        setupProgressBar();
    }

    private void setupProgressBar() {
        IndeterminateProgressDrawable progressDrawable =
                new IndeterminateProgressDrawable(getContext());

        progressDrawable.setUseIntrinsicPadding(false);
        progressDrawable.setColorFilter(
                ResourcesCompat.getColor(getResources(), android.R.color.holo_red_dark, getContext().getTheme()),
                PorterDuff.Mode.SRC_IN);

        videoLoadingProgressView.setIndeterminateDrawable(progressDrawable);
    }

    public void show() {
        videoLoadingContainerView.setVisibility(VISIBLE);
    }

    public void hide() {
        videoLoadingContainerView.setVisibility(GONE);
    }

}
