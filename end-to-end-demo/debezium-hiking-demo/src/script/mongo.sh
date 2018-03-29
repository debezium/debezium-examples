#!/bin/bash

source docker-service.sh
ARGS="-p 27017:27017"

docker_service $1 mongo "$ARGS" ""
