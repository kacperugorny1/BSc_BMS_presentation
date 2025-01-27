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

char* sync_dev(MYSQL *conn);
char* sync_cond(MYSQL *conn);

int insert_device(MYSQL *conn, char* mac, char* name);
int remove_device(MYSQL *conn, int id);
int edit_device(MYSQL *conn, int ind, char* name);

int insert_cond(MYSQL *conn, char* name, char* Lside, char* Rside);
int remove_cond(MYSQL *conn, int id);
int edit_cond(MYSQL *conn, int id, char* name, char* Lside, char* Rside, int enabled);
#endif