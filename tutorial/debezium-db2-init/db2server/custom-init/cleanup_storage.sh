#!/bin/bash

################################################################################
# Wipes the storage directory.
# Note that this essentially makes the database non-persistent when pod is deleted
################################################################################

echo "Inspecting database directory"
ls  $STORAGE_DIR

echo "Wiping database directory"
rm -rf $STORAGE_DIR/*
