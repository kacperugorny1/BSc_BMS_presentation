#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include "../inc/queue.h"

// Function to create a queue
Queue* createQueue() {
    Queue* queue = (Queue*)malloc(sizeof(Queue));
    queue->front = queue->rear = NULL;
    return queue;
}

// Function to enqueue a string
void enqueue(Queue* queue, const char* data, int len) {
    
    Node* newNode = (Node*)malloc(sizeof(Node));
    if(len == 0){
        newNode->data = strdup(data);  // Allocate memory and copy the string
    }
    else{
        newNode->data = malloc(sizeof(char) * (len + 1));
        memcpy(newNode->data, data, len);
        newNode->data[len] = '\0';
    }
    newNode->next = NULL;
    if (queue->rear == NULL) {
        queue->front = queue->rear = newNode;
        return;
    }

    queue->rear->next = newNode;
    queue->rear = newNode;
}

// Function to dequeue a string
char* dequeue(Queue* queue) {
    if (queue->front == NULL) {
        printf("Queue is empty\n");
        return NULL; // Indicate that the queue is empty
    }
    
    Node* temp = queue->front;
    char* data = temp->data;  // Get the data to return
    queue->front = queue->front->next;

    if (queue->front == NULL) {
        queue->rear = NULL;
    }

    free(temp);
    return data;  // Return the string
}

// Function to free the queue
void freeQueue(Queue* queue) {
    while (queue->front != NULL) {
        char* data = dequeue(queue);
        free(data);  // Free the string
    }
    free(queue);
}
