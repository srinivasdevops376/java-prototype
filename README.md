# java-prototype

Owner: Kris Nova

Author: Kris Nova [Github][6]


# Overview

`java-prototype` simulates a troublesome monolithic Java application that is designed to be hard to orchestrate in containers.

# Install

Installing with [maven][5]

```bash
mvn clean package
```

Also for a quick development run the following command to build and run the application.

```bash
make dev
```

# Minikube Deployment

### Prerequisites

* [minikube](https://github.com/kubernetes/minikube/releases)
* [helm](https://github.com/kubernetes/helm/releases)
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/#install-kubectl-binary-via-curl)

### Prepare Environment

Start minikube and install helm:

```console
$ minikube start --memory 3064
Starting local Kubernetes v1.9.4 cluster...
Starting VM...
...

$ helm init
$HELM_HOME has been configured at /home/pczarkowski/.helm.
Tiller (the Helm server-side component) has been installed into your Kubernetes Cluster.
Happy Helming!

$ helm version
Client: &version.Version{SemVer:"v2.8.1", GitCommit:"6af75a8fd72e2aa18a2b278cfe5c7a1c5feca7f2", GitTreeState:"clean"}
Server: &version.Version{SemVer:"v2.8.1", GitCommit:"6af75a8fd72e2aa18a2b278cfe5c7a1c5feca7f2", GitTreeState:"clean"}
```

### Install

Download chart dependencies and install java-prototype:

```console
$ helm dependency update
Hang tight while we grab the latest from your chart repositories...
...
Downloading mysql from repo https://kubernetes-charts.storage.googleapis.com

$ helm install --namespace prototype -n demo .
NAME:   demo
LAST DEPLOYED: Thu Mar 29 15:46:30 2018
NAMESPACE: prototype
...
...
NOTES:
1. Get the application URL by running these commands:
  export NODE_PORT=$(kubectl get --namespace prototype -o jsonpath="{.spec.ports[0].nodePort}" services demo-java-prototype)
  export NODE_IP=$(kubectl get nodes --namespace prototype -o jsonpath="{.items[0].status.addresses[0].address}")
  echo http://$NODE_IP:$NODE_PORT
```

After a few minutes there should be two pods that show as READY:

```console
$ kubectl -n prototype get pods
NAME                                   READY     STATUS    RESTARTS   AGE
demo-java-prototype-79c9969867-xpcw2   1/1       Running   1          1m
demo-mysql-865b44b449-9rtpq            1/1       Running   0          1m
```

### Test

Verify the application is responding:

```console
$ curl $(minikube service demo-java-prototype --url -n prototype)
Success!
$ curl $(minikube service demo-java-prototype --url -n prototype)/health
{"status":"UP","diskSpace":{"status":"UP","total":17293533184,"free":15394152448,"threshold":10485760},"db":{"status":"UP","database":"MySQL","hello":1}}
```

### Cleanup

Delete the helm deployment:

```console
$ helm delete --purge demo
release "demo" deleted
```

Destroy minikube:

```console
$ minikube delete
Deleting local Kubernetes cluster...
Machine deleted.
```




# Cloud Deployment
1. GCP: Create a build trigger using `cloudbuild.yaml` linking your clone or fork of this repo.
1. AWS: Include `buildspec.yaml` in a CodeBuild object linking your clone or fork of this repo.

There are two containers to build, a frontend (the java app) and a backend (the mysql database). _NOTE_ This is not a secure database image, and therefore should not be used in production.

This deployment strategy requires [helm](https://helm.sh/), with a tiller installation that has appropriate RBAC roles and rolebindings.
Optionally, it is recommended to install `ahmetb`'s [kubectx and kubens](https://github.com/ahmetb/kubectx) and the `Farmotive` cloud-native dev tools:[kex](https://github.com/farmotive/kex), [kpoof](https://github.com/farmotive/kpoof), [klog](https://github.com/farmotive/klog), and [kud](https://github.com/farmotive/kud).
If on a Mac, install these with Homebrew:
```bash
brew tap farmotive/k8s
brew install kpoof klog kex kud
```

### Configuring

There are three environmental variables that need to be defined in order to authenticate with a MySQL server.

```bash
export JAVAPROTOTYPE_MYSQL_CONNECTION_STRING="jdbc:mysql://my.server.url/database"
export JAVAPROTOTYPE_MYSQL_CONNECTION_USER="user"
export JAVAPROTOTYPE_MYSQL_CONNECTION_PASS="password"
```

### Deployment One: the mysql database container
Set environment variables in order to install the mysql chart using helm:
```bash
export NAMESPACE=java-prototype
export DEPLOY_NAME=java-prototype-mysql
export JAVAPROTOTYPE_MYSQL_CONNECTION_PASS="password" #alter this if exposing this deployment externally
export JAVAPROTOTYPE_MYSQL_CONNECTION_USER="java-prototype"
export SIZE=1Gi #If cost is not a concern, increase this to a desired capacity, or omit ",persistence.size=$SIZE" below to use the default 10Gi allocation.
```

Install the backend deployment:

```bash
kubectl create namespace $NAMESPACE
kubens $NAMESPACE
helm install --namespace $NAMESPACE \
--name=$DEPLOY_NAME \
--set mysqlUser=$JAVAPROTOTYPE_MYSQL_CONNECTION_USER,mysqlPassword=$JAVAPROTOTYPE_MYSQL_CONNECTION_PASS,mysqlDatabase=java-prototype,persistence.size=$SIZE \
stable/mysql
```

Once the deployment is successful, obtain the randomly generated root password from the secret for MYSQL_ROOT_PASSWORD:
```bash
export MYSQL_PASS=$(kubectl get secret --namespace $NAMESPACE java-prototype-mysql-mysql -o jsonpath="{.data.mysql-root-password}" | base64 --decode; echo)
echo $MYSQL_PASS
```

Then connect to the mysql instance ([kpoof](https://github.com/farmotive/kpoof), [Sequel Pro](www.sequelpro.com), `mysql client`, etc) and import `java-prototype.sql`

If you wish to manually port forward, first, get the pod name with

```bash
kubectl get pods -n java-prototype
```

Copy the pod name, and then replace <POD> below with the pod name:

```bash
kubectl port-forward -n java-prototype <POD> 3306:3306
```

```bash
mysql -u root -p$MYSQL_PASS -h 127.0.0.1 java-prototype < java-prototype.sql
```
When prompted, use the password echoed above.

### Deployment Two: the java application

Modify the image name in `java-prototype/values.yaml image.name` to match the image generated by `cloudbuild.yaml`.  It will resemble `gcr.io/$PROJECT_ID/java-prototype`.
Modify the password in `java-prototype/values.yaml env.pass` to match the `JAVAPROTOTYPE_MYSQL_CONNECTION_PASS` export (if `JAVAPROTOTYPE_MYSQL_CONNECTION_PASS` was altered).

```bash
helm install helm/java-prototype/ --namespace java-prototype --name java \
  --set mysql.enabled=false,flywayPass=NWWXmQAHVb,mysqlPassword=NWWXmQAHVb
```

Once the output of the `NOTES.txt` displays, run `kubectl get pods -w` to see the java deployment succeed.  Use `ctrl-C` to regain a prompt.

### Manipulating the application
_NOTE_ The following activities require multiple terminal sessions.

1. Create a local connection for the database
```bash
kpoof
```
Select the mysql pod.  Port 3306 will be forwarded.  Connect using the database manager of your choosing with `127.0.0.1` as the host, `java-prototype` as the user, and the value for `$JAVAPROTOTYPE_MYSQL_CONNECTION_PASS` as the password.  Choose `java-prototype` as the schema and `getrequests` as the table.  You should see column headers of `id`, `timestamp`, and `hash` with an empty row set.

1. In a new terminal session, create a local connection for the application
```bash
kpoof
```
Select the java app pod.  Port 8080 will be forwarded.  Connect using the browser of your choosing to `localhost:8080`.  Once resolved, `Success!` will appear.

1. In a new terminal session run the following infinite loop to create entries in the database:
```bash
while true; do curl localhost:8080; done
```

1.  Optional:

In a new terminal session:
```bash
klog -f
```
Select the java pod.  It is likely that the application will log `outOfMemory` errors.  Additionally, logs will contain the writes to the database as well as free memory (if any).
1. Review each terminal window.  The terminal for logs will update with a crash or a new write statement with a random hash.  For each successful `curl`, the terminal for port-forwarding the app will show a valid connection to 8080.  The database manager, when refreshed, will show database rows for each successful connection.

### Cleanup
```bash
helm delete --purge java java-prototype-mysql
kubectl delete namespace java-prototype
```



# Troubleshooting

If you encounter any problems that the documentation does not address, [file an issue][3] or talk to us on the [Kubernetes Slack team][4] channel `#monolith`.

# Contributing

Thanks for taking the time to join our community and start contributing!

Feedback and discussion is available on [the mailing list][2].

* Please familiarize yourself with the [Code of Conduct][0] before contributing.
* See [CONTRIBUTING.md][1] for instructions on the developer certificate of origin that we require.


# Changelog

[0]: https://github.com/heptio/java-prototype/CODE-OF-CONDUCT.md
[1]: https://github.com/heptio/java-prototype/CONTRIBUTING.md
[2]: https://groups.google.com/forum/#!forum/monolithic-apps-to-k8s
[3]: https://github.com/heptio/java-prototype/issues
[4]: http://slack.kubernetes.io/
[5]: https://maven.apache.org/install.html
[6]: https://github.com/kris-nova/
