#!/bin/bash

dbaccess < $INFORMIXDIR/etc/syscdcv1.sql

dbaccess < $INFORMIXDIR/etc/inventory.sql
