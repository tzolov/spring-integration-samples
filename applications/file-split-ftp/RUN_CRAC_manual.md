* Build the project:
```
mvn clean install -DskipTests
```

* Build a Docker image with CRaC JDK support:

```
docker  build -f ./DockerfileCRaCBasic -t tzolov/file-split-ftp-sample-image:builder-basic .
```

Run the image in a terminal
```
docker run -it  --privileged --rm --name=file-split-ftp-sample --ulimit nofile=1024 -p 8080:8080 -v $(pwd)/target:/opt/mnt tzolov/file-split-ftp-sample-image:builder-basic /bin/bash
```
Later mounts the local `target` folder to the `/opt/mnt` in the container to access the file-split-ftp-6.0.0.jar.


while in the container copy the jar and run the file-split-ftp app:

```
echo 128 > /proc/sys/kernel/ns_last_pid; java -XX:CRaCCheckpointTo=/opt/crac-files -jar /opt/app/file-split-ftp-6.0.0.jar
```

After the application is warmed up, open a second terminal and use `jcmd` to trigger the checkpoint creation:
```
docker exec -it  --privileged -u root file-split-ftp-sample jcmd /opt/app/file-split-ftp-6.0.0.jar JDK.checkpoint
```

The checkpoints images are created under the `/opt/crac-files` folder in the `my-crac-app` container.
After the checkpoint complete, CRaC stops/kills the running application.

Next we create a new dedicated Docker image (named `my_app_on_crac`) that packages all checkpoint files and original jar:
```
docker commit $(docker ps -aqf "name=file-split-ftp-sample") tzolov/file-split-ftp-sample-image:checkpoint
docker kill $(docker ps -aqf "name=file-split-ftp-sample")
```

Now you can shutdown the original container and use the new `tzolov/file-split-ftp-sample-image:checkpoint` image to bootstrap the app form the checkpoint:
```
docker run -it --privileged --rm --name my_app_on_crac tzolov/file-split-ftp-sample-image:checkpoint java -XX:CRaCRestoreFrom=/opt/crac-files
```


Implementation: https://github.com/spring-projects/spring-framework/blob/main/spring-context/src/main/java/org/springframework/context/support/DefaultLifecycleProcessor.java

