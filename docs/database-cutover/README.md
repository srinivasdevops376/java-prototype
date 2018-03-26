# Demonstration of live cutover from legacy MySQL server to a MySQL running in Kubernetes

This shows how you can use MySQL replication to live migrate data to MySQL on a Kubernetes cluster as part of modernizing a legacy application.

There is an assumption that the server you wish to migrate data from is using bin logging etc. check out [old-mysql/values.yaml](old-mysql/values.yaml) for the mysql config settings expected to be set.

## Steps

### Lay down Databases

Deploy the "original" MySQL server to simulate an existing MySQL database that you wish to replicate:

```console
$ helm install -n old stable/mysql -f old-mysql/values.yaml
NAME:   old
LAST DEPLOYED: Fri Mar  9 10:58:28 2018
NAMESPACE: default
STATUS: DEPLOYED
...
```

Set some helpful environment variables:

```console
$ OLD_PASS=$(kubectl get secret --namespace default old-mysql -o jsonpath="{.data.mysql-root-password}" | base64 --decode; echo) && OLD_POD=$(kubectl get pod --selector=app=old-mysql -o jsonpath='{.items..metadata.name}')
```

Deploy the "new" MySQL server that we're migrating to:

```console
$ helm install -n new stable/mysql -f new-mysql/values.yaml
NAME:   new
LAST DEPLOYED: Fri Mar  9 11:15:47 2018
NAMESPACE: default
STATUS: DEPLOYED
...
```

Set some helpful environment variables:

```console
$ NEW_PASS=$(kubectl get secret --namespace default new-mysql -o jsonpath="{.data.mysql-root-password}" | base64 --decode; echo) && NEW_POD=$(kubectl get pod --selector=app=new-mysql -o jsonpath='{.items..metadata.name}')
```


Create some tables in original MySQL DB:

> Note: this will run sysbench as a K8s job resource to create some tables in the `sbtest` database.

```console
$ kubectl exec -ti ${OLD_POD} -- mysqladmin -pnot-a-secure-password create sbtest
$ kubectl apply -f sysbench/sysbench-1.yaml
job "sysbench-1" created
```

Wait a few minutes then check if tables exist:

```console
$ kubectl exec -ti ${OLD_POD} -- mysql -p$OLD_PASS sbtest -e "show tables;"
mysql: [Warning] Using a password on the command line interface can be insecure.
+------------------+
| Tables_in_sbtest |
+------------------+
| sbtest1          |
| sbtest2          |
| sbtest3          |
| sbtest4          |
+------------------+
```

### Set up replication

Next we need to backup and restore the master database to the slave:

> Note: the backup command is a bit complicated, so we're going to `kubectl exec` into a bash prompt, run commands, then exit.

```console
$ kubectl exec -ti ${NEW_POD} -- bash

pod$ mysqldump -pnot-a-secure-password -h old-mysql --skip-lock-tables --single-transaction --flush-logs --hex-blob --master-data=2 -A | tee /tmp/dump.sql | mysql -pnot-a-secure-password

mysql: [Warning] Using a password on the command line interface can be insecure.
mysqldump: [Warning] Using a password on the command line interface can be insecure.

root@new-mysql-cf9b46655-d859q:/$ exit

```

Set the slave to read only:

```
$ kubectl exec -ti ${NEW_POD} -- mysql -pnot-a-secure-password -e "SET GLOBAL read_only = ON;"
```

Create replication user on master:

> Note: normally you would not grant to `%` but for the ease of demo we will.

```console
$ kubectl exec -ti ${OLD_POD} -- mysql -pnot-a-secure-password \
  -e "GRANT REPLICATION SLAVE ON *.* TO \"repl\"@\"%\" IDENTIFIED BY \"replpass\";"
mysql: [Warning] Using a password on the command line interface can be insecure.

$ kubectl exec -ti ${NEW_POD} -- mysql --host=old-mysql --user=repl --password=replpass -e "SHOW GRANTS;"
mysql: [Warning] Using a password on the command line interface can be insecure.
+----------------------------------------------+
| Grants for repl@%                            |
+----------------------------------------------+
| GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%' |
+----------------------------------------------+

```

Next we need to get the master log file and position from the slave:

```console
$ kubectl exec -ti ${NEW_POD} -- head /tmp/dump.sql -n80 | grep "MASTER_LOG_POS"
-- CHANGE MASTER TO MASTER_LOG_FILE='mysql-bin.000004', MASTER_LOG_POS=154;

```

Set up the slave to replicate:
_substitute the values from above into `MASTER_LOG_FILE` and `MASTER_LOG_POS`_


```console
$ kubectl exec -ti ${NEW_POD} -- mysql -pnot-a-secure-password -e "
  CHANGE MASTER TO
    MASTER_HOST='old-mysql',
    MASTER_USER='repl',
    MASTER_PASSWORD='replpass',
    MASTER_LOG_FILE='mysql-bin.000007',
    MASTER_LOG_POS=154;
    START SLAVE;"
mysql: [Warning] Using a password on the command line interface can be insecure.
```

Wait a minute or so then check your slave status:

```console
$ kubectl exec -ti ${NEW_POD} -- mysql -pnot-a-secure-password -e "SHOW SLAVE STATUS \G";
mysql: [Warning] Using a password on the command line interface can be insecure.
*************************** 1. row ***************************
               Slave_IO_State:

        Seconds_Behind_Master: NULL
      Slave_SQL_Running_State: Slave has read all relay log; waiting for more updates
```

Check the data replicated:

```
$ kubectl exec -ti ${NEW_POD} -- mysql -pnot-a-secure-password sbtest -e \
    "show tables; select * from sbtest1 limit 5;"
mysql: [Warning] Using a password on the command line interface can be insecure.

Create some more data on the master to ensure its replicating correctly:

```console
$ kubectl exec -ti ${OLD_POD} -- mysqladmin -pnot-a-secure-password create sbtest2 && \
  kubectl apply -f sysbench/sysbench-2.yaml

```

After a few minutes we'll have more data on the slave:

```console
$ kubectl exec -ti ${NEW_POD} -- mysql -pnot-a-secure-password sbtest2 -e \
    "show tables; select * from sbtest1 limit 1;"
mysql: [Warning] Using a password on the command line interface can be insecure.
+-------------------+
| Tables_in_sbtest2 |
+-------------------+
| sbtest1           |
| sbtest2           |
| sbtest3           |
| sbtest4           |
| sbtest5           |
| sbtest6           |
| sbtest7           |
| sbtest8           |
+-------------------+
+----+-------+-------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------+
| id | k     | c                                                                                                                       | pad                                                         |
+----+-------+-------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------+
|  1 | 49929 | 83868641912-28773972837-60736120486-75162659906-27563526494-20381887404-41576422241-93426793964-56405065102-33518432330 | 67847967377-48000963322-62604785301-91415491898-96926520291 |
+----+-------+-------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------+

```

### Disable replication

Stop replicating, disable read only mode and reset slave info:

```console
$ kubectl exec -ti ${NEW_POD} -- mysql -pnot-a-secure-password -e "
        STOP SLAVE;
        SET GLOBAL read_only = OFF;
        RESET SLAVE ALL;"
```


Delete the old database ... we don't need it anymore!

```console
$ helm delete --purge old
release "old" deleted
```

```console
$ kubectl exec -ti ${NEW_POD} -- mysqladmin -pnot-a-secure-password create sbtest3 && \
  kubectl apply -f sysbench/sysbench-3.yaml

```

After a few minutes we'll have more data on the slave:

```console
$ kubectl exec -ti ${NEW_POD} -- mysql -pnot-a-secure-password sbtest3 -e \
    "show tables; select * from sbtest1 limit 1;"
mysql: [Warning] Using a password on the command line interface can be insecure.
+-------------------+
| Tables_in_sbtest3 |
+-------------------+
| sbtest1           |
| sbtest2           |
| sbtest3           |
| sbtest4           |
| sbtest5           |
| sbtest6           |
| sbtest7           |
| sbtest8           |
+-------------------+
+----+-------+-------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------+
| id | k     | c                                                                                                                       | pad                                                         |
+----+-------+-------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------+
|  1 | 49929 | 83868641912-28773972837-60736120486-75162659906-27563526494-20381887404-41576422241-93426793964-56405065102-33518432330 | 67847967377-48000963322-62604785301-91415491898-96926520291 |
+----+-------+-------------------------------------------------------------------------------------------------------------------------+-------------------------------------------------------------+
```

## Cleanup

Delete sysbench jobs:

```console
kubectl delete -f sysbench/
job "sysbench-1" deleted
job "sysbench-2" deleted
job "sysbench-3" deleted
```

Delete new database:

```console
helm delete --purge new
release "new" deleted
```
