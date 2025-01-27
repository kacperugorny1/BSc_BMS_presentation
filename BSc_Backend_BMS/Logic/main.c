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
MQTTAsync client;
pthread_barrier_t start_barrier;
MQTTAsync_responseOptions pub_opts = MQTTAsync_responseOptions_initializer;

int *prev_dig;
int **prev_an;
bool *condDone;
int getValue(int id, char **cond, int *actualState);
int getValueSlope(int id, char **cond, int *actualState);
int checkExpression(int id, int condId,char** cond, int* actualState);

void condCheck(int id, int *actualState);
void executeRside(int id, char* Rside, int* actualState);

// TODO: how often periodic read occurs for each cond - new column in cond.
// 		 new column in cond that specify if it uses SLOPES.

//Handler - calls condCheck on mqtt message, menage states of variables
void *handleTopic(void* args){
    long id = (long)args;
	int temp[5] = {0};
	char* msg;
	bool first_msg = true;

	
	while(1){
		pthread_mutex_lock(&mutexes[id]);
		while(mess[id]->front == NULL){
			pthread_cond_wait(&conds[id], &mutexes[id]);
		}
		printf("MESSAGE_HANDLE thread_id=%d\n",id);
		msg = dequeue(mess[id]);
		pthread_mutex_unlock(&mutexes[id]);
		
		if(strlen(msg) != 25){
			free(msg);
			continue;
		}
		temp[0] = (int)strtol(&msg[3],NULL,16);
		for(int i = 0; i < 4; ++i)
			temp[i + 1] = strtol(&msg[6 + i*5],NULL,16);
		if(!first_msg){
			condCheck(id, temp);
		}
		else first_msg = false;
		prev_dig[id] = temp[0];
		prev_an[id][0] = temp[1];
		prev_an[id][1] = temp[2];
		prev_an[id][2] = temp[3];
		prev_an[id][3] = temp[4];
	
		free(msg);
	}

}
// t=10:10
//Runs checkExpression and executeRside for every rule
//Also implements periodic conds
void condCheck(int id, int *actualState){
	static time_t timestamp = 0;
    bool time_check = false;
    if(time(NULL) - timestamp >= 4) time_check = true;
	for(int i = 0; i < cond_count; ++i){
		char* ptr;
		ptr = LeftsCond[i];
		if(time_check || strstr(LeftsCond[i],":\0") || strstr(LeftsCond[i],":)") || strchr(LeftsCond[i],'\\') || strstr(LeftsCond[i], "/\0") || strstr(LeftsCond[i], "/)")){
			if(checkExpression(id, i, &ptr, actualState))
				executeRside(id,RightsCond[i],actualState);
		}
	}
	if(time_check)
		timestamp = time(NULL);
}

//Get value from a GPIO or numeric value
int getValue(int id, char **cond, int *actualState) {
    char *endptr;
    int val;
    bool analog;
    int targetId = -1;
	
    if ((*cond)[0] != 'd') {
        val = (int)strtol(*cond, &endptr, 10);
        *cond = endptr;
        return val;
    }
    int devId = (int)strtol(&(*cond)[1], &endptr, 10);

    if (endptr[0] == 'g')
        analog = false;
    else if (endptr[0] == 'a')
        analog = true;
    else
        return -1;
    int g = (int)strtol(&endptr[1], &endptr, 10);
    *cond = endptr;

    for (int j = 0; j < topics_count; ++j) {
        if (IDs[j] == devId) {
            targetId = j;
            break;
        }
    }

    if (analog) {
        return (targetId != id) ? prev_an[targetId][g - 1] : actualState[g];
    } else {
        return (targetId != id) ? ((prev_dig[targetId] >> (g - 1)) & 1 ? 0xFFF : 0x000)
                                : ((actualState[0] >> (g - 1)) & 1 ? 0xFFF : 0x000);
    }
}

//Get value from a SLOPE
int getValueSlope(int id, char **cond, int *actualState){
	char *endptr;
	int dev = (int)strtol(&(*cond)[1], &endptr, 10);
	int targetId = -1;
	
	for (int j = 0; j < topics_count; ++j) {
		if (IDs[j] != dev) continue;
		targetId = j;
		break;
	}
	char type = endptr[0];

	int g = (int)strtol(&endptr[1], &endptr, 10);
	char sym = *endptr;
	*cond = ++endptr;
	
	if(!(((actualState[0]>>(g-1)) ^ (prev_dig[id]>>(g-1))) & 1))
		return 0;

	if(targetId != id) return 0;
	if(type == 'a') return 0;
	if(sym == '/') return ((actualState[0]>>(g-1)) & 1) * 0xFFF;
	else if (sym == '\\') return ((prev_dig[id]>>(g-1)) & 1) * 0xFFF;
	else if (sym == ':') return 0xFFF;
	else return 0;
}

//Get value from a TimeCond
int getValueTime(int id, int condId, char **cond, int *actualState){
	time_t cur_time = time(NULL);
	struct tm *local_time = localtime(&cur_time);
	int h = strtol(++(*cond),cond,10);
	if(**(cond) != ':') return 0;
	int m = strtol(++(*cond), cond, 10);
	printf("h=%d,%d; m=%d,%d, s=%d\n",h,local_time->tm_hour,m,local_time->tm_min,local_time->tm_sec);
	if(h == local_time->tm_hour && m == local_time->tm_min && condDone[condId] == false){
		condDone[condId] = true;
		return 0xFFF;
	}
	else if((h != local_time->tm_hour || m != local_time->tm_min) && condDone[condId] == true){
		condDone[condId] = false;
	}
	return 0;
}

//Interpreter of logic conditionals returns value of condition
//Recursive function
int checkExpression(int id, int condId, char **cond, int *actualState) {
    int64_t val = 0, val2;

    while (**cond != '\0') {
        switch (**cond) {
			case 't':
                ++(*cond);
				if(**cond == '=' && condId != -1) val = getValueTime(id,condId, cond, actualState);
				else return 0;
				break;
            case '+':
                ++(*cond);
                val += (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
                break;
            case '-':
                ++(*cond);
                val -= (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
                break;
            case '*':
                ++(*cond);
                val *= (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
                break;

            case '/':
                ++(*cond);
				if(**cond == 'd' || **cond == '(' || (**cond >= 48 & **cond < 58))
                	val /= (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
                else{
					--(*cond);
					while(**cond != 'd') --(*cond);
					val = getValueSlope(id,cond,actualState);
				}
				break;

			case '\\':
				while(**cond != 'd') --(*cond);
				val = getValueSlope(id,cond,actualState);
				break;
			case ':':
				while(**cond != 'd') --(*cond);
				val = getValueSlope(id,cond,actualState);
				break;

            case '>':
                ++(*cond);
                val2 = (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
                val = (val > val2) ? 0xFFF : 0x000;
                break;

            case '<':
                ++(*cond);
                val2 = (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
                val = (val < val2) ? 0xFFF : 0x000;
                break;

            case '|':
                ++(*cond);
                val2 = (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
                val |= val2;
                break;

            case '&':
                ++(*cond);
                val2 = (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
                val &= val2;
                break;

            case '=':
                ++(*cond);
                val2 = (**cond != '(') ? getValue(id, cond, actualState) : checkExpression(id, condId, cond, actualState);
				if(val2 == 1) val2 = 0xFFF;
				val = val == val2? 0xFFF: 0;
                break;

            case '(':
                ++(*cond);
                val = checkExpression(id, condId, cond, actualState);
                break;

            case ')':
                ++(*cond);
                return val;
                break;

            default:
			//cyfra
                val = getValue(id, cond, actualState);
                break;
        }
    }
    return val;
}

//Function that takes Rside and executes it - sends MQTT
void executeRside(int id, char* Rside, int* actualState){
	MQTTAsync_message pubmsg = MQTTAsync_message_initializer;
	char* endptr;
	int targetId = 0;
	char msg[15];
	char *str = malloc(strlen(Rside) + 1);
	char *token;
	int set;
	

	if (str != NULL) {
		strcpy(str, Rside);
	}

	pubmsg.qos = MQTTREASONCODE_GRANTED_QOS_1;
	pubmsg.retained = 0;

	token = strtok(str, " ");
    while (token != NULL) {
		
		endptr = token;
		set = getValue(id,&endptr,actualState);

        int dev = (int)strtol(&token[1], &endptr, 10);
        int targetId = -1;
        
        for (int j = 0; j < topics_count; ++j) {
            if (IDs[j] != dev) continue;
            targetId = j;
            break;
        }

        bool digitalOut;
        if (endptr[0] == 'g') {
            digitalOut = true;
        } else if (endptr[0] == 'a') {
            digitalOut = false;
        } else {
            free(str);
            return;
        }

        int g = (int)strtol(&endptr[1], &endptr, 10);
        
		char* ptr;
		ptr = endptr+1;
        if (digitalOut) {
			if(ptr[0] == '!')
				set = set == 0 ? 0xFF : 0;
			else{
				set = checkExpression(id, -1, &ptr, actualState);
				if(set > 0) set = 0xFF;
			}
			pubmsg.payloadlen = sprintf(msg,"00FFFF%02X%02X", 1 << (g - 5), set);
			//executeRsideDigital(id, targetId, g, actualState, &pubmsg, endptr, msg);
        } else {
            set = checkExpression(id, -1, &ptr, actualState);
			if (g == 3) g = 0x04;
			else if(g == 4) g = 0x06;
			else return;
			pubmsg.payloadlen = sprintf(msg, "FFFFFF%02X%04X", g, set & 0xFFF);
			//executeRsideAnalog(id, targetId, g, actualState, &pubmsg, endptr, msg);
        }
		pubmsg.payload = msg;
        MQTTAsync_sendMessage(client, topics[targetId], &pubmsg, &pub_opts);
        printf("m:%s t:%s\n", pubmsg.payload, topics[targetId]);

        token = strtok(NULL, " ");
    }

    free(str);  
}

//Configuration
int main(int argc, char* argv[])
{
	MQTTAsync_connectOptions conn_opts = MQTTAsync_connectOptions_initializer;
	MQTTAsync_disconnectOptions disc_opts = MQTTAsync_disconnectOptions_initializer;
	int rc;
	int ch;

    MYSQL *conn;

    pthread_t* threads;
	
	
	
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

	condDone = (bool*)calloc(cond_count * sizeof(bool), cond_count);
	

    //PTHREAD
    threads = malloc(topics_count * sizeof(pthread_t));
	prev_dig = malloc(topics_count * sizeof(int));
	prev_an = malloc(topics_count * sizeof(int*));
    for(long i = 0; i < topics_count; ++i){
        pthread_create(&threads[i], NULL, handleTopic, (void *)i);
		prev_an[i] = malloc(4 * sizeof(int));
	}




    //MQTT
	if ((rc = MQTTAsync_create(&client, ADDRESS, CLIENTID, MQTTCLIENT_PERSISTENCE_NONE, NULL))
			!= MQTTASYNC_SUCCESS)
	{
		printf("Failed to create client, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto exit;
	}

	if ((rc = MQTTAsync_setCallbacks(client, client, connlost, msgarrvd, NULL)) != MQTTASYNC_SUCCESS)
	{
		printf("Failed to set callbacks, return code %d\n", rc);
		rc = EXIT_FAILURE;
		goto destroy_exit;
	}
	conn_opts.keepAliveInterval = 20;
	conn_opts.cleansession = 1;
	conn_opts.onSuccess = onConnect;
	conn_opts.onFailure = onConnectFailure;
	conn_opts.context = client;
	if ((rc = MQTTAsync_connect(client, &conn_opts)) != MQTTASYNC_SUCCESS)
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
	
	pub_opts.onSuccess = NULL;
	pub_opts.onFailure = NULL;
	pub_opts.context = client;
	
	
	if(argc == 2 && strcmp(argv[1],"-t") == 0) {
		// int id = 5;
		// int actualState[5] = {0}; // Initialize with actual values
		// char expression[100];
		// strcpy(expression,"(d1a2-d1a1)/2");
		// char *cond = expression;
		// int result = checkExpression(id, &cond, actualState);
		// printf("Result: %d\n", result);

		// strcpy(expression,"(d1a2-d1a1)>2500");
		// cond = expression;
		// result = checkExpression(id, &cond, actualState);
		// printf("Result: %d\n", result);

		// strcpy(expression,"(d1a2-d1a1)>1000");
		// cond = expression;
		// result = checkExpression(id, &cond, actualState);
		// printf("Result: %d\n", result);

		// strcpy(expression,"((d1a2-d1a1)>2500)|(d1g1=0)");
		// cond = expression;
		// result = checkExpression(id, &cond, actualState);
		// printf("Result: %d\n", result);

		// strcpy(expression,"((d1a2-d1a1)*2>2500)|(d1g1=1)");
		// cond = expression;
		// result = checkExpression(id, &cond, actualState);
		// printf("Result: %d\n", result);

		// strcpy(expression,"((d1a2-d1a1)>2500)&(d1g1=1)");
		// cond = expression;
		// result = checkExpression(id, &cond, actualState);
		// printf("Result: %d\n", result);

		// strcpy(expression,"((d1a2-d1a1)>1000)&(d1g1=1)");
		// cond = expression;
		// result = checkExpression(id, &cond, actualState);
		// printf("Result: %d\n", result);

	}

	do 
	{
		sleep(100);
	} while (ch!='Q' && ch != 'q');

	disc_opts.onSuccess = onDisconnect;
	disc_opts.onFailure = onDisconnectFailure;
	if ((rc = MQTTAsync_disconnect(client, &disc_opts)) != MQTTASYNC_SUCCESS)
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
	MQTTAsync_destroy(&client);
    mysql_close(conn);
exit:
    mysql_close(conn);
 	return rc;
}
