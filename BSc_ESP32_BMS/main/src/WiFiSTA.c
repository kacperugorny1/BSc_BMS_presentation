#include "WiFiSTA.h"

static const char *TAG = "WIFISTA_MSG";
static int s_retry_num = 0;

// WiFi event handler
void wifi_event_handler(void *arg, esp_event_base_t event_base, int32_t event_id, void *event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        //multiple tries to connect
        if(s_retry_num < ESP_MAXIMUM_RETRY){
            esp_wifi_connect();
            s_retry_num++;
            ESP_LOGI(TAG, "%d try to connect", s_retry_num);
        }
        else{
            xEventGroupClearBits(wifi_event_group, CONNECTED_BIT);
        }
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        xEventGroupSetBits(wifi_event_group, CONNECTED_BIT);
        s_retry_num = 0;
        ESP_LOGI(TAG, "Connected to WiFi");
    }
}

// WiFi initialization for station mode
void wifi_init_sta(void) {
    wifi_event_group = xEventGroupCreate();
    esp_netif_init();
    esp_event_loop_create_default();
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    esp_wifi_init(&cfg);

    esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL);
    esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_event_handler, NULL);
    wifi_config_t wifi_config = {
        .sta = {
            .ssid = WIFI_SSID,
            .password = WIFI_PASS,
        },
    };
  
    nvs_handle_t nvs_handle;
    esp_err_t ret = nvs_open("wifi_config", NVS_READWRITE, &nvs_handle);
    if (ret == ESP_OK) {
        size_t ssid_len = 0;
        size_t password_len = 0;
        size_t mqtt_len = 0;
        nvs_get_str(nvs_handle, "ssid", NULL, &ssid_len);
        nvs_get_str(nvs_handle, "password", NULL, &password_len);
        nvs_get_str(nvs_handle, "mqtt", NULL, &mqtt_len);

        if (ssid_len > 0 && password_len > 0) {
            char *ssid = malloc(ssid_len);
            char *password = malloc(password_len);
            mqtt = malloc(mqtt_len);

            nvs_get_str(nvs_handle, "ssid", ssid, &ssid_len);
            nvs_get_str(nvs_handle, "password", password, &password_len);
            nvs_get_str(nvs_handle, "mqtt", mqtt, &mqtt_len);
            wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
            ESP_ERROR_CHECK(esp_wifi_init(&cfg));
            esp_event_handler_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL);
            
            strlcpy((char *)wifi_config.sta.ssid, ssid, sizeof(wifi_config.sta.ssid));
            strlcpy((char *)wifi_config.sta.password, password, sizeof(wifi_config.sta.password));

            free(ssid);
            free(password);
        }
    }
    
    esp_wifi_set_mode(WIFI_MODE_STA);
    esp_wifi_set_config(ESP_IF_WIFI_STA, &wifi_config);
    esp_wifi_start();

    // Wait until connection is established
    EventBits_t bits = xEventGroupWaitBits(wifi_event_group, CONNECTED_BIT, pdFALSE, pdTRUE, 25000 / portTICK_PERIOD_MS);
    if (!(bits & CONNECTED_BIT)) {
        ESP_LOGI(TAG, "Failed to connect to WiFi");
    }
}
