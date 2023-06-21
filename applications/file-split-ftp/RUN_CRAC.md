[Spring Framework CRaC Implementation](https://github.com/spring-projects/spring-framework/blob/main/spring-context/src/main/java/org/springframework/context/support/DefaultLifecycleProcessor.java)


# Quick Start

* Build the project:

```
mvn clean install -DskipTests
```

Build, run and checkpoint the application:

```
./checkpoint.sh
```

Later builds the `file-split-ftp` application, uses the `Dockerfile` to create a docker, builder, image: `tzolov/file-split-ftp-sample-image:builder`
Then it starts the application from the builder image, generates a CRaC checkpoint (stored in container's `/opt/crac-files` folder).
Finally it generates a new, checkpoint image (`tzolov/file-split-ftp-sample-image:checkpoint`) from the checkpointed application.


Then use the `tzolov/file-split-ftp-sample-image:checkpoint` to restore the application from the checkpoint:

```
./restore.sh
```

## Automatic checkpoint/restore at Boot startup

Uses the [Automatic checkpoint/restore at startup](https://docs.spring.io/spring-framework/reference/6.1-SNAPSHOT/integration/checkpoint-restore.html#_automatic_checkpointrestore_at_startup)

```
./checkpoint-on-refresh.sh
```

Then use the `tzolov/file-split-ftp-sample-image:checkpoint` to restore the application from the checkpoint:

```
./restore.sh
```

## Step-by-step procedure

Build the project jar:

```
mvn clean install -DskipTests
```

Build a Docker image with CRaC JDK support:

```
docker  build -f ./DockerfileCRaCBasic -t tzolov/file-split-ftp-sample-image:builder-basic .
```




Run the `tzolov/file-split-ftp-sample-image:builder-basic` image:

```
docker run -it  --privileged --rm --name=file-split-ftp-sample --ulimit nofile=1024 -p 8080:8080 -v $(pwd)/target:/opt/mnt tzolov/file-split-ftp-sample-image:builder-basic /bin/bash
```

Then start the `file-split-ftp-6.0.0.jar` app from the mounted `/opt/mnt` folder.

```
echo 128 > /proc/sys/kernel/ns_last_pid; java -XX:CRaCCheckpointTo=/opt/crac-files -jar /opt/app/file-split-ftp-6.0.0.jar
```

- `-XX:CRaCCheckpointTo=/opt/crac-files` where to store the checkpoint files.

After the application is warmed up, open a second terminal and trigger the checkpoint creation:
```
docker exec -it  --privileged -u root file-split-ftp-sample jcmd /opt/app/file-split-ftp-6.0.0.jar JDK.checkpoint
```

Next we create a new dedicated Docker image (`tzolov/file-split-ftp-sample-image:checkpoint`) that packages all checkpoint files and the app jar:
```
docker commit $(docker ps -aqf "name=file-split-ftp-sample") tzolov/file-split-ftp-sample-image:checkpoint
```

Now you can shutdown the original container:

```
docker kill $(docker ps -aqf "name=file-split-ftp-sample")
```


and use the new `tzolov/file-split-ftp-sample-image:checkpoint` image to bootstrap the app form the checkpoint:

```
docker run -it --privileged --rm --name my_app_on_crac tzolov/file-split-ftp-sample-image:checkpoint java -XX:CRaCRestoreFrom=/opt/crac-files
```

