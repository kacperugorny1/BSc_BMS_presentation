#include "../inc/mysql.h"
#include "../inc/mqtt.h"

int insert_device(MYSQL *conn, char* mac, char* name){
    int i;
    char *query = malloc((79 + strlen(name))*sizeof(char));
    if(query == NULL){
        fprintf(stderr, "Memory alloc error\n");
        exit(1);
    }
    sprintf(query,"INSERT INTO DEVICES(Mac, Name) VALUES ('%s','%s');", mac, name);

    i = !mysql_query(conn, query);

    free(query);
    return i;
}

int remove_device(MYSQL *conn, int ind){
    int i;
    char query[100];
    if(query == NULL){
        fprintf(stderr, "Memory alloc error\n");
        exit(1);
    }
    sprintf(query,"DELETE FROM DEVICES WHERE DevID = %d", ind);

    i = !mysql_query(conn, query);

    return i;
}

int edit_device(MYSQL *conn, int ind, char* name){
    int i;
    char query[100];
    if(query == NULL){
        fprintf(stderr, "Memory alloc error\n");
        exit(1);
    }
    sprintf(query,"UPDATE DEVICES SET Name = '%s' WHERE DevID = %d", name, ind);

    i = !mysql_query(conn, query);

    return i;
}



int insert_cond(MYSQL *conn, char* name, char* Lside, char* Rside){
    char *query = malloc((70 + strlen(name) + strlen(Lside) + strlen(Rside)) * sizeof(char));
    int i; 
    if(query == NULL){
        fprintf(stderr, "Memory alloc error\n");
        exit(1);
    }
    sprintf(query,"INSERT INTO COND(Name, Lside, Rside, Enabled) VALUES ('%s','%s','%s',TRUE);", name, Lside, Rside);

    i = !mysql_query(conn, query);
  

    free(query);
}

int remove_cond(MYSQL *conn, int id){
    char *query = malloc((70) * sizeof(char));
    int i; 
    if(query == NULL){
        fprintf(stderr, "Memory alloc error\n");
        exit(1);
    }
    sprintf(query,"DELETE FROM COND WHERE CondID = %d",id);

    i = !mysql_query(conn, query);
  

    free(query);
}

int edit_cond(MYSQL *conn, int id, char* name, char* Lside, char* Rside, int enabled){
    char *query = malloc((70 + strlen(name) + strlen(Lside) + strlen(Rside)) * sizeof(char));
    int i; 
    if(query == NULL){
        fprintf(stderr, "Memory alloc error\n");
        exit(1);
    }
    sprintf(query,"UPDATE COND SET Lside='%s', Rside='%s', Name='%s', Enabled=%d WHERE CondID = %d",Lside,Rside,name,enabled,id);

    i = !mysql_query(conn, query);
  

    free(query);
}



char* sync_dev(MYSQL *conn){
    char *query = "SELECT * FROM DEVICES";
    char* toSend;
     if (mysql_query(conn, query)) {
        fprintf(stderr, "SELECT query failed: %s\n", mysql_error(conn));
        return NULL;
    }

    MYSQL_RES *result = mysql_store_result(conn);
    if (result == NULL) {
        fprintf(stderr, "Failed to store result: %s\n", mysql_error(conn));
        return NULL;
    }

    MYSQL_ROW row;
    toSend = malloc(sizeof(char)*1000);
    strcpy(toSend,"SYNCDEV:");
    while ((row = mysql_fetch_row(result))) {
        strcat(toSend, "D");
        strcat(toSend, row[0]);
        strcat(toSend, ";");
        strcat(toSend, row[1]);
        strcat(toSend, ";");
        strcat(toSend, row[2]);
        strcat(toSend, ";");
        
    }

    mysql_free_result(result);
    return toSend;

}

char* sync_cond(MYSQL *conn){
    char *query = "SELECT * FROM COND";
    char* toSend;
     if (mysql_query(conn, query)) {
        fprintf(stderr, "SELECT query failed: %s\n", mysql_error(conn));
        return NULL;
    }
    MYSQL_RES *result = mysql_store_result(conn);
    if (result == NULL) {
        fprintf(stderr, "Failed to store result: %s\n", mysql_error(conn));
        return NULL;
    }
    MYSQL_ROW row;
    toSend = malloc(sizeof(char)*1000);
    strcpy(toSend,"SYNCCOND:");
    while ((row = mysql_fetch_row(result))) {
        strcat(toSend, "C");
        strcat(toSend, row[0]);
        strcat(toSend, ";");
        strcat(toSend, row[1]);
        strcat(toSend, ";");
        strcat(toSend, row[2]);
        strcat(toSend, ";");
        strcat(toSend, row[3]);
        strcat(toSend, ";");
        strcat(toSend, row[4]);
        strcat(toSend, ";");
    }
    mysql_free_result(result);
    return toSend;

}



void select_conds(MYSQL *conn){
    int i = 0;
    const char *query = "SELECT Lside, Rside FROM COND WHERE ENABLED = TRUE";

    if (mysql_query(conn, query)) {
        fprintf(stderr, "SELECT query failed: %s\n", mysql_error(conn));
        return;
    }

    MYSQL_RES *result = mysql_store_result(conn);
    if (result == NULL) {
        fprintf(stderr, "Failed to store result: %s\n", mysql_error(conn));
        return;
    }
    cond_count = mysql_num_rows(result);
    RightsCond = malloc(cond_count*sizeof(char*));
    LeftsCond = malloc(cond_count*sizeof(char*));
    MYSQL_ROW row;
    while ((row = mysql_fetch_row(result))) {
        LeftsCond[i] = malloc(sizeof(char)*(strlen(row[0]) + 1));
        RightsCond[i] = malloc(sizeof(char)*(strlen(row[1]) + 1));
    
        strcpy(LeftsCond[i],row[0]);
        strcpy(RightsCond[i],row[1]);
        printf("%s => %s\n",LeftsCond[i],RightsCond[i]);
        ++i;
    }

    mysql_free_result(result);

}

void select_topics(MYSQL *conn) {
    int i = 0;
    const char *query = "SELECT Mac, DevID FROM DEVICES"; 

    if (mysql_query(conn, query)) {
        fprintf(stderr, "SELECT query failed: %s\n", mysql_error(conn));
        return;
    }

    MYSQL_RES *result = mysql_store_result(conn);
    if (result == NULL) {
        fprintf(stderr, "Failed to store result: %s\n", mysql_error(conn));
        return;
    }

    int num_fields = mysql_num_fields(result);
    topics_count  = mysql_num_rows(result);
    topics = malloc(topics_count * sizeof(char*));
    IDs = malloc(topics_count * sizeof(int));
    mess = malloc(topics_count * sizeof(Queue*));
    mess_cloud = malloc(sizeof(Queue));
    mess_cloud = createQueue();
    conds = malloc(topics_count * sizeof(pthread_cond_t));
    mutexes = malloc(topics_count * sizeof(pthread_mutex_t));
    dev_status = malloc(topics_count*sizeof(bool));
    for(int i = 0; i < topics_count; i++) dev_status[i] = false;
    
    MYSQL_ROW row;

    // // Print column names (optional)
    // MYSQL_FIELD *field;
    // while ((field = mysql_fetch_field(result))) {
    //     printf("%s\t", field->name);
    // }
    // printf("\n");

    // Fetch and print each row of the result

    while ((row = mysql_fetch_row(result))) {
        mess[i] = createQueue();
        pthread_cond_init(&conds[i], NULL);
        pthread_mutex_init(&mutexes[i], NULL);
        IDs[i] = atoi(row[1]);
        topics[i++] = malloc(sizeof(char) * MAC_ADDR_LENGTH);
        strcpy(topics[i - 1], row[0]);
        printf("%d - %s\t",IDs[i - 1], topics[i - 1] ? topics[i - 1] : "NULL");
        printf("\n");
    }

    mysql_free_result(result);
}

