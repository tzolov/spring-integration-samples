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
