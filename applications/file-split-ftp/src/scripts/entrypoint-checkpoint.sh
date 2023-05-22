#!/bin/bash

echo 128 > /proc/sys/kernel/ns_last_pid; java -XX:CRaCCheckpointTo=/opt/crac-files -jar /opt/app/file-split-ftp-6.0.0.jar&
sleep 10
jcmd /opt/app/file-split-ftp-6.0.0.jar JDK.checkpoint
sleep infinity
