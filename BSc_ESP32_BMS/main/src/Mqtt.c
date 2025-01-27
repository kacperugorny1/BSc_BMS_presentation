#include "Mqtt.h"


static const char *TAG = "mqtt_config";

void mqtt_app_start(void)
{
    char addrr[64] = "mqtt://";
    strcat(addrr,mqtt);
    
    const esp_mqtt_client_config_t mqtt_cfg = {
        .broker.address.uri = addrr,
        .session.keepalive = 5,
        .session.last_will = {
            .topic = macString,
            .msg = "ED:OFFLINE",
            .qos = 1,
            .retain = true
        }
    };
    client = esp_mqtt_client_init(&mqtt_cfg);
    esp_mqtt_client_register_event(client, ESP_EVENT_ANY_ID, mqtt_event_handler, client);
    esp_mqtt_client_start(client);
}


void mqtt_event_handler(void *handler_args, esp_event_base_t base, int32_t event_id, void *event_data)
{
    ESP_LOGD(TAG, "Event dispatched from event loop base=%s, event_id=%ld", base, event_id);
    mqtt_event_handler_cb(event_data);
}

esp_err_t mqtt_event_handler_cb(esp_mqtt_event_handle_t event){ //here esp_mqtt_event_handle_t is a struct which receieves struct event from mqtt app start funtion
    esp_mqtt_client_handle_t client = event->client;
    switch (event->event_id)
    {
    case MQTT_EVENT_BEFORE_CONNECT:
        break;
    case MQTT_EVENT_CONNECTED:
        {
        ESP_LOGI(TAG, "MQTT_EVENT_CONNECTED");
        esp_mqtt_client_subscribe(client, macString, 1);
        }
        break;
    case MQTT_EVENT_DISCONNECTED:   
        ESP_LOGI(TAG, "MQTT_EVENT_DISCONNECTED");
        break;
    case MQTT_EVENT_SUBSCRIBED:
        ESP_LOGI(TAG, "MQTT_EVENT_SUBSCRIBED, msg_id=%d", event->msg_id);
        break;
    case MQTT_EVENT_UNSUBSCRIBED:
        ESP_LOGI(TAG, "MQTT_EVENT_UNSUBSCRIBED, msg_id=%d", event->msg_id);
        break;
    case MQTT_EVENT_PUBLISHED:
        ESP_LOGI(TAG, "MQTT_EVENT_PUBLISHED, msg_id=%d", event->msg_id);

        break;
    case MQTT_EVENT_DATA:
        ESP_LOGI(TAG, "MQTT_EVENT_DATA:");
        if(event->data[0] == 'E' && event->data[1] == 'D' && event->data[2] == ':') break;
        Payload_t* payload = malloc(sizeof(payload));
        payload->mess = malloc(sizeof(char)*event->data_len/2);
        memcpy(payload->mess, event->data, event->data_len);
        payload->len = event->data_len;
        if (xQueueSend(toDevMess, &payload, 0) != pdPASS) {
            ESP_LOGW(TAG, "Queue full, discarding message");
            free(payload->mess);
            free(payload);
        }
        break;
    case MQTT_EVENT_ERROR:
        ESP_LOGI(TAG, "MQTT_EVENT_ERROR");
        break;
    default:
        ESP_LOGI(TAG, "Other event id:%d", event->event_id);
        break;
    }
    return ESP_OK;
}
