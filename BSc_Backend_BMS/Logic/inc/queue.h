#ifndef QUEUE_H
#define QUEUE_H

typedef struct Node {
    char* data;
    struct Node* next;
} Node;

typedef struct Queue {
    Node* front;
    Node* rear;
} Queue;

// Function prototypes
Queue* createQueue();
void enqueue(Queue* queue, const char* data);
char* dequeue(Queue* queue);
void freeQueue(Queue* queue);

#endif // QUEUE_H
