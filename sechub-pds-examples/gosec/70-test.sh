#!/usr/bin/env bash

file_to_upload="$1"
retries=20

function usage() {
    echo "Usage: `basename $0` <file-path-to-upload>"
}

function is_job_finished () {
    local status="$1"
    job_finished="no"

    if [[ $status == "DONE" || $status == "FINISHED" ]]
    then
        job_finished="yes"
    fi

    echo $job_finished
}

function paramater_missing() {
    MESSAGE="$1"

    echo "[ERROR] $MESSAGE"
    usage
    exit 1
}

if [[ -z "$PDS_SERVER" ]]
then
    paramater_missing "Environment variable PDS_SERVER missing"
fi

if [[ -z "$PDS_USERID" ]]
then
    paramater_missing "Environment variable PDS_USERID missing"
fi

if [[ -z "$PDS_APITOKEN" ]]
then
    paramater_missing "Environment variable PDS_APITOKEN missing"
fi

if [[ -z "$file_to_upload" ]]
then
    paramater_missing "Please provide file to upload"
fi

if [[ ! -f "$file_to_upload" ]]
then
    paramater_missing "File $file_to_upload does not exist."
fi

pds_api="../../sechub-developertools/scripts/pds-api.sh"
jobUUID=`$pds_api create_job PDS_GOSEC "288607bf-ac81-4088-842c-005d5702a9e9" | jq '.jobUUID' | tr -d \"`

"$pds_api" upload_zip "$jobUUID" "$file_to_upload"
"$pds_api" mark_job_ready_to_start "$jobUUID"

# Check the status of the job
status=""

while [[ $retries -ge 0 && $(is_job_finished $status) == "no" ]]
do
    status=`$pds_api job_status "$jobUUID" | jq '.state' | tr -d \"`
    echo "Job status: $status"
    ((retries--))
    sleep 0.5
done

# echo return the actual result
"$pds_api" job_result "$jobUUID"

