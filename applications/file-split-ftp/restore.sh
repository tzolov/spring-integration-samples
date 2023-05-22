#!/usr/bin/env bash
set -e

docker run --rm -p 8080:8080 --name file-split-ftp-sample tzolov/file-split-ftp-sample-image:checkpoint
