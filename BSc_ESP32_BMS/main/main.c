#include <stdio.h>
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"
#include "esp_mac.h"
#include "nvs_flash.h"
#include "esp_system.h"
#include "esp_http_server.h"
#include "driver/gpio.h"

#include "Globals.h"
#include "WiFiSTA.h"
#include "WiFiAP.h"
#include "Mqtt.h"
#include "Uart.h"


static const char *TAG = "MAIN_PROGRAM";


#define BOOT_BUTTON_GPIO GPIO_NUM_9
#define LED_WIFIAP_GPIO GPIO_NUM_21
#define LED_FREE GPIO_NUM_22
void mqtt_tx_task(void *arg);

void app_main(void) {
    uint8_t baseMac[6];
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    toDevMess = xQueueCreate(50, sizeof(Payload_t*));
    toMqttMess = xQueueCreate(50, sizeof(Payload_t*));
    
    esp_read_mac(baseMac, ESP_MAC_WIFI_STA);
    sprintf(macString,"%02x:%02x:%02x:%02x:%02x:%02x", baseMac[0], baseMac[1], baseMac[2], baseMac[3], baseMac[4], baseMac[5]);

    gpio_set_direction(BOOT_BUTTON_GPIO, GPIO_MODE_INPUT);
    gpio_pullup_en(BOOT_BUTTON_GPIO);  // Enable pull-up resistor if needed (depending on the button wiring)
    
    gpio_reset_pin(LED_WIFIAP_GPIO);
    gpio_pullup_dis(LED_WIFIAP_GPIO);
    gpio_pulldown_dis(LED_WIFIAP_GPIO);
    gpio_set_direction(LED_WIFIAP_GPIO, GPIO_MODE_OUTPUT);
    gpio_set_level(LED_WIFIAP_GPIO, 0);


    wifi_init_sta();

    if (!(xEventGroupGetBits(wifi_event_group) & CONNECTED_BIT) || gpio_get_level(BOOT_BUTTON_GPIO) == 0) {
        ESP_LOGI(TAG, "Starting in AP mode.");
        wifi_init_softap();
        start_webserver();
        gpio_set_level(LED_WIFIAP_GPIO, 1);
        return;
    }


    ESP_LOGI(TAG, "WLACZ MQTT: %s", mqtt);
    ESP_LOGI(TAG, "MOJ MAC: %s", macString);
    
    uart_configure();
    mqtt_app_start();
    xTaskCreate(uart_tx_task, "uart_tx_task", 4096, NULL, 10, NULL);
    xTaskCreate(mqtt_tx_task, "mqtt_tx_task", 4096, NULL, 10, NULL);
    xTaskCreate(uart_rx_task, "uart_rx_task", 4096, NULL, 10, NULL);

}

void mqtt_tx_task(void *arg){
    char* MqttTransfer = malloc(sizeof(char)*(128 + 4));
    Payload_t* receviedData;
    for(;;){
        if(xQueueReceive(toMqttMess, &receviedData, 200/portTICK_PERIOD_MS) == pdTRUE){
            if(receviedData->len == 1)
                sprintf(MqttTransfer,"ED:%02X",receviedData->mess[0]);
            else if(receviedData->len == 8)
                sprintf(MqttTransfer, "ED:%02X%02X:%02X%02X:%02X%02X:%02X%02X",receviedData->mess[0],receviedData->mess[1],receviedData->mess[2],receviedData->mess[3],receviedData->mess[4],receviedData->mess[5],receviedData->mess[6],receviedData->mess[7]);
            else if(receviedData->len == 9)
                sprintf(MqttTransfer, "ED:%02X:%02X%02X:%02X%02X:%02X%02X:%02X%02X",receviedData->mess[0],receviedData->mess[1],receviedData->mess[2],receviedData->mess[3],receviedData->mess[4],receviedData->mess[5],receviedData->mess[6],receviedData->mess[7],receviedData->mess[8]);
            else continue;
            if(receviedData->len == 9)
                esp_mqtt_client_publish(client, macString, MqttTransfer, 0, 1, 1);
            else
                esp_mqtt_client_publish(client, macString, MqttTransfer, 0, 1, 0);
            ESP_LOGI(TAG, "Data sent: ");
            free(receviedData->mess);
            free(receviedData);
        }
    }
}