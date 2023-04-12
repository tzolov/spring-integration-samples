
## Build CRaC Supporting Image

```
docker build -f src/main/resources/Dockerfile  -t java_17_crac .
```

Uses the `src/main/resources/Dockerfile` to build a new container image (`java_17_crac:latest`) using `Ubuntu 22.04` and pre-installed `Java 17 CRaC`.


## Create App Checkpoint
----

Assuming that your jar is under `target/file-split-ftp-6.0.0.jar`

1. Run the `java_17_crac:latest` image, preinstalled with Ubuntu 22.04 and CRaC, in a privileged container.
Also mount the local `target` folder to the `/opt/mnt` folder in the container.

```
docker run -it --privileged --rm --name=my-crac-app -v $(pwd)/target:/opt/mnt java_17_crac:latest /bin/bash
```

2. From within the image copy the java application into `/opt/app/` and run it with `-XX:CRaCCheckpointTo` option:

```
cp /opt/mnt/file-split-ftp-6.0.0.jar /opt/app/file-split-ftp-6.0.0.jar && java -XX:CRaCCheckpointTo=/opt/crac-files -jar /opt/app/file-split-ftp-6.0.0.jar
```

3. In another host terminal run:

```
docker exec -it  --privileged -u root my-crac-app jcmd /opt/app/file-split-ftp-6.0.0.jar JDK.checkpoint
```

to create generate an app checkpoint and then run docker commit:

```
docker commit $(docker ps -aqf "name=my-crac-app") my_app_on_crac:checkpoint
```

to create a new Docker image with the checkpoint application ready.

Then stop and exit the container in step 1.

## Run from Checkpoint

Start the application from the check point

```
docker run -it --privileged --rm --name my_app_on_crac my_app_on_crac:checkpoint java -XX:CRaCRestoreFrom=/opt/crac-files
```
