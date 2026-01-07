package com.merzlabs.cordova.webview;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.apache.cordova.LOG;

import java.util.ArrayList;
import java.util.List;

public class ServoServer {
    AsyncHttpServer server = new AsyncHttpServer();

    List<WebSocket> _sockets = new ArrayList<WebSocket>();

    public ServoServer() {

        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("Hello!!!");
            }
        });

        server.websocket("/live", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                _sockets.add(webSocket);

                //Use this to clean up any references to your websocket
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
            server.listen(5000);
        } catch (Exception e){
            LOG.e("ServoServer", "Failed to Start server", e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            LOG.e("ServoServer", "Failed to stop server", e);
        }
    }
}
