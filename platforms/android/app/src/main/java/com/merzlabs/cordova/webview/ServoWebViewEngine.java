package com.merzlabs.cordova.webview;

import android.content.Context;
import android.view.View;
import android.webkit.ValueCallback;

import org.apache.cordova.CordovaBridge;
import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginManager;
import org.apache.cordova.LOG;

public class ServoWebViewEngine implements CordovaWebViewEngine {
    public static final String TAG = "ServoWebViewEngine";
    protected final CordovaServoView servoView;

    protected String currentUrl = "";
    protected boolean canGoBack = false;
    protected CordovaPreferences preferences;
    protected CordovaBridge bridge;
    protected CordovaWebViewEngine.Client client;
    protected CordovaWebView parentWebView;
    protected CordovaInterface cordova;
    protected PluginManager pluginManager;
    protected CordovaResourceApi resourceApi;
    protected NativeToJsMessageQueue nativeToJsMessageQueue;

    /**
     * Used when created via reflection.
     */
    public ServoWebViewEngine(Context context, CordovaPreferences preferences) {
        this(new CordovaServoView(context), preferences);
        LOG.d(TAG, "Servo WebView Engine Starting Right Up 1...");
    }

    public ServoWebViewEngine(CordovaServoView webView) {
        this(webView, null);
        LOG.d(TAG, "Servo WebView Engine Starting Right Up 2...");
    }

    public ServoWebViewEngine(CordovaServoView webView, CordovaPreferences preferences) {
        this.servoView = webView;
        this.preferences = preferences;
        LOG.d(TAG, "Servo WebView Engine Starting Right Up 3...");
    }

    @Override
    public void init(CordovaWebView parentWebView, CordovaInterface cordova, final CordovaWebViewEngine.Client client,
            CordovaResourceApi resourceApi, PluginManager pluginManager,
            NativeToJsMessageQueue nativeToJsMessageQueue) {
        ConfigXmlParser parser = new ConfigXmlParser();
        parser.parse(cordova.getActivity());

        this.parentWebView = parentWebView;
        this.cordova = cordova;
        this.client = client;
        this.resourceApi = resourceApi;
        this.pluginManager = pluginManager;
        this.nativeToJsMessageQueue = nativeToJsMessageQueue;
        servoView.init(this, cordova);
    }

    @Override
    public CordovaWebView getCordovaWebView() {
        return parentWebView;
    }

    @Override
    public ICordovaCookieManager getCookieManager() {
        // TODO ServoView does not have a cookie manager, yet
        return null;
    }

    @Override
    public View getView() {
        return servoView;
    }

    @Override
    public void loadUrl(final String url, boolean clearNavigationStack) {
        LOG.d(TAG, "loadUrl called with: " + url + " (clearStack=" + clearNavigationStack + ")");
        currentUrl = url;
        servoView.loadUri(url);
    }

    @Override
    public String getUrl() {
        return currentUrl;
    }

    @Override
    public void stopLoading() {
        servoView.stop();
    }

    @Override
    public void clearCache() {
        // ServoView doesn't have a direct clearCache method
        LOG.w(TAG, "clearCache not implemented for ServoView");
    }

    @Override
    public void clearHistory() {
        // ServoView doesn't have a direct clearHistory method
        canGoBack = false;
        LOG.w(TAG, "clearHistory not implemented for ServoView");
    }

    @Override
    public boolean canGoBack() {
        return canGoBack;
    }

    /**
     * Go to previous page in history.
     *
     * @return true if we went back, false if we are already at top
     */
    @Override
    public boolean goBack() {
        if (canGoBack) {
            servoView.goBack();
            return true;
        }
        return false;
    }

    @Override
    public void setPaused(boolean value) {
        if (value) {
            servoView.onPause();
        } else {
            servoView.onResume();
        }
    }

    @Override
    public void destroy() {
        // ServoView cleanup
        // TODO ServoView may need additional cleanup
        this.servoView.stop();
    }

    @Override
    public void evaluateJavascript(String js, ValueCallback<String> callback) {
        // Servo doesn't have evaluateJavascript
        LOG.w(TAG, "evaluateJavascript not implemented for ServoView");

        // For now, just log and callback with null
        if (callback != null) {
            callback.onReceiveValue(null);
        }
    }

    /**
     * Handle JavaScript bridge messages from alert()
     */
    protected void handleJsBridgeMessage(String message) {
        LOG.w(TAG, "handleJsBridgeMessage not implemented for ServoView: " + message);
    }
}
