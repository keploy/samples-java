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
docker run -e POSTGRES_USER=petclinic -e POSTGRES_PASSWORD=petclinic -e POSTGRES_DB=petclinic -p 5432:5432 --net keploy-network  postgres:15.2
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

## Run the backend with Keploy

```
keploy record -c "java -jar target/<name-of-your-jar>"
```

Now when you interact with the UI, the tests should start getting created in a folder called 'keploy' in the directory where you started the backend. When you are done recording the testcases and mocks, you can run them using keploy.

## Running the testcases using Keploy

```
keploy test -c "java -jar target/<name-of-your-jar>" --delay 10
```
Here delay is the time it takes for your application to get started, after which Keploy will start running the testcases. If your application takes longer than 10s to get started, you can change the delay accordingly.

Hope this helps you out, if you still have any questions, reach out to us on our [Slack](https://join.slack.com/t/keploy/shared_invite/zt-12rfbvc01-o54cOG0X1G6eVJTuI_orSA)



