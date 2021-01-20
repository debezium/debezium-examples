#!/bin/bash

source ./env.sh

cell=${CELL:-'test'}
uid=$TABLET_UID
mysql_port=$[17000 + $uid]
printf -v alias '%s-%010d' $cell $uid
printf -v tablet_dir 'vt_%010d' $uid

mkdir -p $VTDATAROOT/backups

echo "Starting MySQL for tablet $alias..."
action="init"

if [ -d $VTDATAROOT/$tablet_dir ]; then
 echo "Resuming from existing vttablet dir:"
 echo "    $VTDATAROOT/$tablet_dir"
 action='start'
fi

mysqlctl \
 -log_dir $VTDATAROOT/tmp \
 -tablet_uid $uid \
 -mysql_port $mysql_port \
 $action
