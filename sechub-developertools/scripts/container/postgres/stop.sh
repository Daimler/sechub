#!/usr/bin/env bash
# SPDX-License-Identifier: MIT

source ./../common-containerscript.sh

function usage() {
    echo "Usage: $script_name <port>" 
}

defineEexposedPort $1
defineImage "sechub-test-postgres"

killContainer
