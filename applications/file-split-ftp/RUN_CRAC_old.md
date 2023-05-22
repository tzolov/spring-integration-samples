From within the `file-split-ftp` folder use the `tzolov/java_17_crac:latest` image to start a new container: `my-crac-app`.
Later mounts the local `target` folder to the `/opt/mnt` in the container to access the file-split-ftp-6.0.0.jar.
```
docker run -it --privileged --rm --name=my-crac-app -v $(pwd)/target:/opt/mnt tzolov/java_17_crac:latest /bin/bash
```
while in the container copy the jar and run the file-split-ftp app:
```
cp /opt/mnt/file-split-ftp-6.0.0.jar /opt/app/file-split-ftp-6.0.0.jar && java -XX:CRaCCheckpointTo=/opt/crac-files -jar /opt/app/file-split-ftp-6.0.0.jar
```
the jar is copied because the checkpoint is not self-packaged but requires the original jar as well.

After the application is warmed up, open a second terminal and use `jcmd` to trigger the checkpoint creation:
```
docker exec -it  --privileged -u root my-crac-app jcmd /opt/app/file-split-ftp-6.0.0.jar JDK.checkpoint
```
the `/opt/app/file-split-ftp-6.0.0.jar` string identifies the running process to be checkpointed. The process ID can be used as well.
The checkpoints images are created under the `/opt/crac-files` folder in the `my-crac-app` container.
After the checkpoint complete, CRaC stops/kills the running application.

Next we create a new dedicated Docker image (named `my_app_on_crac`) that packages all checkpoint files and original jar:
```
docker commit $(docker ps -aqf "name=my-crac-app") my_app_on_crac:checkpoint
```
Now you can shutdown the original container and use the new `my_app_on_crac:checkpoint` image to bootstrap the app form the checkpoint:
```
docker run -it --privileged --rm --name my_app_on_crac my_app_on_crac:checkpoint java -XX:CRaCRestoreFrom=/opt/crac-files
```
