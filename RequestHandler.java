import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestHandler implements Runnable {

    private Socket clientSocket;

    public RequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleRequest();
        } catch (IOException e) {
            System.out.println("Error while handling request");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error while closing client socket");
            }
        }
    }

    private void handleRequest() throws IOException {
        // handle incoming request
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            HttpRequest httpRequest = new HttpRequest(reader);
            //System.out.println(httpRequest.getFullRequest());
            System.out.println("--------------------");
            httpRequest.printHeaders();
            System.out.println("--------------------");

            if(httpRequest.getCorrupted())
            {
                sendResponse(400, "Bad Request", "text/html", null, writer, httpRequest);
                return;
            }            
            
            if ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod())) {
                handleGetRequest(httpRequest, writer);
            } else if ("POST".equals(httpRequest.getMethod())) {
                handlePostRequest(httpRequest, writer);
            } else if ("TRACE".equals(httpRequest.getMethod())) {
                handleTraceRequest(httpRequest, writer);
            } else if ("OPTIONS".equals(httpRequest.getMethod()) || "DELETE".equals(httpRequest.getMethod()) || "PATCH".equals(httpRequest.getMethod()) || "PUT".equals(httpRequest.getMethod())) {
                String content = "Not Implemented";
                byte[] contentBytes = content.getBytes();
                sendResponse(501, "Not Implemented", "text/html", contentBytes, writer, httpRequest);
            } else {
                System.out.println("Bad Request");
                sendResponse(400, "Bad Request", "text/html", null, writer, httpRequest);
            } 
            
        } catch (Exception e) {
            String content = "Internal Server Error";
            byte[] contentBytes = content.getBytes();
            sendResponse(500, "Internal Server Error", "text/html", contentBytes, null, null);
            System.out.println("Error while handling request");
        }
    }

    private void handleGetRequest(HttpRequest httpRequest, BufferedWriter writer) throws IOException {
        if(httpRequest.getMethod() == "HEAD") {
            // Send only the headers
            sendResponse(200, "OK", "text/html", null, writer, httpRequest);
            return;
        }
        
        if(!handleSearchAndSendFile(writer, httpRequest)) {
            String content = "Not Found";
            byte[] contentBytes = content.getBytes();
            sendResponse(404, "Not Found", "text/html", contentBytes, writer, httpRequest);
        }
    }

    private void handlePostRequest(HttpRequest httpRequest, BufferedWriter writer) {
        try {
            if (httpRequest.getPath().equals("/params_info.html")) {
                
                Path filePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), "params_info.html");
                if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    byte[] contentBytes = Files.readAllBytes(filePath);
                    sendResponse(200, "OK", "text/html", contentBytes, writer, httpRequest);
                } else {
                    HttpRequest.fillParamsInfoFile(filePath, MultiThreadedWebServer.getServerParam()); // create the file empty
                    byte[] contentBytes = Files.readAllBytes(filePath);
                    sendResponse(200, "OK", "text/html", contentBytes, writer, httpRequest);
                    return; 
                }
            } else {
                if(!handleSearchAndSendFile(writer, httpRequest)) {
                    String content = "Not Found";
                    byte[] contentBytes = content.getBytes();
                    sendResponse(404, "Not Found", "text/html", contentBytes, writer, httpRequest);
                }
            }
        } catch (IOException e) {
            System.out.println("Error while handling request");
        }
    }       

    private void handleTraceRequest(HttpRequest httpRequest, BufferedWriter writer) throws IOException {
        String content = httpRequest.getFullRequest();
        byte[] contentBytes = content.getBytes();
        sendResponse(200, "OK", "message/http", contentBytes, writer, httpRequest);
    }

    private boolean handleSearchAndSendFile(BufferedWriter writer, HttpRequest httpRequest) throws IOException {
        try{
            String path = httpRequest.getPath();
            Path filePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), path);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                byte[] fileContent = Files.readAllBytes(filePath);
                sendResponse(200, "OK", determineContentType(filePath), fileContent, writer, httpRequest);
                return true;
            }
            return false;
        } catch (IOException e) {
            String content = "Internal Server Error";
            byte[] contentBytes = content.getBytes();
            sendResponse(500, "Internal Server Error", "text/html", contentBytes, writer, httpRequest);
            System.out.println("Error while handling request");
            return false;
        }
    }
    
    private void sendResponse(int statusCode, String statusText, String contentType, byte[] content, BufferedWriter writer, HttpRequest httpRequest) throws IOException {
        try {
            if (httpRequest.getCorrupted()){
                corruptedResponse(writer);
                return;
            }
            StringBuilder responseHeaders = new StringBuilder();
            responseHeaders.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");            
    
            if (httpRequest.isChunked()) {
                responseHeaders.append("Transfer-Encoding: chunked\r\n");
            } else if (!httpRequest.getMethod().equalsIgnoreCase("HEAD")) {
                responseHeaders.append("Content-Length: ").append(content.length).append("\r\n");
            }
    
            responseHeaders.append("Content-Type: ").append(contentType).append("\r\n");
            responseHeaders.append("\r\n");
    
            System.out.println("Response Headers:\n" + responseHeaders.toString());
    
            writer.write(responseHeaders.toString());
            writer.flush();
            
            // Send the response body if the request method is not HEAD
            if (!httpRequest.getMethod().equalsIgnoreCase("HEAD")) {
                if (httpRequest.isChunked()) {
                    // Send content in chunks
                    try (OutputStream outputStream = clientSocket.getOutputStream()) {
                        int offset = 0;
                        while (offset < content.length) {
                            int bytesToWrite = Math.min(1000, content.length - offset);
                            outputStream.write(String.valueOf(bytesToWrite).getBytes());
                            outputStream.write("\r\n".getBytes());
                            outputStream.write(content, offset, bytesToWrite);
                            outputStream.write("\r\n".getBytes());
                            offset += bytesToWrite;
                        }
                        outputStream.write("0\r\n\r\n".getBytes());
                        outputStream.flush();
                    } catch (Exception e) {
                        System.out.println("Error while sending response");
                    }
                }
                else {
                    // Send content in a single chunk
                    try (OutputStream outputStream = clientSocket.getOutputStream()) {
                        outputStream.write(content);
                        outputStream.flush();
                        outputStream.close();
                    } catch (Exception e) {
                        System.out.println("Error while sending response");
                    }
                }
            }
    
            System.out.println("Sent response header:\nHTTP/1.1 " + statusCode + " " + statusText);
        } catch (Exception e) {
            System.out.println("Error while sending response");
        }
    }

    private void corruptedResponse(BufferedWriter writer) {
        try {
            System.out.println("400 Bad Request - The request is corrupted");
            String errorMessage = "400 Bad Request - The request is corrupted";
            byte[] errorContent = errorMessage.getBytes();
            writer.write("HTTP/1.1 400 Bad Request\r\n");
            writer.write("Content-Length: " + errorContent.length + "\r\n");
            writer.write("Content-Type: text/plain\r\n");
            writer.write("\r\n");
            writer.write(errorMessage);
            writer.flush();
            return;
        } catch (IOException e) {
            System.out.println("Error while sending response");
        }
    }

    private String determineContentType(Path filePath) {
        // determine content type based on file extension
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".ico")) {
            return "icon";
        } else {
            return "application/octet-stream";
        }
    }
}
