#!/bin/bash

# Define variables
SOURCE_FILES="main.c src/queue.c src/mqtt.c src/mysql.c src/globals.c"  # List your C source files here
OUTPUT_FILE="logic.out"       # Replace with your desired output binary name

# Compile the code
gcc $SOURCE_FILES -o $OUTPUT_FILE -lmariadb -lpaho-mqtt3a -lpthread -lm -g

# Check if compilation was successful
if [ $? -eq 0 ]; then
    echo "Compilation successful! Executable created: $OUTPUT_FILE"
else
    echo "Compilation failed."
fi
