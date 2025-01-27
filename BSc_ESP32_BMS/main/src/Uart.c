#include "Uart.h"


#define TX_PIN GPIO_NUM_4
#define RX_PIN GPIO_NUM_5


static const char *TAG = "UART";
static const int RX_BUF_SIZE = 1024;


void uart_configure()
{
    uart_config_t uart2_config = {
        .baud_rate = 115200,
        .data_bits = UART_DATA_8_BITS,
        .parity    = UART_PARITY_EVEN,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
    };

    uart_driver_install(UART, RX_BUF_SIZE * 2, 0, 0, NULL, 0);
    uart_param_config(UART, &uart2_config);
    uart_set_pin(UART, TX_PIN, RX_PIN, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE);
    ESP_LOGI(TAG, "Configured Uart ");
	
}
void uart_rx_task(void *arg){
    char* Rx_data = malloc(sizeof(char)*128);
    for(;;){
        int l = uart_read_bytes(UART_NUM_1, (void *)Rx_data, 9,10/portTICK_PERIOD_MS);
        if(l != 0){
            Payload_t* payload = malloc(sizeof(payload));
            payload->mess = malloc(sizeof(char)*(l+1));
            payload->len = l;
            memcpy(payload->mess, Rx_data, l);
            if (xQueueSend(toMqttMess, &payload, 0) != pdPASS) {
                ESP_LOGW(TAG, "Queue full, discarding message");
                free(payload->mess);
                free(payload);
            }
            continue;
        }
    }
}
void uart_tx_task(void *arg){
    for(;;){
        Payload_t* receviedData;
        char mess[100];
        if(xQueueReceive(toDevMess, &receviedData, 200/portTICK_PERIOD_MS) == pdTRUE){
            ESP_LOGI(TAG, "DEQUEUED DATA");
            char buf = 0;
            for(int i = 0; i < receviedData->len; ++i){
                if(i % 2 != 0){
                   mess[i/2] = hex_to_ascii(buf, receviedData->mess[i]);
                }else{
                    buf = receviedData->mess[i];
                }
            }
            uart_write_bytes(UART, mess, receviedData->len/2);
            free(receviedData->mess);
            free(receviedData);
        }
    }
    
}
