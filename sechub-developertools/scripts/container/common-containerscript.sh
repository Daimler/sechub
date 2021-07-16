#!/bin/bash 
CONST_DOES_NOT_EXIST="does-not-exist"

script_name=$0

function addEnv(){
    assertNotEmpty "empty env param not allowed" $1
    count=0
    while [ "x${envlist[$count]}" != "x" ]
    do
       count=$(( $count + 1 ))
    done
    envlist[count]=$1
}

function calcEnvData(){
    env_data=""
    count=0
    while [ "x${envlist[$count]}" != "x" ]
    do
       env_data="$env_data -e ${envlist[$count]}"   
       count=$(( $count + 1 ))
    done
}

#
# param 1: exposed port, may not be empty
function defineContainerPort(){
    assertNotEmpty "port not defined" $1
    
    container_port=$1
}

#
# param 1: exposed port, may not be empty
function defineEexposedPort(){
    assertNotEmpty "port not defined" $1
    
    exposed_port=$1
}

function assertNotEmpty(){
    if [[ -z "$1" ]]; then
        echo "> no message given, wrong implemented!"
        exit 4
    fi
    if [[ -z "$2" ]]; then
         echo "> $1"
         usage 
         exit 1
    fi
}

function defineImage(){
    assertNotEmpty "image name not defined" $1
    assertNotEmpty "port was not exposed" $exposed_port
    
    image_name=$1
    container_name="${image_name}_${exposed_port}"

}

function ensureImageBuild(){
    assertNotEmpty "image name not defined" $image_name
    
    docker build -t $image_name -f Dockerfile .

    buildResult=$?

    if [[ "$buildResult" != 0 ]]; then
        echo "> FAILURE: buildresult was not 0 but $buildResult"
        exit 1
    fi
}


function checkContainerRunning(){
    assertNotEmpty "container name not defined" $container_name
    
    running=$(docker inspect --format="{{.State.Running}}" $container_name 2> /dev/null)
    
    if [ $? -eq 1 ]; then
      echo "* $container_name does not exist."
      running=CONST_DOES_NOT_EXIST
    fi
}

function startContainer(){
    assertNotEmpty "container port not defined" $container_port
    assertNotEmpty "expose port not defined" $exposed_port
    assertNotEmpty "image name not defined" $image_name
    assertNotEmpty "container name not defined" $container_name

    calcEnvData 

    # -p host:port:container_port
    nohup docker run --rm -p 127.0.0.1:$exposed_port:$container_port/tcp $env_data --name $container_name $image_name >/dev/null 2>&1 &
    
    running=$CONST_DOES_NOT_EXIST
    
    while [ "$running" != "true" ]; do
        checkContainerRunning 
    done
    
    echo ">> container has been started!"
    
    
}

function stopContainer(){

    checkContainerRunning

    if [ "$running" == "false" ]; then
      echo "* $container_name is not running"
    else
        echo "* trigger stop of container $container_name"
        docker container stop $container_name
        echo "* container $container_name stopped"
    fi

}

function killContainer(){

    checkContainerRunning

    if [ "$running" != CONST_DOES_NOT_EXIST ]; then
        echo "* trigger kill of container $container_name"
        docker container kill $container_name
        echo "* container $container_name killed"
    fi

}


function ensureContainerNotRunning(){
    checkContainerRunning
    
    if [ "$running" != CONST_DOES_NOT_EXIST ]; then
        echo "* container with name $container_name found, will terminate old one!"
        killContainer # kill already running container with same name if existing
    fi
    
}
