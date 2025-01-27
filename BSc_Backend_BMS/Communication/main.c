#define _POSIX_C_SOURCE 200112L
#include <stdio.h> 
#include <stdlib.h> 
#include <string.h>
#include <stdbool.h>
#include <stdint.h>
#include <math.h>
#include <time.h>

#include <unistd.h>

#include <pthread.h>
#include <mariadb/mysql.h>
#include <MQTTAsync.h>
#include "inc/queue.h"
#include "inc/mqtt.h"
#include "inc/mysql.h"
#include "inc/globals.h"

MYSQL *conn;

MQTTAsync client_local;
MQTTAsync client_cloud;
MQTTAsync_responseOptions pub_opts_local = MQTTAsync_responseOptions_initializer;
MQTTAsync_responseOptions pub_opts_cloud = MQTTAsync_responseOptions_initializer;

pthread_cond_t cloud_update_sig;
pthread_mutex_t cloud_update_mutex;

int *dig_val;
int **analog_val;

char mac_address[18];


//Handler - saves data to dig_val and analog_val, sends updates to clouds mqtt
void *handleLocalTopic(void* args){
	
	MQTTAsync_message pubmsg = MQTTAsync_message_initializer;
	pubmsg.qos = MQTTREASONCODE_GRANTED_QOS_1;
	pubmsg.retained = 1;
    long id = (long)args;
	char* msg;
	
	// "LOC:D1:BYT2BYT2BYT2BYT2BYT"
	char payload_cloud[4 + 13 * topics_count + 1];
	
	// "LOC:D1:BYT2BYT2BYT2BYT2BYT"
	char payload_cloud_prev[4 + 13 * topics_count + 1];


	while(1){
		pthread_mutex_lock(&mutexes[id]);
		while(mess[id]->front == NULL){
			pthread_cond_wait(&conds[id], &mutexes[id]);
		}
		printf("MESSAGE_HANDLE thread_id=%d\n",id);
		msg = dequeue(mess[id]);
		pthread_mutex_unlock(&mutexes[id]);
		
		if(strcmp(msg, "ED:OFFLINE") == 0){
			dev_status[id] = false;
		}
		else if(strlen(msg) != 25){
			free(msg);
			continue;
		}
		else dev_status[id] = true;

		
		

		dig_val[id] = (int)strtol(&msg[3],NULL,16);
		for(int i = 0; i < 4; ++i){
			analog_val[id][i] = strtol(&msg[6 + i*5],NULL,16);
		}



		strcpy(payload_cloud, "LOC:");
    
		// Offset for appending data
		int offset = strlen(payload_cloud);
		
		for (int i = 0; i < topics_count; ++i) {
			if(dev_status[i])
			// Append each formatted string to payload_cloud without overwriting the null terminator
        		offset += snprintf(payload_cloud + offset,  // Start writing from the current offset
					13 * topics_count + 1,      // Maximum space to write (ensure buffer won't overflow)
					"D%d:%c%c%c%c%c%c%c%c%c", 
					IDs[i], 
					dig_val[i], 
					(analog_val[i][0] >> 8) & 0xFF, 
					(analog_val[i][0]) & 0xFF, 
					(analog_val[i][1] >> 8) & 0xFF, 
					(analog_val[i][1]) & 0xFF, 
					(analog_val[i][2] >> 8) & 0xFF, 
					(analog_val[i][2]) & 0xFF, 
					(analog_val[i][3] >> 8) & 0xFF, 
					(analog_val[i][3]) & 0xFF);
			else
				offset += snprintf(payload_cloud + offset,
					13 * topics_count + 1, // Ensure enough space
					"D%d:OFFLINE",
					IDs[i]);
    	}
		if(memcmp(payload_cloud,payload_cloud_prev,offset) == 0){
			free(msg);
			continue;
		}
		pubmsg.payloadlen = offset;
		pubmsg.payload = payload_cloud;
        MQTTAsync_sendMessage(client_cloud, mac_address, &pubmsg, &pub_opts_cloud);
		memcpy(payload_cloud_prev,payload_cloud,offset);
		free(msg);
	}

}

//Handler - Handles messages from cloud mqtt -> redirects them to specific device mqtt topic on local 
//will implement adding new devices and resetting the logic process
void *handleCloudTopic(void* args){
	char newMac[18],Lside[255], Rside[255], newName[255]; 
	MQTTAsync_message pubmsg = MQTTAsync_message_initializer;
	pubmsg.qos = MQTTREASONCODE_GRANTED_QOS_1;
	char *token;
	char* endptr;
	int d = -1;
	int id = -1;
	while(1){
		pthread_mutex_lock(&cloud_update_mutex);
		while(mess_cloud->front == NULL){
			pthread_cond_wait(&cloud_update_sig, &cloud_update_mutex);
		}
		char *str = dequeue(mess_cloud);
		printf("HANDLE MESSAGE CLOUD:\nPAYLOAD: %s\n", str);
		if(strcmp(str, "LOC:OFFLINE") == 0){
			pubmsg.retained = 1;
			pubmsg.payload = &"LOC:ONLINE";
			pubmsg.payloadlen = 10;
			MQTTAsync_sendMessage(client_cloud, mac_address, &pubmsg, &pub_opts_cloud);
			pubmsg.retained = 0;
		}
		else if(strcmp(str,"LOC:") == 0){
			
		}
		else if(str[0] == 'A' && str[1] == 'D'){
			token = strtok(&str[3],";");
			strcpy(newMac,token);
			token = strtok(NULL,";");
			strcpy(newName,token);
			
			if(insert_device(conn,newMac,newName)){
				char* state = sync_dev(conn);
				pubmsg.retained = 0;
				pubmsg.payload = state;
				pubmsg.payloadlen = strlen(state);
				MQTTAsync_sendMessage(client_cloud, mac_address, &pubmsg, &pub_opts_cloud);
				free(state);
			}	
			else printf("falied to insert\n");
			
    		system("sudo systemctl restart logic.service");
    		system("sudo systemctl restart comms.service");
		}
		else if(str[0] == 'E' && str[1] == 'D'){
			token = strtok(&str[3], ";");
			id = strtol(token,NULL,10);
			token = strtok(NULL, ";");
			strcpy(newName,token);

			if(edit_device(conn,id,newName)){
			}	
			else printf("falied to edit\n");
			
		}
		else if(str[0] == 'R' && str[1] == 'D'){
			id = strtol(&str[3], NULL, 10);
			
			if(remove_device(conn, id)){
				
			}	
			else printf("falied to insert\n");
			
    		system("sudo systemctl restart logic.service");
    		system("sudo systemctl restart comms.service");
		}
		else if(str[0] == 'R' && str[1] == 'C'){
			id = strtol(&str[3],NULL,10);
			
			if(remove_cond(conn,id)){

			}
			else printf("falied to remove\n");
    		system("sudo systemctl restart logic.service");
		}
		else if(str[0] == 'E' && str[1] == 'C'){
			token = strtok(&str[3], ";");
			id = strtol(token,NULL,10);
			token = strtok(NULL, ";");
			strcpy(newName,token);
			token = strtok(NULL, ";");
			strcpy(Lside,token);
			token = strtok(NULL, ";");
			strcpy(Rside,token);
			token = strtok(NULL, ";");
			int enabled = strtol(token,NULL,10);

			if(edit_cond(conn,id,newName,Lside,Rside, enabled)){

			}
			else printf("falied to edit cond\n");
    		system("sudo systemctl restart logic.service");
		}
		else if(str[0] == 'A' && str[1] == 'C'){
			token = strtok(&str[3],";");
			strcpy(newName,token);
			token = strtok(NULL,";");
			strcpy(Lside,token);
			token = strtok(NULL,";");
			strcpy(Rside,token);
			if(insert_cond(conn,newName,Lside,Rside)){
				char* conds = sync_cond(conn);
				pubmsg.retained = 0;
				pubmsg.payload = conds;
				pubmsg.payloadlen = strlen(conds);
				MQTTAsync_sendMessage(client_cloud, mac_address, &pubmsg, &pub_opts_cloud);
				free(conds);
			}
			else printf("falied to insert\n");
    		system("sudo systemctl restart logic.service");
		}
		else if(strcmp(str,"SYNCDEV") == 0){
			char* state = sync_dev(conn);
			pubmsg.retained = 0;
			pubmsg.payload = state;
			pubmsg.payloadlen = strlen(state);
			MQTTAsync_sendMessage(client_cloud, mac_address, &pubmsg, &pub_opts_cloud);
			free(state);
		}	
		else if(strcmp(str,"SYNCCOND") == 0){
			char* conds = sync_cond(conn);
			pubmsg.retained = 0;
			pubmsg.payload = conds;
			pubmsg.payloadlen = strlen(conds);
			MQTTAsync_sendMessage(client_cloud, mac_address, &pubmsg, &pub_opts_cloud);
			free(conds);
		}		

		else if(str[0] == 'D'){
			d = strtol(&str[1], &endptr, 10);
			for (int j = 0; j < topics_count; ++j) {
				if (IDs[j] != d)  continue;
				id = j;
				break;
			}
			printf("%s\n%d\n", &endptr[1], strlen(&endptr[1]));
			pubmsg.payload = &endptr[1];
			pubmsg.payloadlen = strlen(&endptr[1]);
			MQTTAsync_sendMessage(client_local, topics[id], &pubmsg, &pub_opts_local);
		}

		free(str);
		pthread_mutex_unlock(&cloud_update_mutex);
	
	}

}


//Configuration
int main(int argc, char* argv[])
{
	MQTTAsync_connectOptions conn_opts_local = MQTTAsync_connectOptions_initializer;
	MQTTAsync_disconnectOptions disc_opts_local = MQTTAsync_disconnectOptions_initializer;

	
	MQTTAsync_connectOptions conn_opts_cloud = MQTTAsync_connectOptions_initializer;
	MQTTAsync_disconnectOptions disc_opts_cloud = MQTTAsync_disconnectOptions_initializer;
	MQTTAsync_willOptions will_opts_cloud = MQTTAsync_willOptions_initializer;

	int rc;
	int ch;
	
    FILE *fp;

    pthread_t* threads;

	pthread_cond_init(&cloud_update_sig, NULL);
	pthread_mutex_init(&cloud_update_mutex, NULL);
	
	fp = fopen("/sys/class/net/eth0/address", "r");
    if (fp == NULL) {
        perror("Unable to open file");
        return 1;
    }
    // Read the MAC address from the file
    fgets(mac_address, sizeof(mac_address), fp);
    fclose(fp);




	
    //mysql
    if (!(conn = mysql_init(0)))
    {
        fprintf(stderr, "unable to initialize connection struct\n");
        exit(1);
    }

    // Connect to the database
    if (!mysql_real_connect(
            conn,                 // Connection
            "localhost",// Host
            "root",            // User account
            "8Z4HE)T({,401Om/",   // User password
            "BMS",               // Default database
            3306,                 // Port number
            NULL,                 // Path to socket file
            0                     // Additional options
        ))
    {
        // Report the failed-connection error & close the handle
        fprintf(stderr, "Error connecting to Server: %s\n", mysql_error(conn));
        mysql_close(conn);
        exit(1);
    }
    select_topics(conn);
	select_conds(conn);


	    //PTHREAD
    threads = malloc((topics_count + 1) * sizeof(pthread_t));
	pthread_create(&threads[0], NULL, handleCloudTopic, (void *)-1);
	dig_val = malloc(topics_count * sizeof(int));
	analog_val = malloc(topics_count * sizeof(int*));
    for(long i = 0; i < topics_count; ++i){
        pthread_create(&threads[i + 1], NULL, handleLocalTopic, (void *)i);
		analog_val[i] = malloc(4 * sizeof(int));
	}



    //MQTT
	if ((rc = MQTTAsync_create(&client_local, ADDRESS_LOCAL, CLIENTID, MQTTCLIENT_PERSISTENCE_NONE, NULL))
			!= MQTTASYNC_SUCCESS)
	{
		printf("Failed to create client_local, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto exit;
	}

	if ((rc = MQTTAsync_setCallbacks(client_local, client_local, connlost, msgarrvd, NULL)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to set callbacks, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto destroy_exit;
	}

	if ((rc = MQTTAsync_create(&client_cloud, ADDRESS_CLOUD, CLIENTID, MQTTCLIENT_PERSISTENCE_NONE, NULL))
			!= MQTTASYNC_SUCCESS)
	{
		printf("Failed to create client_cloud, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto exit;
	}

	if ((rc = MQTTAsync_setCallbacks(client_cloud, client_cloud, connlost, msgarrvd, NULL)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to set callbacks, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto destroy_exit;
	}

	conn_opts_local.keepAliveInterval = 20;
	conn_opts_local.cleansession = 1;
	conn_opts_local.onSuccess = onConnect;
	conn_opts_local.onFailure = onConnectFailure;
	conn_opts_local.context = client_local;
	if ((rc = MQTTAsync_connect(client_local, &conn_opts_local)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to start connect, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto destroy_exit;
	}

	conn_opts_cloud.keepAliveInterval = 20;
	conn_opts_cloud.cleansession = 1;
	conn_opts_cloud.onSuccess = onConnectCloud;
	conn_opts_cloud.onFailure = onConnectFailure;
	conn_opts_cloud.context = client_cloud;
	conn_opts_cloud.will = &will_opts_cloud;

	will_opts_cloud.qos = 1;
	will_opts_cloud.retained = 1;
	will_opts_cloud.message = "LOC:OFFLINE";
	will_opts_cloud.topicName = mac_address;

	if ((rc = MQTTAsync_connect(client_cloud, &conn_opts_cloud)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to start connect, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto destroy_exit;
	}




	while (!subscribed && !finished)
		#if defined(_WIN32)
			Sleep(100);
		#else
		
		sleep(10);
			
		#endif

	if (finished)
		goto exit;
	
	pub_opts_local.onSuccess = NULL;
	pub_opts_local.onFailure = NULL;
	pub_opts_local.context = client_local;

	pub_opts_cloud.onSuccess = NULL;
	pub_opts_cloud.onFailure = NULL;
	pub_opts_cloud.context = client_cloud;

	do 
	{
		sleep(100);
	} while (ch!='Q' && ch != 'q');

	disc_opts_local.onSuccess = onDisconnect;
	disc_opts_local.onFailure = onDisconnectFailure;
	if ((rc = MQTTAsync_disconnect(client_local, &disc_opts_local)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to start disconnect, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto destroy_exit;
	}

	
	disc_opts_cloud.onSuccess = onDisconnect;
	disc_opts_cloud.onFailure = onDisconnectFailure;
	if ((rc = MQTTAsync_disconnect(client_cloud, &disc_opts_cloud)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to start disconnect, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto destroy_exit;
	}

 	while (!disc_finished)
 	{
		#if defined(_WIN32)
			Sleep(100);
		#else
			sleep(100);
		#endif
 	}

destroy_exit:
	MQTTAsync_destroy(&client_local);
    mysql_close(conn);
exit:
    mysql_close(conn);
 	return rc;
}
