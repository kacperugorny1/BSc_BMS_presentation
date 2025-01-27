#include "../inc/globals.h"


char **topics = NULL;
Queue **mess = NULL;
Queue *mess_cloud;

bool *dev_status = NULL;

char **LeftsCond = NULL;
char **RightsCond = NULL;
int *IDs = NULL;
pthread_cond_t *conds = NULL;
pthread_mutex_t *mutexes = NULL;



int cond_count = 0;
int topics_count = 0;