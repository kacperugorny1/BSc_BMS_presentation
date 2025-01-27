#ifndef GLOBALS_H
#define GLOBALS_H

#include "queue.h"
#include <pthread.h>
#include <stdbool.h>


extern char mac_address[];

extern char** topics;
extern bool* dev_status;

extern Queue** mess;
extern Queue* mess_cloud;

extern char** LeftsCond;
extern char** RightsCond;
extern int* IDs;
extern int cond_count;

extern pthread_cond_t* conds;
extern pthread_mutex_t* mutexes;

extern pthread_cond_t cloud_update_sig;
extern pthread_mutex_t cloud_update_mutex;

extern int topics_count;


#endif