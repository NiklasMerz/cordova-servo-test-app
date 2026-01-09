/**
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

// Wait for the deviceready event before using any of Cordova's device APIs.
// See https://cordova.apache.org/docs/en/latest/cordova/events/events.html#deviceready
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    // Cordova is now initialized. Have fun!

    console.log('Running cordova-' + cordova.platformId + '@' + cordova.version);
    document.getElementById('deviceready').classList.add('ready');

    setTimeout(() => {
        document.getElementById('deviceready').innerText = location.origin + " " + navigator.userAgent;
    
    
        const ws = new WebSocket('ws://localhost:5000/cordova-socket');

        ws.onopen = () => {
            console.log('✅ Connected to WebSocket server');
            ws.send('Hello Server');
            console.log('📤 Sent: Hello Server');
        };

        ws.onmessage = (event) => {
            console.log('📥 Received:', event.data);
        };

        ws.onerror = (error) => {
            console.error('❌ WebSocket error:', error);
        };

        ws.onclose = (event) => {
            console.log('🔌 Connection closed:', event.code, event.reason);
        };
    }, 3000);



    console.log(device.cordova);
    console.log(device.model);
    console.log(device.platform);
    console.log(device.uuid);
    console.log(device.version);

    /*
    Fingerprint.show({
        description: "Some biometric description"
    }, successCallback, errorCallback);
    */

    function successCallback() {
        alert("Authentication successful");
    }

    function errorCallback(error) {
        alert("Authentication invalid " + error.message);
    }
}
