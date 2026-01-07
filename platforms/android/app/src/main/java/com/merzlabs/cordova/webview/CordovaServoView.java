package com.merzlabs.cordova.webview;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.LOG;
import org.servo.servoview.Servo;

/**
 * Custom ServoView subclass that enables us to capture events needed for Cordova.
 */
public class CordovaServoView extends org.servo.servoview.ServoView implements CordovaWebViewEngine.EngineView {
    private ServoWebViewEngine parentEngine;
    private CordovaInterface cordova;

    public CordovaServoView(Context context) {
        this(context, null);
    }

    public CordovaServoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void init(ServoWebViewEngine parentEngine, CordovaInterface cordova) {
        this.cordova = cordova;
        this.parentEngine = parentEngine;
        
        // Set up the Servo client to handle callbacks
        setClient(new Servo.Client() {
            @Override
            public void onAlert(String message) {
                // Handle JavaScript alert
            }

            @Override
            public void onLoadStarted() {
                String url = parentEngine != null ? parentEngine.currentUrl : "unknown";
                LOG.d("ServoView", "onLoadStarted: " + url);
                if (parentEngine != null && parentEngine.client != null) {
                    String urlStr = parentEngine.currentUrl != null ? parentEngine.currentUrl : "";
                    parentEngine.client.onPageStarted(urlStr);
                }
            }

            @Override
            public void onLoadEnded() {
                String url = parentEngine != null ? parentEngine.currentUrl : "unknown";
                LOG.d("ServoView", "onLoadEnded: " + url);
                if (parentEngine != null && parentEngine.client != null) {
                    String urlStr = parentEngine.currentUrl != null ? parentEngine.currentUrl : "";
                    parentEngine.client.onPageFinishedLoading(urlStr);
                }
            }

            @Override
            public void onTitleChanged(String title) {
                // Handle title changes
            }

            @Override
            public void onUrlChanged(String url) {
                if (parentEngine != null) {
                    parentEngine.currentUrl = url;
                }
            }

            @Override
            public void onHistoryChanged(boolean canGoBack, boolean canGoForward) {
                if (parentEngine != null) {
                    parentEngine.canGoBack = canGoBack;
                }
            }

            @Override
            public void onRedrawing(boolean redrawing) {
                // Handle redrawing state
            }

            @Override
            public void onImeShow() {
                // Handle IME show
            }

            @Override
            public void onImeHide() {
                // Handle IME hide
            }

            @Override
            public void onMediaSessionMetadata(String title, String artist, String album) {
                // Handle media session metadata
            }

            @Override
            public void onMediaSessionPlaybackStateChange(int state) {
                // Handle media session playback state
            }

            @Override
            public void onMediaSessionSetPositionState(float duration, float position, float playbackRate) {
                // Handle media session position state
            }
        });
    }

    @Override
    public CordovaWebView getCordovaWebView() {
        return parentEngine != null ? parentEngine.getCordovaWebView() : null;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (parentEngine != null && parentEngine.client != null) {
            Boolean ret = parentEngine.client.onDispatchKeyEvent(event);
            if (ret != null) {
                return ret.booleanValue();
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
