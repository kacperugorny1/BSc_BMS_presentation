#ifndef GLOBALS_H_
#define GLOBALS_H_

#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "mqtt_client.h"

#define UART UART_NUM_1
extern char macString[];
extern EventGroupHandle_t wifi_event_group;
extern const int CONNECTED_BIT;
extern char *mqtt;
extern QueueHandle_t toDevMess;
extern QueueHandle_t toMqttMess;

struct{
    char* mess;
    int len;
}typedef Payload_t;


int hex_to_int(char c);
int hex_to_ascii(char c, char d);

extern esp_mqtt_client_handle_t client;

#endif // GLOBALS_H
