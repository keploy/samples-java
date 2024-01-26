## Introduction

This is a petclinic app where you can record testcases and mocks by interacting with the UI, and then test them using Keploy.
This project has two parts - the frontend and backend, since Keploy is a backend testing platform, we need to start the backend part of the project
using Keploy and run the frontend as it is.

## Setup the frontend

```
git clone https://github.com/keploy/samples-java.git
cd samples-java/spring-petclinic/spring-petclinic-angular
npm uninstall -g angular-cli @angular/cli
npm cache clean
npm install -g @angular/cli@latest
npm install --save-dev @angular/cli@latest
npm i
```

## Start the frontend

```
npm run start
```

## Spin up the database

```
docker run -e POSTGRES_USER=petclinic -e POSTGRES_PASSWORD=petclinic -e POSTGRES_DB=petclinic -p 5432:5432 --net keploy-network --name mypostgres postgres:15.2
```

## Setup Keploy

```
wget https://raw.githubusercontent.com/keploy/keploy/main/keploy.sh && source keploy.sh
```

## Setup the backend

```
cd samples-java/spring-petclinic/spring-petclinic-rest
mvn clean install -Dmaven.test.skip=true
```

You also need to update the postgresql properties, go to

```
spring-petclinic/spring-petclinic-rest/src/main/resources/application-postgresql.properties
```

and change

```
spring.datasource.url=jdbc:postgresql://mypostgres:5432/petclinic
```

to

```
spring.datasource.url=jdbc:postgresql://localhost:5432/petclinic
```



## Run the backend with Keploy(binary)

```
keploy record -c "java -jar target/<name-of-your-jar>"
```

Now when you interact with the UI, the tests should start getting created in a folder called 'keploy' in the directory where you started the backend. When you are done recording the testcases and mocks, you can run them using keploy.

## Starting the backend with Keploy(docker)
Starting the backend with keploy requires just a small change in the script used to run Keploy. The command will look something like this:

### For docker on Mac

```
alias keploy='sudo docker run --pull always --name keploy-v2 -e BINARY_TO_DOCKER=true -p 16789:16789 --privileged --pid=host -it -v " + os.Getenv("PWD") + ":/files -v /sys/fs/cgroup:/sys/fs/cgroup -v debugfs:/sys/kernel/debug:rw -v /sys/fs/bpf:/sys/fs/bpf -v /var/run/docker.sock:/var/run/docker.sock -v " + os.Getenv("HOME") + "/.keploy-config:/root/.keploy-config -v " + os.Getenv("HOME") + "/.keploy:/root/.keploy --rm ghcr.io/keploy/keploy'
```

### For docker on Linux

```
alias keploy='sudo docker run --pull always --name keploy-v2 -e BINARY_TO_DOCKER=true -p 16789:16789 --privileged --pid=host -it -v " + os.Getenv("PWD") + ":/files -v /sys/fs/cgroup:/sys/fs/cgroup -v /sys/kernel/debug:/sys/kernel/debug -v /sys/fs/bpf:/sys/fs/bpf -v /var/run/docker.sock:/var/run/docker.sock -v " + os.Getenv("HOME") + "/.keploy-config:/root/.keploy-config -v " + os.Getenv("HOME") + "/.keploy:/root/.keploy --rm ghcr.io/keploy/keploy'

```

```
keploy record -c "docker compose up" --containerName javaApp --buildDelay 50s
```

## Running the testcases using Keploy(binary)

```
keploy test -c "java -jar target/<name-of-your-jar>" --delay 10
```

## Running the testcases using Keploy(docker)

```
keploy test -c "docker compose up" --containerName javaApp --buildDelay 50s --delay 10
```
Here `delay` is the time it takes for your application to get started, after which Keploy will start running the testcases. If your application takes longer than 10s to get started, you can change the `delay` accordingly.

Hope this helps you out, if you still have any questions, reach out to us on our [Slack](https://join.slack.com/t/keploy/shared_invite/zt-12rfbvc01-o54cOG0X1G6eVJTuI_orSA)



