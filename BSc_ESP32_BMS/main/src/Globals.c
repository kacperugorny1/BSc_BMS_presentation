
#include "Globals.h"

char macString[19] = "";
EventGroupHandle_t wifi_event_group;
const int CONNECTED_BIT = BIT0;
char *mqtt;
esp_mqtt_client_handle_t client;
QueueHandle_t toDevMess;
QueueHandle_t toMqttMess;


int hex_to_int(char c){
        int first = c / 16 - 3;
        int second = c % 16;
        int result = first*10 + second;
        if(result > 9) result--;
        return result;
}

int hex_to_ascii(char c, char d){
        int high = hex_to_int(c) * 16;
        int low = hex_to_int(d);
        return high+low;
}