cordova.define("cordova-plugin-servo-webview.ServoBridge", function(require, exports, module) {
(function () {
    'use strict';
    var exec = require('cordova/exec');
    var nativeApi = require('cordova/android/promptbasednativeapi');

    var ws = null;
    var isConnected = false;
    var sendQueue = [];  // Outgoing messages waiting for connection
    var recieveQueue = []; // Incoming messages waiting to be processed
    var bridgeSecret = null;  // Store the actual bridge secret from native
    var isBridgeInitialized = false;

    // Save original prompt function
    var originalPrompt = window.prompt;

    function initWebSocket() {
        ws = new WebSocket('ws://localhost:5000/cordova-socket');

        ws.onopen = function () {
            isBridgeInitialized = false;
            bridgeSecret = null;
            console.info('[ServoWS] Connected to WebSocket server');
            isConnected = true;
            
            // Init to get bridge secret from native
            ws.send('gap_init:0');
        };

        ws.onmessage = function (event) {
            console.debug('[ServoWS] Received message:', event.data);

            let message = event.data;

            if (message.indexOf('gap_init:') === 0) {
                const actualSecret = message.substring('gap_init:'.length);
                if (actualSecret) {
                    bridgeSecret = actualSecret;
                    console.debug('[ServoWS] Stored bridge secret:', actualSecret);
                    console.debug('[ServoWS] Bridge initialized', isBridgeInitialized, bridgeSecret);
                    
                    // init android Exec again
                    if (!isBridgeInitialized) {
                        console.log('[ServoWS] Initializing Cordova exec with bridge secret');
                        isBridgeInitialized = true;
                        exec.init();
                        nativeApi.setNativeToJsBridgeMode(0, 0);
                    }

                    // The queued messages need the new bridge secret
                    for (let i = 0; i < sendQueue.length; i++) {
                        let queuedMsg = sendQueue[i];
                        let execRequest = JSON.parse(queuedMsg);
                        execRequest.bridgeSecret = bridgeSecret;
                        sendQueue[i] = JSON.stringify(execRequest);
                    }

                    // Flush queued messages

                    flushSendQueue();
                } else {
                    console.error('[ServoWS] Failed to get bridge secret from native');
                }
               
                return;
            }

            // Store incoming message for processing
            recieveQueue.push(message);
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
                var requestBridgeSecrete = gapData[0];
                var service = gapData[1];
                var action = gapData[2];
                var callbackId = gapData[3];
                var argsJson = message;

                // Create exec request for WebSocket
                var execRequest = {
                    bridgeSecret: requestBridgeSecrete,
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

                // Return null because we are polling only for the requests
                return null;
            }
            // gap_bridge_mode: prefix = set bridge mode
            else if (defaultValue.indexOf('gap_bridge_mode:') === 0) {
                console.info('[ServoWS] Bridge mode set to:', message, defaultValue);
                exec.setNativeToJsBridgeMode(0, bridgeSecret);
                return null;
            }
            // gap_poll: prefix = retrieve messages
            else if (defaultValue.indexOf('gap_poll:') === 0) {
                message = recieveQueue.shift();
                if (message !== undefined) {
                    console.debug('[ServoWS] Polling for messages', message);
                    return message;
                }
                return null;
            }
            // gap_init: prefix = initialize bridge
            else if (defaultValue.indexOf('gap_init:') === 0) {
                console.trace('[ServoWS] Bridge initialization', message, defaultValue, "Secret", bridgeSecret);
                if (bridgeSecret) {
                    return bridgeSecret;
                } else {
                    console.warn('[ServoWS] Bridge secret not yet available, Calling native to init and refresh exec');
                    ws.send(defaultValue);
                    return null;
                }
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
