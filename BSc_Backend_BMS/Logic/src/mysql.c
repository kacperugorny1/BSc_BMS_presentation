#include "../inc/mysql.h"
#include "../inc/mqtt.h"

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
    conds = malloc(topics_count * sizeof(pthread_cond_t));
    mutexes = malloc(topics_count * sizeof(pthread_mutex_t));
    
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

