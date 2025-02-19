# How to configure the client

To change the client configuration, such as the URL base path, or the number of threads, or the total number of mesages to be uploaded,
Go to `ClientConfig.java` under `skiclient` folder. 

`SERVER_BASE_PATH_EC2` is for updating the remote EC2 server base path. 

`SERVER_BASE_PATH_LOCALHOST` is for testing of the server running on tomcat on local host. 

`NUMBER_OF_THREADS` is the total number of threads used for sending `POST` request to the server. 

`TOTAL_NUMBER_OF_POST` is the total number of messages randomly generated which is used by the client to send `POST` request to server. 

# How to run the client

To run the client, open the project in Intellij and go to `Main.java` file, then execute the main function in `Main.java`.

# Execution result

For execution result, please see the `ClientPart2ScreenShot.png` graph. 

# Verification

To verify that the requests are actually sent to the ec2 server, please see the `PostmanGetRequest.png`, `PostmanPostREquest.png` and `ServerLog.png` graph in `cs6650_client` package. 