#!/bin/bash

function help {
    echo "Usage:"
    echo "./$TOOL start"
    echo "./$TOOL stop"
    echo "internal usage is docker_service {start|stop} docker_image <arg like -p 1234:1234 -e ABC=DEF> <comamnd to run from the docker container>"
    exit -1
}

function docker_service {
    if [[ $# -eq 4 ]]; then
        OP=$1
        TOOL=$2
        ARGS=$3
        COMMAND=$4

        SANITIZED_TOOL=$TOOL
        #strip org name
        if [[ $SANITIZED_TOOL == *"/"* ]]; then
            SANITIZED_TOOL=$(echo $SANITIZED_TOOL | cut -d "/" -f2)
        fi
        #Remplace : with - if a tag is used
        if [[ $SANITIZED_TOOL == *":"* ]]; then
            NAME=$(echo $SANITIZED_TOOL | cut -d ":" -f1)
            TAG=$(echo $SANITIZED_TOOL | cut -d ":" -f2)
            SANITIZED_TOOL="$NAME-$TAG"
        fi
        case $OP in
        "start")
            docker stop $SANITIZED_TOOL
            docker rm $SANITIZED_TOOL
            docker run --name $SANITIZED_TOOL $ARGS -d $TOOL $COMMAND
            docker logs -f $SANITIZED_TOOL
            ;;
        "stop")
            docker stop $SANITIZED_TOOL
            ;;
        "logs")
            docker logs -f $SANITIZED_TOOL
            ;;
        *)
            help $TOOL
            ;;
        esac
    else
        help
    fi
}
