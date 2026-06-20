#include <ESP32Servo.h>
#include <WiFi.h>
#include <WiFiManager.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"

#define DATABASE_URL "https://YOUR-PROJECT-ID.firebasedatabase.app"
#define FIREBASE_PROJECT_ID "YOUR-PROJECT-ID"
#define FIREBASE_CLIENT_EMAIL "firebase-adminsdk-xxxxx@YOUR-PROJECT-ID.iam.gserviceaccount.com"

const char FIREBASE_PRIVATE_KEY[] PROGMEM = "-----BEGIN PRIVATE KEY-----\nYOUR_PRIVATE_KEY_HERE\n-----END PRIVATE KEY-----\n";

const int SERVO_PIN = 13;
const int TRIG_LEVEL = 5;
const int ECHO_LEVEL = 18;
const int TRIG_PROX = 19;
const int ECHO_PROX = 21;
const int LED_RED = 25;
const int LED_YELLOW = 26;
const int LED_GREEN = 27;
const int BUZZER_PIN = 23;
const int FLAME_PIN = 4;
const int GAS_PIN = 34;
const int TRIGGER_PIN = 0; 

int cfg_triggerDistance = 20;      
int cfg_openDuration = 4000;       
int cfg_openAngle = 90;            
int cfg_redThresh = 80;            
int cfg_yellowThresh = 50;         
bool cfg_beepOpen = true;          
bool cfg_beepFull = true;          
bool cfg_beepGas = true;           

const int BIN_HEIGHT_CM = 30; 
const unsigned long INTERVAL_FULL_BEEP = 1800000; 
const unsigned long INTERVAL_GAS_BEEP = 600000;   

Servo lidServo;
bool isOpen = false;
unsigned long lidOpenedAt = 0;
int trashPercentage = 0;

unsigned long fullBeepTimer = 0;
unsigned long gasBeepTimer = 0;
unsigned long lastFlameToggleTime = 0;
unsigned long lastCloudSync = 0;
unsigned long lastSettingsSync = 0; 
unsigned long lastFlameTime = 0; 

bool flameBuzzerState = false;
bool fullCooldownActive = false;
bool gasCooldownActive = false;
bool isOnline = false;

bool isFlameAlert = false;
bool isGasAlert = false;

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;
String binID;
String customBinName; 

void sendPushNotification(String title, String message) {
  if (!isOnline) return;
  
  Serial.println("Изпращане на Push известие: " + title);

  FCM_HTTPv1_JSON_Message fcmMsg;
  fcmMsg.topic = "bin_" + binID; 
  fcmMsg.notification.title = title;
  fcmMsg.notification.body = message;
  fcmMsg.android.priority = "high";

  if (Firebase.FCM.send(&fbdo, &fcmMsg)) {
    Serial.println("УСПЕХ: Известието е изпратено към телефона!");
  } else {
    Serial.println("ГРЕШКА при изпращане на известие: " + fbdo.errorReason());
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(TRIG_LEVEL, OUTPUT);
  pinMode(ECHO_LEVEL, INPUT);
  pinMode(TRIG_PROX, OUTPUT);
  pinMode(ECHO_PROX, INPUT);
  pinMode(LED_RED, OUTPUT);
  pinMode(LED_YELLOW, OUTPUT);
  pinMode(LED_GREEN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(FLAME_PIN, INPUT);
  pinMode(TRIGGER_PIN, INPUT_PULLUP);

  lidServo.setPeriodHertz(50);
  lidServo.attach(SERVO_PIN, 500, 2400);
  lidServo.write(0); 
  delay(500);
  lidServo.detach(); 

  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  delay(100);

  String mac = WiFi.macAddress();
  mac.replace(":", "");
  binID = "SmartBin-" + mac.substring(6);
  customBinName = binID; 
  
  WiFiManager wm;
  wm.setConfigPortalTimeout(60); 
  
  Serial.println("Опит за свързване с Wi-Fi...");
  if (!wm.autoConnect(binID.c_str(), "12345678")) {
    Serial.println("ОФЛАЙН режим!");
    isOnline = false;
  } else {
    Serial.println("Свързан към интернет! Firebase старт...");
    isOnline = true;

    config.database_url = DATABASE_URL;
    config.service_account.data.client_email = FIREBASE_CLIENT_EMAIL;
    config.service_account.data.project_id = FIREBASE_PROJECT_ID;
    config.service_account.data.private_key = FIREBASE_PRIVATE_KEY;

    fbdo.setBSSLBufferSize(4096, 1024);
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);

    FirebaseJson json;
    json.set("triggerDistance", cfg_triggerDistance);
    json.set("openAngle", cfg_openAngle);
    json.set("openDuration", cfg_openDuration);
    json.set("redThresh", cfg_redThresh);
    json.set("yellowThresh", cfg_yellowThresh);
    json.set("beepOpen", cfg_beepOpen);
    json.set("beepFull", cfg_beepFull);
    json.set("beepGas", cfg_beepGas);
    Firebase.RTDB.setJSON(&fbdo, "/bins/" + binID + "/settings", &json);
  }
}

long readDistance(int trig, int echo) {
  digitalWrite(trig, LOW); delayMicroseconds(2);
  digitalWrite(trig, HIGH); delayMicroseconds(10);
  digitalWrite(trig, LOW);
  long duration = pulseIn(echo, HIGH, 30000); 
  if (duration == 0) return 999; 
  return duration * 0.034 / 2;
}

void beep(int times, int duration) {
  for (int i = 0; i < times; i++) {
    digitalWrite(BUZZER_PIN, HIGH);
    delay(duration);
    digitalWrite(BUZZER_PIN, LOW);
    if (i < times - 1) delay(100); 
  }
}

void loop() {
  if (digitalRead(TRIGGER_PIN) == LOW) {
    WiFiManager wm;
    wm.resetSettings();
    ESP.restart(); 
  }

  unsigned long currentMillis = millis();

  if (isOnline && Firebase.ready() && (currentMillis - lastCloudSync >= 3000)) {
    lastCloudSync = currentMillis;
    
    Firebase.RTDB.setInt(&fbdo, "/bins/" + binID + "/trashPercentage", trashPercentage);
    Firebase.RTDB.setBool(&fbdo, "/bins/" + binID + "/flameAlert", isFlameAlert);
    Firebase.RTDB.setBool(&fbdo, "/bins/" + binID + "/gasAlert", isGasAlert);
    
    if (Firebase.RTDB.getInt(&fbdo, "/bins/" + binID + "/remoteCommand") && fbdo.intData() == 1 && !isOpen) {
      if (cfg_beepOpen) beep(1, 150);
      lidServo.attach(SERVO_PIN, 500, 2400);
      lidServo.write(cfg_openAngle); 
      isOpen = true;
      lidOpenedAt = currentMillis;
      Firebase.RTDB.setInt(&fbdo, "/bins/" + binID + "/remoteCommand", 0); 
    }
  }

  if (isOnline && Firebase.ready() && (currentMillis - lastSettingsSync >= 15000)) {
    lastSettingsSync = currentMillis;
    
    if (Firebase.RTDB.getString(&fbdo, "/bins/" + binID + "/name")) {
      if (fbdo.stringData() != "") customBinName = fbdo.stringData();
    }

    if (Firebase.RTDB.getJSON(&fbdo, "/bins/" + binID + "/settings")) {
      FirebaseJson &json = fbdo.jsonObject();
      FirebaseJsonData data;
      
      if (json.get(data, "triggerDistance")) cfg_triggerDistance = data.intValue;
      if (json.get(data, "openAngle"))       cfg_openAngle = data.intValue;
      if (json.get(data, "openDuration"))    cfg_openDuration = data.intValue;
      if (json.get(data, "redThresh"))       cfg_redThresh = data.intValue;
      if (json.get(data, "yellowThresh"))    cfg_yellowThresh = data.intValue;
      if (json.get(data, "beepOpen"))        cfg_beepOpen = data.boolValue;
      if (json.get(data, "beepFull"))        cfg_beepFull = data.boolValue;
      if (json.get(data, "beepGas"))         cfg_beepGas = data.boolValue;
    }
  }

  if (!isOpen) {
    long proxDistance = readDistance(TRIG_PROX, ECHO_PROX);
    if (proxDistance > 0 && proxDistance <= cfg_triggerDistance) { 
      if (cfg_beepOpen) beep(1, 150); 
      lidServo.attach(SERVO_PIN, 500, 2400);
      lidServo.write(cfg_openAngle); 
      isOpen = true;
      lidOpenedAt = currentMillis;
    }
  }

  if (isOpen && (currentMillis - lidOpenedAt >= cfg_openDuration)) { 
    lidServo.write(0); 
    delay(500); 
    lidServo.detach(); 
    isOpen = false;
    delay(1000); 
  }

  bool currentFlameReading = (digitalRead(FLAME_PIN) == HIGH);

  if (currentFlameReading) { 
    if (!isFlameAlert) {
      sendPushNotification("🔥 WARNING: Flame detected!", "Flame detected in bin: " + customBinName);
    }
    
    isFlameAlert = true;
    lastFlameTime = currentMillis; 
    
    if (currentMillis - lastFlameToggleTime >= (flameBuzzerState ? 400 : 200)) {
      lastFlameToggleTime = currentMillis;
      flameBuzzerState = !flameBuzzerState;
      digitalWrite(BUZZER_PIN, flameBuzzerState ? HIGH : LOW);
    }
  } else {
    if (flameBuzzerState) {
      digitalWrite(BUZZER_PIN, LOW);
      flameBuzzerState = false;
    }

    if (isFlameAlert && (currentMillis - lastFlameTime >= 5000)) {
      isFlameAlert = false;
    }

    int currentGasReading = analogRead(GAS_PIN);
    
    if (currentGasReading > 300) {
      if (!isGasAlert) {
        sendPushNotification("🤢 Warning: Bad smell!", "Bad smell detected in bin: " + customBinName);
      }
      isGasAlert = true;
    } else if (currentGasReading < 250) {
      isGasAlert = false;
    }

    if (!gasCooldownActive) {
      if (isGasAlert) {
        if (cfg_beepGas) beep(3, 150); 
        gasBeepTimer = currentMillis;
        gasCooldownActive = true;
      }
    } else {
      if (currentMillis - gasBeepTimer >= INTERVAL_GAS_BEEP) {
        if (isGasAlert) {
          if (cfg_beepGas) beep(3, 150);
          gasBeepTimer = currentMillis;
        } else {
          gasCooldownActive = false;
        }
      }
    }

    bool isFullNow = (trashPercentage >= cfg_redThresh);
    if (!fullCooldownActive) {
      if (isFullNow) {
        sendPushNotification("🗑️ Bin is full!", "Please empty the bin: " + customBinName);
        if (cfg_beepFull) beep(2, 150); 
        fullBeepTimer = currentMillis;
        fullCooldownActive = true;
      }
    } else {
      if (currentMillis - fullBeepTimer >= INTERVAL_FULL_BEEP) {
        if (isFullNow) {
          sendPushNotification("🗑️ Bin is still full!", "Please empty the bin: " + customBinName);
          if (cfg_beepFull) beep(2, 150);
          fullBeepTimer = currentMillis;
        } else {
          fullCooldownActive = false;
        }
      }
    }
  }

  if (!isOpen) {
    long levelDistance = readDistance(TRIG_LEVEL, ECHO_LEVEL);
    if (levelDistance != 999) {
      if (levelDistance > BIN_HEIGHT_CM) levelDistance = BIN_HEIGHT_CM;
      
      int newPercentage = ((BIN_HEIGHT_CM - levelDistance) * 100) / BIN_HEIGHT_CM;
      if (newPercentage < 0) newPercentage = 0;
      if (newPercentage > 100) newPercentage = 100;
      
      trashPercentage = (trashPercentage * 3 + newPercentage) / 4;

      digitalWrite(LED_GREEN, LOW);
      digitalWrite(LED_YELLOW, LOW);
      digitalWrite(LED_RED, LOW);

      if (trashPercentage >= cfg_redThresh) digitalWrite(LED_RED, HIGH); 
      else if (trashPercentage >= cfg_yellowThresh) digitalWrite(LED_YELLOW, HIGH); 
      else digitalWrite(LED_GREEN, HIGH); 
    }
    delay(50); 
  }
}