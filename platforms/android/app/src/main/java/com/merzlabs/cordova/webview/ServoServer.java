package com.merzlabs.cordova.webview;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import android.content.res.AssetManager;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServoServer {
    private static final String TAG = "ServoServer";
    private final AssetManager assetManager;
    AsyncHttpServer server = new AsyncHttpServer();

    List<WebSocket> _sockets = new ArrayList<WebSocket>();

    public ServoServer(CordovaInterface cordova) {
        this.assetManager = cordova.getActivity().getAssets();

        // The HTTP server handles serving the local assets
        server.get(".*", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String path = request.getPath();

                LOG.d(TAG, "The webview requests this path: " + path);

                // Default index response
                if (path.isEmpty() || path.equals("/")) {
                    path = "www/index.html";
                } else if (!path.startsWith("www/")) {
                    // remove leading slash if present
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    path = "www/" + path;
                }

                try {
                    LOG.d(TAG, "Serving: " + path);
                    InputStream inputStream = assetManager.open(path);

                    // Get the asset and send it back
                    int available = inputStream.available();
                    byte[] buffer = new byte[available];
                    inputStream.read(buffer);
                    inputStream.close();

                    // Get content type for file
                    String contentType = URLConnection.guessContentTypeFromName(path);

                    response.send(contentType, buffer);
                } catch (IOException e) {
                    LOG.e(TAG, "Error loading the asset", e);
                    // TODO how to handle the error here properly?
                    response.send("Error on path: " + path);
                }

            }
        });

        // The websocket server is the current solution for getting a JS <-> native
        // bridge without customizing servo
        server.websocket("/live", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                _sockets.add(webSocket);

                // Use this to clean up any references to your websocket
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                LOG.e("WebSocket", "An error occurred", ex);
                        } finally {
                            _sockets.remove(webSocket);
                        }
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        if ("Hello Server".equals(s))
                            webSocket.send("Welcome Client!");
                    }
                });

            }
        });
    }

    public void start() {
        try {
            LOG.d(TAG, "Starting asset & websocket server");
            server.listen(5000);
        } catch (Exception e) {
            LOG.e(TAG, "Failed to Start server", e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            LOG.e(TAG, "Failed to stop server", e);
        }
    }
}
