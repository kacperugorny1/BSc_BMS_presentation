#include "../inc/mqtt.h"

int disc_finished = 0;
int subscribed = 0;
int finished = 0;



void connlost(void *context, char *cause)
{
	MQTTAsync client = (MQTTAsync)context;
	MQTTAsync_connectOptions conn_opts = MQTTAsync_connectOptions_initializer;
	int rc;

	printf("\nConnection lost\n");
	if (cause)
		printf("     cause: %s\n", cause);

	printf("Reconnecting\n");
	conn_opts.keepAliveInterval = 20;
	conn_opts.cleansession = 1;
	conn_opts.onSuccess = onConnect;
	conn_opts.onFailure = onConnectFailure;
	if ((rc = MQTTAsync_connect(client, &conn_opts)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to start connect, return code %d\n", rc);
		finished = 1;
	}
}


int msgarrvd(void *context, char *topicName, int topicLen, MQTTAsync_message *message)
{
    printf("\nMessage arrived\n");
    printf("     topic: %s\n", topicName);
    printf("   message: %.*s\n", message->payloadlen, (char*)message->payload);
    printf("       len: %d\n", message->payloadlen);
	if(memcmp(topicName,mac_address, topicLen) == 0){
		pthread_mutex_lock(&cloud_update_mutex);
		enqueue(mess_cloud,message->payload, message->payloadlen);
		pthread_cond_signal(&cloud_update_sig);
		pthread_mutex_unlock(&cloud_update_mutex);
	}
	else{
		for(int i = 0; i < topics_count; ++i){
			if(memcmp(topicName,topics[i], topicLen) == 0){
				pthread_mutex_lock(&mutexes[i]);
				enqueue(mess[i],message->payload, message->payloadlen);
				pthread_cond_signal(&conds[i]);
				pthread_mutex_unlock(&mutexes[i]);
				break;
			}
		}
	}
	
    MQTTAsync_freeMessage(&message);
    MQTTAsync_free(topicName);
    return 1;
}

void onDisconnectFailure(void* context, MQTTAsync_failureData* response)
{
	printf("Disconnect failed, rc %d\n", response->code);
	disc_finished = 1;
}

void onDisconnect(void* context, MQTTAsync_successData* response)
{
	printf("Successful disconnection\n");
	disc_finished = 1;
}

void onSubscribe(void* context, MQTTAsync_successData* response)
{
	printf("Subscribe succeeded\n");
	subscribed = 1;
}

void onSubscribeFailure(void* context, MQTTAsync_failureData* response)
{
	printf("Subscribe failed, rc %d\n", response->code);
	printf("%s\n", response->message);
	finished = 1;
}


void onConnectFailure(void* context, MQTTAsync_failureData* response)
{
	printf("Connect failed, rc %d\n", response->code);
	finished = 1;
}


void onConnect(void* context, MQTTAsync_successData* response)
{
	MQTTAsync client = (MQTTAsync)context;
	MQTTAsync_responseOptions opts = MQTTAsync_responseOptions_initializer;
	int rc;

	printf("Successful connection\n");
    for(int i = 0; i < topics_count; ++i){
        printf("Subscribing to topic %s\nfor client %s using QoS%d\n\n", topics[i], CLIENTID, QOS);
        opts.onSuccess = onSubscribe;
        opts.onFailure = onSubscribeFailure;
        opts.context = client;
        if ((rc = MQTTAsync_subscribe(client, topics[i], QOS, &opts)) != MQTTASYNC_SUCCESS)
        {
            printf("Failed to start subscribe, return code %d\n", rc);
            finished = 1;
        }

    }
}

void onConnectCloud(void* context, MQTTAsync_successData* response)
{
	MQTTAsync client = (MQTTAsync)context;
	MQTTAsync_responseOptions opts = MQTTAsync_responseOptions_initializer;
	int rc;

	printf("Successful connection\n");
	printf("Subscribing to topic %s\nfor client %s using QoS%d\n\n", mac_address, CLIENTID, QOS);
	opts.onSuccess = onSubscribe;
	opts.onFailure = onSubscribeFailure;
	opts.context = client;
	if ((rc = MQTTAsync_subscribe(client, mac_address, QOS, &opts)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to start subscribe, return code %d\n", rc);
		finished = 1;
	}

}
