#ifndef WIFIAP_H_
#define WIFIAP_H_

#include <stdio.h>
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"
#include "nvs_flash.h"
#include "esp_http_server.h"
#include "Globals.h"



#define AP_SSID "BMS_END_DEVICE"
#define AP_PASS "configbms"

// Function declarations
esp_err_t start_webserver(void);
void stop_webserver(httpd_handle_t server);
esp_err_t wifi_config_post_handler(httpd_req_t *req);
void wifi_init_softap(void);

#endif