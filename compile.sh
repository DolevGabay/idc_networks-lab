#!/bin/bash

# Compile the MultiThreadedChatServer.java file
javac Program.java

if [ $? -eq 0 ]; then
  echo "Compilation successful! You can now run the server with './run.sh'."
else
  echo "Compilation failed. Please check the error messages above."
fi