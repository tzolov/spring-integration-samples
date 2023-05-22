#!/usr/bin/env bash
set -e

mvn clean package -DskipTests
docker build -t tzolov/file-split-ftp-sample-image:builder .
docker run -d --privileged --rm --name=file-split-ftp-sample --ulimit nofile=1024 -p 8080:8080 -v $(pwd)/target:/opt/mnt tzolov/file-split-ftp-sample-image:builder
echo "Please wait during creating the checkpoint..."
sleep 20
docker ps
docker commit --change='ENTRYPOINT ["/opt/app/entrypoint-restore.sh"]' $(docker ps -aqf "name=file-split-ftp-sample") tzolov/file-split-ftp-sample-image:checkpoint
docker kill $(docker ps -aqf "name=file-split-ftp-sample")

