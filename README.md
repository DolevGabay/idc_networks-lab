# MultiThreadedWebServer

MultiThreadedWebServer is a simple Java web server that handles HTTP requests concurrently using multiple threads.

## Getting started
- cd to your project directory
- make sure that you have the permissionts to run the compile.sh and run.sh files
- chmod +x compile.sh
- chmod +x run.sh

- replace the values with your desired configurations in config.ini:
- `port`: The port on which the server listens for incoming connections.
- `maxThreads`: The maximum number of threads to handle concurrent requests.
- `root`: The root directory from which the server serves files.
- `defaultPage`: The default page to serve when no specific file is requested.

## How to run:
- ./compile.sh
- ./run.sh

## Classes and Their Roles
1. **Program:** 
    - Role: This class serves as the entry point for the execution of the web server, it initialize a new MultiThreadedWebServer and start it.

2. **MultiThreadedWebServer:**  
   - Role: The main class responsible for starting the server, accepting incoming connections, and spawning threads to handle client requests.
   
3. **RequestHandler:**  
   - Role: Implements the `Runnable` interface and represents a worker thread responsible for handling client requests. It parses incoming HTTP requests, delegates request handling based on the request method (GET or POST), and sends appropriate responses back to the client.
   
4. **HttpRequest:**  
   - Role: Represents an HTTP request received from the client. It parses the request line, headers, and parameters. It also provides methods to access various request attributes such as method, path, headers, and parameters.

## Design Overview

The MultiThreadedWebServer follows a multi-threaded design to handle multiple client connections concurrently. When a client connects to the server, a new `RequestHandler` thread is spawned to handle the client's request. The server listens for incoming connections on the specified port and creates a fixed-size thread pool using `ExecutorService` to manage thread concurrency.

The `RequestHandler` class is responsible for handling incoming HTTP requests. It utilizes the HttpRequest class to parse the requests and determine their methods. Depending on the request method, it either serves static files from the designated root directory for GET requests or extracts parameters from the request body for POST requests. In the case of POST requests, it constructs an HTML response containing the received parameters to be displayed to the client.

The `HttpRequest` class parses HTTP request lines, headers, and parameters, providing methods to access various attributes of the request. It distinguishes between GET and POST requests and extracts parameters accordingly.

Overall, the server is designed to efficiently handle concurrent HTTP requests, serving static content and processing POST requests with form data submission.






