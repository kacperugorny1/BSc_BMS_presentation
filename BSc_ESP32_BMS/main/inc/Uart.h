#ifndef UART_H_
#define UART_H_

#include "driver/uart.h"
#include "driver/gpio.h"
#include "esp_log.h"
#include "Globals.h"

void uart_rx_task(void *arg);
void uart_tx_task(void *arg);
void uart_configure();

#endif