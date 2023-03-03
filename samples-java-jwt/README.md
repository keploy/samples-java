# Example Authintication App

A sample Authintication app to test Keploy integration capabilities using [SpringBoot](https://spring.io) and JWT.

## Pre-requisites

- [Java 8+](https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started.html#getting-started.installing)

## Quick Installation of "KEPLOY" server

### **MacOS**

```shell
curl --silent --location "https://github.com/keploy/keploy/releases/latest/download/keploy_darwin_all.tar.gz" | tar xz -C /tmp

sudo mv /tmp/keploy /usr/local/bin && keploy
```

### **Linux**

<details>
<summary>Linux</summary>

```shell
curl --silent --location "https://github.com/keploy/keploy/releases/latest/download/keploy_linux_amd64.tar.gz" | tar xz -C /tmp

sudo mv /tmp/keploy /usr/local/bin && keploy
```

</details>

<details>
<summary>Linux ARM</summary>

```shell
curl --silent --location "https://github.com/keploy/keploy/releases/latest/download/keploy_linux_arm64.tar.gz" | tar xz -C /tmp

sudo mv /tmp/keploy /usr/local/bin && keploy
```


</details>

### **Windows**

<details>
<summary>Windows</summary>

- Download
  the [Keploy Windows AMD64](https://github.com/keploy/keploy/releases/latest/download/keploy_windows_amd64.tar.gz), and
  extract the files from the zip folder.
- Run the `keploy.exe` file.

</details>

<details>
<summary>Windows ARM</summary>

- Download
  the [Keploy Windows ARM64](https://github.com/keploy/keploy/releases/latest/download/keploy_windows_arm64.tar.gz), and
  extract the files from the zip folder.
- Run the `keploy.exe` file.

</details>

## Build configuration

[Find the latest release](https://search.maven.org/artifact/io.keploy/keploy-sdk) of the Keploy Java SDK at maven
central.

Add *keploy-sdk* as a dependency to your *pom.xml*:

    `<dependency>`
      `<groupId>`io.keploy `</groupId>`
      `<artifactId>`keploy-sdk `</artifactId>`
      `<version>`N.N.N `</version>` (eg: 1.2.8)
    `</dependency>`

or to *build.gradle*:

    implementation 'io.keploy:keploy-sdk:N.N.N' (eg: 1.2.8)

## Usage
- Refer [this](https://github.com/keploy/java-sdk#usage).

## Setup Authintication App

```bash
git clone https://github.com/keploy/samples-java 
```
Enter the ide (e.g. intellij idea) from `samples-java-jwt` directory

### Maven clean install

```shell
mvn clean install -Dmaven.test.skip=true 
```

### Set KEPLOY_MODE to record

- To record testcases use `KEPLOY_MODE` env variable and set the same to `record` mode.

## Generate testcases

To generate testcases we just need to **make some API calls.** You can use [Postman](https://www.postman.com/), [Hoppscotch](https://hoppscotch.io/), or simply `curl`

### 1. hello api without authentication
if you tried the following request, it will get you forbidden entry, because there is no token.

```bash
curl --location --request GET 'http://localhost:8080/hello'
```
![fail_hello](./src/main/resources/fail_hello.png "fail_hello")

### 2. Login
Now, let's login.

*note: You can enter any username, but for password it must be "keploy", if you connected to database you can make password dynamic*

```bash
curl --location --request POST 'http://localhost:8080/login' \
--header 'Content-Type: application/json' \
--data-raw '{
    "username": "Reem",
    "lastName": "keploy"
}'
```

this will return the token for the username, similar to the following. 

```
{
    "jwt": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJSZWVtIiwiZXhwIjoxNjc3ODY2MTA2LCJpYXQiOjE2Nzc4MzAxMDZ9.2RBqAB-LzirIyHyBlCSYRhDeEVa6FTDaiaqxCYnPQaw"
}
```
![login](./src/main/resources/login.png "login")

### 3. hello api with authentication
take the token from login response, and add it in `Authorization` token then send the request as shown

```bash
curl --location --request GET 'http://localhost:8080/hello'
```
![success_hello](./src/main/resources/success_hello.png "success_hello")

Now both API calls(login and hello api with authentication) were captured as **editable** testcases and written to `test/e2e/keploy-tests` folder. 

![record](./src/main/resources/capturing_record_and_testing.png "record")

Now, let's see the magic! 🪄💫

## Test mode

To test using `KEPLOY_MODE` env variable, set the same to `test` mode.

```
export KEPLOY_MODE=test
```

Now simply run the application either by ide or using command:

```shell
java -javaagent:<your full path to agent jar>.jar -jar <your full path to appliation jar>.jar
```

Keploy will run all the captures test-cases, compare and show the results on the console and on Keploy.
![successTests](./src/main/resources/success_tests.png  "Summary")

Go to the Keploy Console TestRuns Page to get deeper insights on what testcases ran, what failed.

![testruns](./src/main/resources/tests.png  "Summary")

