#ifndef GLOBALS_H
#define GLOBALS_H

#include "queue.h"
#include <pthread.h>

extern char** topics;
extern Queue** mess;
extern char** LeftsCond;
extern char** RightsCond;
extern int* IDs;
extern pthread_cond_t* conds;
extern pthread_mutex_t* mutexes;
extern int cond_count;
extern int topics_count;


#endif