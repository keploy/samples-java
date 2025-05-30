## Introduction

This is a petclinic app where you can record testcases and mocks by interacting with the UI, and then test them using Keploy.
This project has two parts - the frontend and backend, since Keploy is a backend testing platform, we need to start the backend part of the project using Keploy and run the frontend as it is.

First, you need to install Keploy. For that you can use the command below:

```
curl -O https://raw.githubusercontent.com/keploy/keploy/main/keploy.sh && source keploy.sh
```

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

Now it's time to setup the backend of our application. Let's move to the backend directory and get started.

```
cd samples-java/spring-petclinic/spring-petclinic-rest
```

You can start the backend using Keploy in 2 ways:
- [Using Keploy's binary](#binary-guide)
- [Using Keploy's docker image](#docker-guide)

# Instructions For Starting Using Binary <a name="binary-guide"></a>

Prerequisites For Binary:
1. Node 20.11.0 LTS
2. OpenJDK 17.0.9
3. MVN version 3.6.3

## Setup the backend

You need to update the postgresql properties, go to
`spring-petclinic/spring-petclinic-rest/src/main/resources/application-postgresql.properties`
and change

```
spring.datasource.url=jdbc:postgresql://mypostgres:5432/petclinic
```

to

```
spring.datasource.url=jdbc:postgresql://localhost:5432/petclinic
```
and then build the jar using:

```
mvn clean install -Dmaven.test.skip=true
```

## Spin up the database

```
docker run -e POSTGRES_USER=petclinic -e POSTGRES_PASSWORD=petclinic -e POSTGRES_DB=petclinic -p 5432:5432 --name mypostgres postgres:15.2
```

## Recording the testcases with Keploy

```
keploy record -c "java -jar target/spring-petclinic-rest-3.0.2.jar"
```
Now you can start interacting with the UI and Keploy will automatically create the testcases and mocks for it in a folder named 'keploy'.

## Running the testcases using Keploy

```
keploy test -c "java -jar target/spring-petclinic-rest-3.0.2.jar" --delay 20
```

🎉 Hooray! You've made it to the end of the binary section! 🎉

Next we move on to the instructions to start the application using docker.

# Instructions For Starting Using Docker <a name="docker-guide"></a>

Prerequisites For Docker:
1.  Docker Desktop 4.25.2 and above

Here we just need to change the command used to start the application.

```
keploy record -c "docker compose up" --containerName javaApp --buildDelay 100s
```

## Running the testcases using Keploy

```
keploy test -c "docker compose up" --containerName javaApp --buildDelay 50s --delay 20
```
Here `delay` is the time it takes for your application to get started, after which Keploy will start running the testcases. If your application takes longer than 10s to get started, you can change the `delay` accordingly.
`buildDelay` is the time that it takes for the image to get built. This is useful when you are building the docker image from your docker compose file itself.

Hope this helps you out, if you still have any questions, reach out to us on our [Slack](https://join.slack.com/t/keploy/shared_invite/zt-357qqm9b5-PbZRVu3Yt2rJIa6ofrwWNg)