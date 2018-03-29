# java-prototype

[java-prototype](https://github.com/heptio/java-prototype) simulates a troublesome monolithic Java application that is designed to be hard to orchestrate in containers.

## Introduction

This chart installs java-prototype to kubernetes using the [Helm](https://helm.sh) package manager.

## Prerequisites

- Kubernetes 1.8+ with Beta APIs enabled

## Installing the Chart

To install the chart with the release name `my-release`:

```bash
$ helm dependency update
$ helm install --name my-release .
```

This command installs java-prototype and a MySQL database in the default configuration. The [configuration](#configuration) section lists the parameters that can be configured during installation.

> **Tip**: List all releases using `helm list`

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```bash
$ helm delete --purge my-release
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Configuration

The following table lists the configurable parameters of the java-prototype chart and their default values.

| Parameter                            | Description                               | Default                                              |
| ------------------------------------ | ----------------------------------------- | ---------------------------------------------------- |
| `replicaCount` | how many replicas of the app to run | 1 |
| `image.repository` | image to run | `billkoch/heptio-java-prototype` |
| `image.tag` | image tag to run | `issue-34` |
| `pullPolicy` | image pull policy | `IfNotPresent` |
| `javaopts` | options to pass to java | `-Xmx2048m` |
| `service.type` | type of service | `NodePort` |
| `service.port` | port to expose | `8080` |
| `ingress` | see values.yaml for setting up ingress | |
| `resources` | limit cpu/memory resources | `{}` |
| `nodeSelector` | kubernetes node selector | `{}`
| `tolerations` | kubernetes tolerations | `[]`
| `affinity` | kubernetes affinity | `{}`

The followng table configures mysql chart when `mysql.enabled` is set to `true`.

| Parameter                            | Description                               | Default                                              |
| ------------------------------------ | ----------------------------------------- | ---------------------------------------------------- |
| `mysql.enabled` | use embedded mysql chart | `true`
| `mysql.persistence.enabled` | persist storage | `false`
| `mysql.mysqlUser` | mysql user | `java-prototype`
| `mysql.mysqlDatabase` | mysqll database | `java-prototype`
| `mysql.mysqlPassword` | mysql password | `not-a-secure-password`

The followng table lists database configuration parameters to use when `mysql.enabled` is set to `false`.

| Parameter                            | Description                               | Default                                              |
| ------------------------------------ | ----------------------------------------- | ---------------------------------------------------- |
| `mysqlHost` | mysql host | `java-prototype-mysql-mysql` |
| `mysqlUser` | mysql username | `java-prototype` |
| `mysqlPassword` | mysql password |`not-a-secure-password` |
| `mysqlDatabase` | mysql database | `java-prototype` |
| `mysqlPort` | mysql port | `3306` |
| `flywayUser` | user with escalated privs to create database | `root` |
| `flywayPass` | password with escalated privs to create database | `root` |

Specify each parameter using the `--set key=value[,key=value]` argument to `helm install`. For example,

```bash
$ helm install --name my-release \
  --set javaOpts="-Xmx2048m" .
```

Alternatively, a YAML file that specifies the values for the parameters can be provided while installing the chart. For example,

```bash
$ helm install --name my-release -f examples/externaldb/values.yaml .
```

> **Tip**: You can use the default [values.yaml](values.yaml)

## Persistence

By default persistence for the database is disabled. See the documentation for the [MySql chart](https://github.com/kubernetes/charts/tree/master/stable/mysql) for details on how to configure it for persistent volumes.
