#!/bin/bash

# Load common functions
. "${SCRIPTS_DIR}/common_functions.sh"

# Remove existing services started from this directory
${EXECUTABLE} stack rm "${STACK_NAME}"