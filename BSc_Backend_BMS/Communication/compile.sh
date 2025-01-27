#!/bin/bash

SOURCE_FILES="main.c src/queue.c src/mqtt.c src/mysql.c src/globals.c" 
OUTPUT_FILE="communication.out"      

gcc $SOURCE_FILES -o $OUTPUT_FILE -lmariadb -lpaho-mqtt3a -lpthread -lm

