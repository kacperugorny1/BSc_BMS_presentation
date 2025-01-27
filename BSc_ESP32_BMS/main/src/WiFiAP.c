#include "WiFiAP.h"

static const char *TAG = "wifiAP_config";




// Start WiFi in Access Point mode
void wifi_init_softap(void) {
    // Ensure WiFi driver is deinitialized before setting up in AP mode
    ESP_ERROR_CHECK(esp_wifi_stop());
    ESP_ERROR_CHECK(esp_wifi_deinit());  // Deinitialize the WiFi to ensure no conflict

    // Initialize the TCP/IP stack
    ESP_ERROR_CHECK(esp_netif_init());

    // Create the default AP network interface
    esp_netif_create_default_wifi_ap();

    // Define the WiFi initialization configuration
    wifi_init_config_t wifi_init_config = WIFI_INIT_CONFIG_DEFAULT();

    // Initialize WiFi driver with the defined config
    ESP_ERROR_CHECK(esp_wifi_init(&wifi_init_config));

    // Configure WiFi settings for AP mode
    wifi_config_t wifi_config = {
        .ap = {
            .ssid = AP_SSID,
            .password = AP_PASS,
            .ssid_len = strlen(AP_SSID),
            .channel = 1,
            .max_connection = 4,
            .authmode = WIFI_AUTH_WPA_WPA2_PSK,
        },
    };

    // If no password is provided, set the AP to open authentication
    if (strlen(AP_PASS) == 0) {
        wifi_config.ap.authmode = WIFI_AUTH_OPEN;
    }

    // Set the WiFi mode to AP
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_AP));

    // Set the WiFi configuration for AP
    ESP_ERROR_CHECK(esp_wifi_set_config(ESP_IF_WIFI_AP, &wifi_config));

    // Start the WiFi driver
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_LOGI(TAG, "WiFi AP started with SSID: %s", AP_SSID);
}


// Http post handler
esp_err_t wifi_config_post_handler(httpd_req_t *req) {
    char content[150];
    int ret = httpd_req_recv(req, content, sizeof(content));
    if (ret <= 0) {
        return ESP_FAIL;
    }

    // Null-terminate the received content to ensure it's a proper string
    content[ret] = '\0';

    // Initialize buffers for SSID and password
    char ssid[32] = {0};
    char password[64] = {0};
    char mqtt_ip[50] = {0};

    if (sscanf(content, "ssid=%49[^&]&password=%49[^&]&mqtt=%49s", ssid, password, mqtt_ip) == 3) {
        ESP_LOGI(TAG, "SSID: %s", ssid);
        ESP_LOGI(TAG, "Password: %s", password);
        ESP_LOGI(TAG, "MQTT Address: %s", mqtt_ip);
    } else {
        ESP_LOGE(TAG, "Failed to parse input.\n");
        char *resp = "Falied to parse data - wrong format";
        httpd_resp_send(req, resp, strlen(resp));
    }

    nvs_handle_t nvs_handle;
    esp_err_t ret_status = nvs_open("wifi_config", NVS_READWRITE, &nvs_handle);
    if (ret_status == ESP_OK) {
        nvs_set_str(nvs_handle, "ssid", ssid);
        nvs_set_str(nvs_handle, "password", password);
        nvs_set_str(nvs_handle, "mqtt", mqtt_ip);
        nvs_commit(nvs_handle);
        nvs_close(nvs_handle);
    } else {
        ESP_LOGE(TAG, "Error opening NVS: %s", esp_err_to_name(ret));
    }

    // Proceed with storing credentials and attempting to connect
    wifi_config_t wifi_config = {
        .sta = {
            .ssid = "",
            .password = "",
        },
    };
    strncpy((char *)wifi_config.sta.ssid, ssid, sizeof(wifi_config.sta.ssid));
    strncpy((char *)wifi_config.sta.password, password, sizeof(wifi_config.sta.password));
 
    // Send response
    char resp[50];
    sprintf(resp, "RESTART, %s", macString);
    httpd_resp_send(req, resp, strlen(resp));
    
    vTaskDelay(pdMS_TO_TICKS(1000));  // Wait for 5 seconds

    esp_restart();

    return ESP_OK;
}

// Function to start the web server
esp_err_t start_webserver(void) {
    httpd_handle_t server = NULL;
    httpd_config_t config = HTTPD_DEFAULT_CONFIG();
    config.uri_match_fn = httpd_uri_match_wildcard;

    if (httpd_start(&server, &config) == ESP_OK) {
        httpd_uri_t wifi_config = {
            .uri = "/configure",
            .method = HTTP_POST,
            .handler = wifi_config_post_handler,
            .user_ctx = NULL
        };
        httpd_register_uri_handler(server, &wifi_config);
    }
    return ESP_OK;
}
