Build, run and checkpoint the application:

Uses the [Automatic checkpoint/restore at startup](https://docs.spring.io/spring-framework/reference/6.1-SNAPSHOT/integration/checkpoint-restore.html#_automatic_checkpointrestore_at_startup)

```
./checkpoint-on-refresh.sh
```

Then use the `tzolov/file-split-ftp-sample-image:checkpoint` to restore the application from the checkpoint:

```
./restore.sh
```
