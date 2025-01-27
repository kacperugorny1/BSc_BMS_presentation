#ifndef WIFISTA_H_
#define WIFISTA_H_

#include <stdio.h>
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"
#include "nvs_flash.h"
#include "Globals.h"

#define WIFI_SSID "your_default_ssid"
#define WIFI_PASS "your_default_password"
#define ESP_MAXIMUM_RETRY 5



void wifi_init_sta(void);
void wifi_event_handler(void *arg, esp_event_base_t event_base, int32_t event_id, void *event_data);

#endif