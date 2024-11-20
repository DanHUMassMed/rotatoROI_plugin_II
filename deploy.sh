#!/bin/bash
./gradlew clean jar copyJar
# Check if the Gradle build failed
if [ $? -ne 0 ]; then
    echo "Gradle build failed. Exiting script."
    exit 1
fi

fiji_pid=$(ps auxww|grep Fiji|grep -v grep| awk '{print $2}')
if [ -n "$fiji_pid" ]; then
    echo "Killing Fiji process with PID: $fiji_pid"
    kill -9 "$fiji_pid"
    sleep 0.5
else
    echo "No Fiji process found."
fi

open -a Fiji