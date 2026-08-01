#include <WiFi.h>
#include <WebServer.h>
#include <WiFiUdp.h>
#include <HijelHID_BLEKeyboard.h>
#include <HTTPClient.h>
#include <WiFiClientSecure.h>
#include <HTTPUpdate.h>
#include <ArduinoJson.h>

// WiFi networks (will try each one until connected)
struct WiFiNetwork {
    const char* ssid;
    const char* password;
};

const WiFiNetwork wifiNetworks[] = {
    {"Gold", "0527707295"},
    {"Safridim", "0544409298"}
};
const int NUM_NETWORKS = sizeof(wifiNetworks) / sizeof(wifiNetworks[0]);

const int udpPort = 4210;
const char* udpMessage = "HID_BOT_DISCOVER";
const char* udpResponse = "HID_BOT_HERE";

const String CURRENT_VERSION = "1.3.0";
const String VERSION_URL = "https://raw.githubusercontent.com/avi682/hid-bot-app/main/firmware/esp32_version.json";

WebServer server(80);
WiFiUDP udp;
HijelHID_BLEKeyboard keyboard("ESP32 HID Bot", "Avi", 100);

bool isRunning = false;
bool isHebrewMode = false;
unsigned long sequenceStartTime = 0;

void checkForUpdate() {
    Serial.println("Checking for updates...");
    WiFiClientSecure client;
    client.setInsecure(); // GitHub raw content requires HTTPS
    
    HTTPClient http;
    http.setFollowRedirects(HTTPC_STRICT_FOLLOW_REDIRECTS);
    if (http.begin(client, VERSION_URL)) {
        int httpCode = http.GET();
        if (httpCode == 200) {
            String payload = http.getString();
            JsonDocument doc;
            DeserializationError error = deserializeJson(doc, payload);
            
            if (!error) {
                const char* latestVersion = doc["version"];
                const char* binUrl = doc["binUrl"];
                
                if (String(latestVersion) != CURRENT_VERSION) {
                    String firmwareUrl = String(binUrl);
                    Serial.println("New version found: " + String(latestVersion));
                    Serial.println("Downloading from: " + firmwareUrl);
                    // Close the current HTTP connection before starting OTA
                    http.end(); 
                    
                    // Create a fresh client for the OTA download
                    WiFiClientSecure otaClient;
                    otaClient.setInsecure();
                    
                    httpUpdate.setFollowRedirects(HTTPC_STRICT_FOLLOW_REDIRECTS);
                    t_httpUpdate_return ret = httpUpdate.update(otaClient, firmwareUrl);
                    switch (ret) {
                        case HTTP_UPDATE_FAILED:
                            Serial.printf("HTTP_UPDATE_FAILED Error (%d): %s\n", httpUpdate.getLastError(), httpUpdate.getLastErrorString().c_str());
                            break;
                        case HTTP_UPDATE_NO_UPDATES:
                            Serial.println("HTTP_UPDATE_NO_UPDATES");
                            break;
                        case HTTP_UPDATE_OK:
                            Serial.println("HTTP_UPDATE_OK");
                            ESP.restart();
                            break;
                    }
                    return;
                } else {
                    Serial.println("Already up to date.");
                }
            } else {
                Serial.println("JSON Parse Error");
            }
        } else {
            Serial.printf("HTTP GET failed, error: %s\n", http.errorToString(httpCode).c_str());
        }
        http.end();
    } else {
        Serial.println("Unable to connect to Github.");
    }
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  WiFi.mode(WIFI_STA);
  
  // Try each configured WiFi network
  bool connected = false;
  while (!connected) {
      for (int i = 0; i < NUM_NETWORKS; i++) {
          Serial.printf("Trying WiFi: %s...\n", wifiNetworks[i].ssid);
          WiFi.begin(wifiNetworks[i].ssid, wifiNetworks[i].password);
          
          // Wait up to 10 seconds for connection
          int attempts = 0;
          while (WiFi.status() != WL_CONNECTED && attempts < 20) {
              delay(500);
              Serial.print(".");
              attempts++;
          }
          
          if (WiFi.status() == WL_CONNECTED) {
              Serial.printf("\nConnected to %s! IP: %s\n", wifiNetworks[i].ssid, WiFi.localIP().toString().c_str());
              connected = true;
              break;
          } else {
              Serial.printf("\nFailed to connect to %s\n", wifiNetworks[i].ssid);
              WiFi.disconnect();
              delay(500);
          }
      }
      if (!connected) {
          Serial.println("No networks found, retrying in 5 seconds...");
          delay(5000);
      }
  }
  
  udp.begin(udpPort);
  
  // Initialize BLE Keyboard
  keyboard.begin();

  server.on("/ping", HTTP_GET, []() {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      server.send(200, "application/json", "{\"status\":\"ok\",\"device\":\"ESP32-C3 HID Bot\",\"version\":\"" + CURRENT_VERSION + "\"}");
  });
  server.on("/status", HTTP_GET, []() {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      String json = "{\"running\":";
      json += (isRunning ? "true" : "false");
      json += "}";
      server.send(200, "application/json", json);
  });
  server.on("/start", HTTP_POST, []() {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      if (isRunning) {
          server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"Already running\"}");
      } else {
          isRunning = true;
          isHebrewMode = (server.arg("hebrew") == "true");
          sequenceStartTime = millis();
          server.send(200, "application/json", "{\"status\":\"ok\"}");
      }
  });
  server.on("/stop", HTTP_POST, []() {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      isRunning = false;
      server.send(200, "application/json", "{\"status\":\"ok\"}");
  });
  server.on("/check_update", HTTP_POST, []() {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      server.send(200, "application/json", "{\"status\":\"ok\",\"message\":\"Checking for updates in background\"}");
      // Run in background by setting a flag or just running it now
      // Calling it directly might block the server for a few seconds, which is fine since OTA stops everything anyway.
      checkForUpdate();
  });
  
  server.on("/start", HTTP_OPTIONS, []() {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      server.sendHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
      server.send(204);
  });
  server.on("/stop", HTTP_OPTIONS, []() {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      server.sendHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
      server.send(204);
  });
  server.on("/check_update", HTTP_OPTIONS, []() {
      server.sendHeader("Access-Control-Allow-Origin", "*");
      server.sendHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
      server.send(204);
  });

  server.begin();
}

void executeActionSequence() {
  Serial.println("Action sequence running...");
  if (keyboard.isPaired()) {
      if (isHebrewMode) {
          // Switch to English using Alt+Shift
          keyboard.press(0, KEY_MOD_LALT | KEY_MOD_LSHIFT);
          delay(50);
          keyboard.releaseAll();
          delay(200); // Wait for Windows to process layout change
      }
      
      // 1. Press Windows + R to open Run dialog
      keyboard.tap(KEY_R, KEY_MOD_LGUI);
      delay(500);
      
      // 2. Type URL
      keyboard.print("https://youtube.com");
      delay(200);
      
      // 3. Press Enter
      keyboard.tap(KEY_RETURN);
      
      if (isHebrewMode) {
          delay(500); // Wait for browser to open before switching back
          // Switch back to Hebrew using Alt+Shift
          keyboard.press(0, KEY_MOD_LALT | KEY_MOD_LSHIFT);
          delay(50);
          keyboard.releaseAll();
      }
  } else {
      Serial.println("Keyboard not paired! Cannot execute macro.");
  }
  
  delay(1000);
  isRunning = false; 
}

unsigned long lastBroadcastTime = 0;

void handleUDP() {
  // Broadcast our IP every 2 seconds so the app can find us
  if (millis() - lastBroadcastTime > 2000) {
    lastBroadcastTime = millis();
    String broadcastMessage = "ESP32BOT:" + WiFi.localIP().toString();
    udp.beginPacket(IPAddress(255, 255, 255, 255), 4210);
    udp.print(broadcastMessage);
    udp.endPacket();
  }
}

void loop() {
  server.handleClient();
  handleUDP();
  if (isRunning) { executeActionSequence(); }
}
