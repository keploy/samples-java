## REST API with Spring-Boot + MongoDB

The application is built purely with Java Spring-Boot that does the complete CRUD in the MongoDB database. This CRUD Application is about managing the data of Magical Potions in the Keploy inventory.

### Pre-Requisites

Make sure you have the following -

1. Latest version of JDK
2. MongoDB Container
3. Postman


### About the API

The API has 5 endpoints - <br>
a. Get `/potions` - Gets the data of all the potions stored in the bank. <br>
b. Get `/potions/{id}` - Gets the data of the potion by its Id. <br>
c. Post `/potions` - Posts the data of the potion to the DB. <br>
d. Put `/potions/{id}` - Updates the data of the potion specified by its id. <br>
e. Delete `/potions/{id}` - Deletes the data of the potion specified by its id. <br>
<br><br>
The potions data should have the following attributes - <br>
a. name - Holds the name of the potion. `String` <br>
b. description - Holds the details of what the potions does. `String` <br>
c. bottle - Specify the number of bottles. `Int` <br>
d. quantity - Holds the total amount of potion the bank has in total. `Int` <br>


### Getting Started

Follow the steps to start the application -

1. Clone the repository:

   ```bash
   git clone https://github.com/keploy/samples-java
   ```

2. Start the MongoDB instance:

   ```bash
   docker run -p 27017:27017 --name spring-boot-mongo --network backend mongo
   ```
  
3. Your application is ready to be executed!!
   To start Keploy in record mode, in the root directory, run:

   ```bash
   keploy record -c "./mvnw spring-boot:run"
   ```

   a. Make a `POST` Request:

   ```bash
      curl --location 'http://localhost:8080/potions' \
      --header 'Content-Type: application/json' \
      --data '    {
            "name": "Strength Potion v2",
            "description": "Enhances the drinker'\''s physical strength temporarily.",
            "bottle": 3,
            "quantity": 150
         }'
   ```

   b. Make a `GET` Request:

   ```bash
      curl --location --request GET 'http://localhost:8080/potions'
   ```

   c. Make a `PUT` Request:

   ```bash
      curl --location --request PUT 'http://localhost:8080/potions/UUID_OF_POTION' \
      --header 'Content-Type: application/json' \
      --data '    {
            "name": "Strength Potion",
            "description": "Enhances the drinker'\''s physical strength temporarily.",
            "bottle": 5,
            "quantity": 200
         }'
   ```

   Replace the placeholder `UUID_OF_POTION` with the UUID of the potion you want to update.

   d. Make a `GET` Request using ID:

   ```bash
      curl --location --request GET 'http://localhost:8080/potions/UUID_OF_POTION'
   ```

   Replace the placeholder `UUID_OF_POTION` with the UUID of the potion you want to get.

   e. Make a `DELETE` Request:

   ```bash
      curl --location --request DELETE 'http://localhost:8080/potions/UUID_OF_POTION'
   ```

   Replace the placeholder `UUID_OF_POTION` with the UUID of the potion you want to delete.

 You can try experimenting with the data given in `potions.json` file by taking the data of each potion individually.

 The generated tests and mocks are stored in the `Keploy` directory in the CWD.

4. To test the app, start Keploy in test mode. In the root directory, run:

   ```bash
   keploy test -c "./mvnw spring-boot:run" --delay 15
   ```

   This will run the tests and generate the report in the `Keploy/reports` directory in the CWD.
