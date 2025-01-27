#ifndef MQTT_H
#define MQTT_H

#include <stdio.h> 
#include <stdlib.h> 
#include <string.h>
#include <unistd.h>

#include "globals.h"


#include <MQTTAsync.h>

#define ADDRESS     "mqtt://localhost:1883"
#define CLIENTID    "LogicProcess"
#define QOS         1
#define TIMEOUT     10000L


extern int disc_finished;
extern int subscribed;
extern int finished;


void onConnect(void* context, MQTTAsync_successData* response);
void onConnectFailure(void* context, MQTTAsync_failureData* response);

void onDisconnect(void* context, MQTTAsync_successData* response);
void onDisconnectFailure(void* context, MQTTAsync_failureData* response);

int msgarrvd(void *context, char *topicName, int topicLen, MQTTAsync_message *message);
void connlost(void *context, char *cause);

#endif