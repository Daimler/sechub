#!/usr/bin/env sh
# SPDX-License-Identifier: MIT

# GoSec needs access to the go binary
export PATH="$TOOL_FOLDER/gosec:/usr/local/go/bin:$PATH"
GO111MODULE=on gosec -fmt=sarif -out="$PDS_JOB_RESULT_FILE" "$PDS_JOB_SOURCECODE_UNZIPPED_FOLDER/..."
