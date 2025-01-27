#ifndef MYSQL_H__
#define MYSQL_H__
#include <stdio.h> 
#include <stdlib.h> 
#include <string.h>

#include <unistd.h>
#include <mariadb/mysql.h>
#include "globals.h"


#define MAC_ADDR_LENGTH 18
void select_conds(MYSQL *conn);
void select_topics(MYSQL *conn);

#endif