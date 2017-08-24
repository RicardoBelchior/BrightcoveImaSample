package com.mirror.news;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;

import com.brightcove.ima.GoogleIMAComponent;
import com.brightcove.ima.GoogleIMAEventType;
import com.brightcove.player.edge.Catalog;
import com.brightcove.player.edge.VideoListener;
import com.brightcove.player.event.Event;
import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventListener;
import com.brightcove.player.event.EventType;
import com.brightcove.player.media.DeliveryType;
import com.brightcove.player.media.VideoFields;
import com.brightcove.player.mediacontroller.BrightcoveMediaController;
import com.brightcove.player.mediacontroller.ShowHideController;
import com.brightcove.player.model.CuePoint;
import com.brightcove.player.model.Source;
import com.brightcove.player.model.Video;
import com.brightcove.player.util.StringUtil;
import com.brightcove.player.view.BrightcoveVideoView;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.rbelchior.brightcoveimasample.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrightcoveFullScreenController {
    private static final String TAG = BrightcoveFullScreenController.class.getSimpleName();

    private static final String cuePointType = "ad";

    private static final String EVENT_DID_SHOW_MEDIA_CONTROLS = ShowHideController.DID_SHOW_MEDIA_CONTROLS;
    private static final String EVENT_DID_HIDE_MEDIA_CONTROLS = ShowHideController.DID_HIDE_MEDIA_CONTROLS;

    private BrightcoveFullScreenVideoActivity activity;

    private EventEmitter eventEmitter;

    private GoogleIMAComponent googleIMAComponent;
    private BrightcoveMediaController mediaController;

    public BrightcoveFullScreenController(BrightcoveFullScreenVideoActivity activity) {
        this.activity = activity;

        final BrightcoveVideoView videoView = activity.getBrightcoveVideoView();

        mediaController = new BrightcoveMediaController(videoView);
        mediaController.setCuePointMarkersEnabled(true);
        videoView.setMediaController(mediaController);

    }

    public void create(Bundle extras) {

        final BrightcoveVideoView videoView = activity.getBrightcoveVideoView();

        // setup events listeners
        eventEmitter = videoView.getEventEmitter();
        eventEmitter.emit(EventType.ENTER_FULL_SCREEN); // tell the emitter we're on fullscreen

        setupExitFullScreenListener();
        setupLoadingViewListener();

        eventEmitter.on(EventType.ANY, new EventListener() {
            @Override
            public void processEvent(Event event) {
                Log.d("TAG", "Event: " + event.getType());
            }
        });

        // Use a procedural abstraction to setup the Google IMA SDK via the plugin and establish
        // a playlist listener object for our sample video: the Potter Puppet show.
        // TODO UNCOMMENT NEXT LINE TO ENABLE PRE-ROLL ADS
        String[] adUrls = {}; //IMAVideoAdsBuilder.buildPreRollAdsRequestUrl();
        setupGoogleIMA(videoView, adUrls);

        // show loading view asap
        activity.showLoadingView();

        setupVideo(extras, eventEmitter);

        setupImmersiveMode();
    }

    private void setupImmersiveMode() {
        View decorView = activity.getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(systemUiVisibilityChangeListener);

        eventEmitter.on(EVENT_DID_SHOW_MEDIA_CONTROLS, systemUiVisibilityListener);
        eventEmitter.on(EVENT_DID_HIDE_MEDIA_CONTROLS, systemUiVisibilityListener);
    }

    public void setupLoadingViewListener() {
        eventEmitter.on(EventType.DID_PLAY, loadingViewListener);
        eventEmitter.on(EventType.AD_STARTED, loadingViewListener);
        eventEmitter.on(EventType.SEEK_TO, loadingViewListener);
        eventEmitter.on(EventType.DID_SEEK_TO, loadingViewListener);
    }

    public void setupExitFullScreenListener() {
        eventEmitter.on(EventType.EXIT_FULL_SCREEN, exitFullScreenListener);
        eventEmitter.on(EventType.COMPLETED, exitFullScreenListener);
    }

    public void stop() {

        if (eventEmitter != null) {
            eventEmitter.off();
            eventEmitter.disable();
        }

        View decorView = activity.getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(null);
    }

    private void setupVideo(Bundle extras, EventEmitter eventEmitter) {

        if (extras == null) {
            Log.e("TAG", "You need to pass a video object or id!");
            return;
        }

        final Video video = (Video) extras.get(BrightcoveFullScreenVideoActivity.EXTRA_VIDEO_OBJECT);
        if (video != null) {

            int position = extras.getInt(BrightcoveFullScreenVideoActivity.EXTRA_VIDEO_PLAY_POSITION, 0);
            loadFromVideoObject(video, position);

        } else {

            String videoId = extras.getString(BrightcoveFullScreenVideoActivity.EXTRA_VIDEO_ID);
            if (TextUtils.isEmpty(videoId)) {
                Log.e("TAG", "You need to pass a video id!");
                // anyway, try to load because we want to trigger VideoListener#onError
            }

            loadFromVideoId(eventEmitter, videoId);
        }
    }

    private void loadFromVideoObject(Video video, int position) {
        onVideoLoaded(video, position);
    }

    private void loadFromVideoId(EventEmitter eventEmitter, String videoId) {

        final Resources res = activity.getResources();
        String brightcoveAccountId = res.getString(R.string.brightcove_account_id);
        String brightcovePolicyKey = res.getString(R.string.brightcove_policy_key);

        // Remove the HLS_URL field from the catalog request to allow
        // midrolls to work.  Midrolls don't work with HLS due to
        // seeking bugs in the Android OS.
        Map<String, String> options = new HashMap<>();
        List<String> values = new ArrayList<>(Arrays.asList(VideoFields.DEFAULT_FIELDS));
        values.remove(VideoFields.HLS_URL);
        options.put("video_fields", StringUtil.join(values, ","));

        Catalog catalog = new Catalog(eventEmitter, brightcoveAccountId, brightcovePolicyKey);
        catalog.findVideoByID(videoId, options, videoListener);
    }

    private EventListener loadingViewListener = new EventListener() {
        @Override
        public void processEvent(Event event) {

            switch (event.getType()) {

                case EventType.SEEK_TO:
                    activity.showLoadingView();
                    break;

                case EventType.DID_PLAY:
                case EventType.AD_STARTED:
                case EventType.DID_SEEK_TO:
                    activity.hideLoadingView();
                    break;
            }
        }
    };

    private final EventListener exitFullScreenListener = new EventListener() {
        @Override
        public void processEvent(Event event) {
            activity.onBackPressed();

        }
    };

    private final VideoListener videoListener = new VideoListener() {
        @Override
        public void onVideo(Video video) {

            onVideoLoaded(video, 0);
        }

        @Override
        public void onError(String error) {
            super.onError(error);
            Log.e("TAG", "onError: " + error);
        }
    };

    private void onVideoLoaded(Video video, int positionMillis) {

        activity.onVideoLoaded(video, positionMillis);
    }

    /**
     * Specify where the ad should interrupt the main video. This code provides a procedural
     * abstraction for the Google IMA Plugin setup code.
     */
    private void setupCuePoints(Source source) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> details = new HashMap<>();

        // preroll
        CuePoint cuePoint = new CuePoint(CuePoint.PositionType.BEFORE, cuePointType, properties);
        details.put(Event.CUE_POINT, cuePoint);
        eventEmitter.emit(EventType.SET_CUE_POINT, details);

        // midroll at 10 seconds.
        // Due HLS bugs in the Android MediaPlayer, midrolls are not supported.
        if (!source.getDeliveryType().equals(DeliveryType.HLS)) {
            int cuepointTime = 10 * (int) DateUtils.SECOND_IN_MILLIS;
            cuePoint = new CuePoint(cuepointTime, cuePointType, properties);
            details.put(Event.CUE_POINT, cuePoint);
            eventEmitter.emit(EventType.SET_CUE_POINT, details);
            // Add a marker where the ad will be.
            mediaController.getBrightcoveSeekBar().addMarker(cuepointTime);
        }

        // postroll
        cuePoint = new CuePoint(CuePoint.PositionType.AFTER, cuePointType, properties);
        details.put(Event.CUE_POINT, cuePoint);
        eventEmitter.emit(EventType.SET_CUE_POINT, details);
    }

    /**
     * Setup the Brightcove IMA Plugin: add some cue points; establish a factory object to
     * obtain the Google IMA SDK instance.
     */
    private void setupGoogleIMA(final BrightcoveVideoView brightcoveVideoView, @NonNull final String[] adUrls) {

        // Defer adding cue points until the set video event is triggered.
        eventEmitter.on(EventType.DID_SET_SOURCE, new EventListener() {
            @Override
            public void processEvent(Event event) {
                setupCuePoints((Source) event.properties.get(Event.SOURCE));
            }
        });

        // Establish the Google IMA SDK factory instance.
        final ImaSdkFactory sdkFactory = ImaSdkFactory.getInstance();

        // Set up a listener for initializing AdsRequests. The Google IMA plugin emits an ad
        // request event in response to each cue point event.  The event processor (handler)
        // illustrates how to play ads back to back.
        eventEmitter.on(GoogleIMAEventType.ADS_REQUEST_FOR_VIDEO, new EventListener() {
            @Override
            public void processEvent(Event event) {
                // Create a container object for the ads to be presented.
                AdDisplayContainer container = sdkFactory.createAdDisplayContainer();
                container.setPlayer(googleIMAComponent.getVideoAdPlayer());
                container.setAdContainer(brightcoveVideoView);

                // Build the list of ads request objects, one per ad
                // URL, and point each to the ad display container
                // created above.
                ArrayList<AdsRequest> adsRequests = new ArrayList<>(adUrls.length);
                AdsRequest adsRequest;
                for (String adURL : adUrls) {
                    adsRequest = sdkFactory.createAdsRequest();
                    adsRequest.setAdTagUrl(adURL);
                    adsRequest.setAdDisplayContainer(container);
                    adsRequests.add(adsRequest);
                }

                // Respond to the event with the new ad requests.
                event.properties.put(GoogleIMAComponent.ADS_REQUESTS, adsRequests);
                eventEmitter.respond(event);
            }
        });

        // Create the Brightcove IMA Plugin and register the event emitter so that the plugin
        // can deal with video events.
        googleIMAComponent = new GoogleIMAComponent(brightcoveVideoView, eventEmitter);


//        // Enable logging of ad starts
//        eventEmitter.on(EventType.AD_STARTED, listener);
//        eventEmitter.on(GoogleIMAEventType.DID_FAIL_TO_PLAY_AD, listener);
//        eventEmitter.on(EventType.AD_COMPLETED, listener);
    }

    private View.OnSystemUiVisibilityChangeListener systemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            boolean visible = (visibility &
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;

            if (visible) {
                mediaController.show();
            } else {
                mediaController.hide();
            }
        }
    };

    private EventListener systemUiVisibilityListener = new EventListener() {
        @Override
        public void processEvent(Event event) {
            final String eventType = event.getType();

            if (EVENT_DID_SHOW_MEDIA_CONTROLS.equals(eventType)) {
                activity.showSystemUI();

            } else if (EVENT_DID_HIDE_MEDIA_CONTROLS.equals(eventType)) {
                activity.hideSystemUI();
            }
        }
    };
}