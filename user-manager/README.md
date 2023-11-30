# Example User-Manager App

A sample User-Manager app to test Keploy integration capabilities using [SpringBoot](https://spring.io) and [MongoDB](https://www.mongodb.com/).

**Note** :- Issue Creation is disabled on this Repository, please visit [here](https://github.com/keploy/keploy/issues/new/choose) to submit Issue.

## Pre-requisites

- [Support all Java Version](https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started.html#getting-started.installing)

## Quick Keploy Installation

Based on your OS and preference(Docker/Native), you setup Keploy using One-click installation method:-

```sh
curl -O https://raw.githubusercontent.com/keploy/keploy/main/keploy.sh && source keploy.sh
```

## Setup Employee-Manager App

Clone the repository and install the dependencies
```bash
git clone https://github.com/keploy/samples-java && cd user-manager

mvn clean install -Dmaven.test.skip=true 
```

### Capture the testcases

Once we have our jar file ready,this command will start the recording of API calls using ebpf:-

```bash
sudo -E env PATH=$PATH keploy record -c "java -jar target/user-manager-1.0-SNAPSHOT.jar"
```

![Testcases](./img/test-cases.png?raw=true)

Now let's run a few tests to capture some more scenarios:
#### Generate testcases

To generate testcases we just need to **make some API calls.** You can use [Postman](https://www.postman.com/), [Hoppscotch](https://hoppscotch.io/), or simply `curl`

1. Make a user entry

```bash
curl --location --request POST 'http://localhost:8081/api/user' \
--header 'Content-Type: application/json' \
--data-raw '{
    "id": 1,
    "name": "Dan",
    "age": "23",
    "birthday": "2000-1-1"
}'
```

this will return the response or an entry.

```
{"id":1,"name":"Dan","age":23,"birthday":"2000-1-1"}
```

2. Fetch recorded info about users

```bash
curl --location --request GET 'http://localhost:8081/api/user/1'
```

or by querying through the browser `http://localhost:8081/api/user/1`

3. Update user record

```bash
curl -X PUT -H "Content-Type: application/json" \
-d '{
    "id": 1,
    "name": "Update DAN",
    "age": "22",
    "birthday": "2001-2-2"
}' \
'http://localhost:8081/api/user/1'
```

this will return the response or an entry.

```
{"id":1,"name":"Update DAN","age":22,"birthday":"2001-2-2"}
```

4. Delete user record

```bash
curl -X DELETE 'http://localhost:8081/api/user/1'
```

Now both these API calls were captured as **editable** testcases and written to `keploy/test` folder. The keploy directory would also have `mock.yml` file.

Now, let's see the magic! 🪄💫

## Run the test cases

Now, let's run the keploy in test mode: -

```bash
sudo -E env PATH=$PATH keploy test -c "java -jar target/user-manager-1.0-SNAPSHOT.jar" --delay 10
```

This will run the testcases and generate the report in `keploy/testReports` folder.

