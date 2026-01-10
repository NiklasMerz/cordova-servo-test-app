cordova.define("cordova-plugin-servo-webview.ServoBridge", function(require, exports, module) {
(function () {
    'use strict';

    var ws = null;
    var isConnected = false;
    var sendQueue = [];  // Outgoing messages waiting for connection
    var receiveQueue = [];  // Incoming messages from WebSocket

    // Save original prompt function
    var originalPrompt = window.prompt;

    function initWebSocket() {
        ws = new WebSocket('ws://localhost:5000/cordova-socket');

        ws.onopen = function () {
            console.info('[ServoWS] Connected to WebSocket server');
            isConnected = true;
            flushSendQueue();

            // Enable polling mode AND force prompt-based bridge
            setTimeout(function () {
                if (typeof cordova !== 'undefined' && cordova.exec) {
                    var androidExec = cordova.require('cordova/android/androidExec');
                    if (androidExec) {
                        // Force PROMPT mode for JS->Native (so we can intercept)
                        if (androidExec.setJsToNativeBridgeMode) {
                            androidExec.setJsToNativeBridgeMode(1); // PROMPT mode
                            console.info('[ServoWS] Enabled PROMPT mode for JS->Native');
                        }
                        // Enable POLLING mode for Native->JS
                        if (androidExec.setNativeToJsBridgeMode) {
                            androidExec.setNativeToJsBridgeMode(0); // POLLING mode
                            console.info('[ServoWS] Enabled POLLING mode for Native->JS');
                        }
                    }
                }
            }, 100);
        };

        ws.onmessage = function (event) {
            console.debug('[ServoWS] Received message:', event.data);

            let message = event.data;

            // TODO find out whats going on here
            // Remove all charaters before the first F or S
            const messageStartIndex = message.search(/[FS]/);
            if (messageStartIndex !== -1) {
                message = message.substring(messageStartIndex);
            }
            console.debug('[ServoWS] Cleaned message:', message);

            // Parse and directly invoke cordova callback
            if (typeof cordova !== 'undefined' && cordova.callbackFromNative) {
                try {
                    // Parse message format: S01 callbackId payload
                    var firstChar = message.charAt(0);
                    if (firstChar === 'S' || firstChar === 'F') {
                        var success = firstChar === 'S';
                        var keepCallback = message.charAt(1) === '1';
                        var spaceIdx = message.indexOf(' ', 2);
                        var status = +message.slice(2, spaceIdx);
                        var nextSpaceIdx = message.indexOf(' ', spaceIdx + 1);
                        var callbackId = message.slice(spaceIdx + 1, nextSpaceIdx);
                        var payloadMessage = message.slice(nextSpaceIdx + 1);

                        // Parse payload (simplified - handles basic types)
                        var payload = [JSON.parse(payloadMessage)];

                        console.log('[ServoWS] Calling callback:', callbackId, success, status, payload);
                        if (!callbackId) {
                            throw new Error('Invalid callbackId');
                        }
                        if (isNaN(status)) {
                            throw new Error('Invalid status code');
                        }
                        if (payloadMessage.length === 0) {
                            throw new Error('Invalid payload');
                        }
                        cordova.callbackFromNative(callbackId, success, status, payload, keepCallback);
                    }
                } catch (e) {
                    console.error('[ServoWS] Error parsing message:', e, message);
                    // Fallback: queue for polling
                    receiveQueue.push(message);
                }
            } else {
                receiveQueue.push(message);
            }
        }

        ws.onerror = function (error) {
            console.error('[ServoWS] WebSocket error:', error);
        };

        ws.onclose = function (event) {
            console.info('[ServoWS] Connection closed:', event.code, event.reason);
            isConnected = false;
        };
    }

    function flushSendQueue() {
        while (sendQueue.length > 0 && isConnected) {
            var msg = sendQueue.shift();
            ws.send(msg);
        }
    }

    // Override window.prompt to intercept Cordova bridge calls
    window.prompt = function (message, defaultValue) {
        console.debug("[ServoWS] Call to prompt", message, defaultValue);
        // Check if this is a Cordova bridge call
        if (defaultValue && typeof defaultValue === 'string') {
            // gap: prefix = exec() call
            if (defaultValue.indexOf('gap:') === 0) {
                
                // Parse the Cordova exec request
                // Format: 'gap:' + JSON.stringify([bridgeSecret, service, action, callbackId])
                var gapData = JSON.parse(defaultValue.substring(4));
                console.debug('[ServoWS] Intercepted gap: call', gapData);
                var bridgeSecret = gapData[0];
                var service = gapData[1];
                var action = gapData[2];
                var callbackId = gapData[3];
                var argsJson = message;

                // Create exec request for WebSocket
                var execRequest = {
                    bridgeSecret: bridgeSecret,
                    service: service,
                    action: action,
                    callbackId: callbackId,
                    args: JSON.parse(argsJson),
                    argsJson: argsJson
                };

                // Send via WebSocket
                var wsMessage = JSON.stringify(execRequest);
                if (isConnected) {
                    ws.send(wsMessage);
                } else {
                    console.warn('[ServoWS] Not connected, queuing message');
                    sendQueue.push(wsMessage);
                }

                // Return any queued response messages
                if (receiveQueue.length > 0) {
                    console.debug('[ServoWS] Returning queued message', receiveQueue[0]);
                    return receiveQueue.shift();
                }
                return '';
            }
            // gap_bridge_mode: prefix = set bridge mode
            else if (defaultValue.indexOf('gap_bridge_mode:') === 0) {
                console.log('[ServoWS] Bridge mode set to:', message, defaultValue);
                return '';
            }
            // gap_poll: prefix = retrieve messages
            else if (defaultValue.indexOf('gap_poll:') === 0) {
                console.debug('[ServoWS] Polling for messages');
                // Return queued messages from WebSocket
                if (receiveQueue.length > 0) {
                    return receiveQueue.shift();
                }
                return '';
            }
            // gap_init: prefix = initialize bridge
            else if (defaultValue.indexOf('gap_init:') === 0) {
                console.log('[ServoWS] Bridge initialization');
                // Return a fake bridge secret (not used with WebSocket)
                return '1';
            }
        }

        // Not a Cordova call, use original prompt
        return originalPrompt.call(window, message, defaultValue);
    };


    // Initialize WebSocket when module loads
    initWebSocket();

    console.log('[ServoWS] Prompt initialized');
})();
});
