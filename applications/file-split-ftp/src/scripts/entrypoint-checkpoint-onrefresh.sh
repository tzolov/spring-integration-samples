#!/bin/bash

echo "Before Checkpoint ..."
echo 128 > /proc/sys/kernel/ns_last_pid; java -XX:CRaCCheckpointTo=/opt/crac-files -Dspring.context.checkpoint=onRefresh -jar /opt/app/file-split-ftp-6.0.0.jar&
echo "After Checkpoint ..."
sleep infinity
