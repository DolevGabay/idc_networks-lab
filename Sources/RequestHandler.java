import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
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

            printRequestInfo(httpRequest);

            if(httpRequest.isCorrupted())
            {
                sendResponse(400, "text/html", null, writer, httpRequest);
                return;
            }            
            
            if ("GET".equals(httpRequest.getMethod()) || "HEAD".equals(httpRequest.getMethod())) {
                handleGetRequest(httpRequest, writer);
            } else if ("POST".equals(httpRequest.getMethod())) {
                handlePostRequest(httpRequest, writer);
            } else if ("TRACE".equals(httpRequest.getMethod())) {
                handleTraceRequest(httpRequest, writer);
            } else if ("OPTIONS".equals(httpRequest.getMethod()) || "DELETE".equals(httpRequest.getMethod()) || "PATCH".equals(httpRequest.getMethod()) || "PUT".equals(httpRequest.getMethod())) {
                sendResponse(501, "text/html", null, writer, httpRequest);
            } else {
                System.out.println("Bad Request");
                sendResponse(400, "text/html", null, writer, httpRequest);
            } 
            
        } catch (Exception e) {
            
            sendResponse(500, "text/html", null, null, null);
            System.out.println("Error while handling request");
        }
    }

    private void handleGetRequest(HttpRequest httpRequest, BufferedWriter writer) throws IOException {
        if(!handleSearchAndSendFile(writer, httpRequest)) {
            sendResponse(404, "text/html", null, writer, httpRequest);
        }
    }

    private void handlePostRequest(HttpRequest httpRequest, BufferedWriter writer) {
        try {
            if (httpRequest.getPath().equals("/params_info.html")) {
        
                Path filePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), "params_info.html");
                if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    byte[] contentBytes = updateParamsInfoFile(filePath, httpRequest);
                    sendResponse(200, "text/html", contentBytes, writer, httpRequest);
                } else {
                    // create file
                    if(createParamsInfoFile()){
                        byte[] contentBytes = updateParamsInfoFile(filePath, httpRequest);
                        sendResponse(200,"text/html", contentBytes, writer, httpRequest);
                        return;
                    }
                    sendResponse(500, "text/html", null, writer, httpRequest);
                }
            } else if (httpRequest.getPath().equals("/bonus.html/delete-parameter")){  // bonus 
                Bonus.deleteParameter(writer, httpRequest);
            } else {
                if(!handleSearchAndSendFile(writer, httpRequest)) {
                    sendResponse(404, "text/html", null, writer, httpRequest);
                }
            }
        } catch (IOException e) {
            System.out.println("Error while handling request");
        }
    }       

    private void handleTraceRequest(HttpRequest httpRequest, BufferedWriter writer) throws IOException {
        String content = httpRequest.getFullRequest();
        byte[] contentBytes = content.getBytes();
        sendResponse(200, "application/octet-stream", contentBytes, writer, httpRequest);
    }

    private boolean handleSearchAndSendFile(BufferedWriter writer, HttpRequest httpRequest) throws IOException {
        try{
            String path = httpRequest.getPath();

            // bonus
            if (path.equals("/bonus.html")) {
                Bonus.dispalyBonusPage(writer, httpRequest);
                return true;
            }

            Path filePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), path);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                byte[] fileContent = Files.readAllBytes(filePath);
                sendResponse(200, determineContentType(filePath), fileContent, writer, httpRequest);
                return true;
            }
            return false;
        } catch (IOException e) {
            sendResponse(500, "text/html", null, writer, httpRequest);
            System.out.println("Error while handling request");
            return false;
        }
    }

    private byte[] updateParamsInfoFile(Path filePath, HttpRequest httpRequest) throws IOException {
        // Generate dynamic list of last parameters
        HashMap<String, String> parameters = httpRequest.getParameters();
        StringBuilder dynamicList = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            dynamicList.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>");
        }

        // Generate dynamic list of all server parameters
        HashMap<String, String> allServerParameters = MultiThreadedWebServer.getServerParam();
        StringBuilder allParameters = new StringBuilder();
        for (Map.Entry<String, String> entry : allServerParameters.entrySet()) {
            allParameters.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>");
        }

        Path htmlFilePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), "params_info.html");
        String htmlContent = new String(Files.readAllBytes(htmlFilePath));

        // Find the end of the body tag
        int bodyEndIndex = htmlContent.lastIndexOf("</body>");

        if (bodyEndIndex != -1) {
            // Append dynamic lists to the end of the body
            StringBuilder modifiedHtmlContent = new StringBuilder(htmlContent);
            modifiedHtmlContent.insert(bodyEndIndex, allParameters.toString());
            modifiedHtmlContent.insert(bodyEndIndex, "<h1>All Parameters</h1>");
            modifiedHtmlContent.insert(bodyEndIndex, dynamicList.toString());
            modifiedHtmlContent.insert(bodyEndIndex, "<h1>Last Parameters</h1>");

            byte[] contentBytes = modifiedHtmlContent.toString().getBytes();
            return contentBytes;
        } else {
            return htmlContent.getBytes();
        }
    }

    private boolean createParamsInfoFile(){
        try {
            Path filePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), "params_info.html");
            
            String content = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <title>Parameters Information</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<h1>Parameters Information</h1>\n" +
                    "</body>\n" +
                    "</html>";
            Files.write(filePath, content.getBytes());
            return true;
        } catch (IOException e) {
            System.out.println("Error while creating params_info.html file");
            return false;
        }
    }
    
    private void sendResponse(int statusCode, String contentType, byte[] content, BufferedWriter writer, HttpRequest httpRequest) throws IOException {
        try {
            String statusText = getErrorStatus(statusCode);

            if(statusCode != 200 && content == null) { // if content is null, get the default error page
                content = getContent(statusCode);
            }

            StringBuilder responseHeaders = new StringBuilder();
            responseHeaders.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");            
    
            if (httpRequest.isChunked()) {
                responseHeaders.append("Transfer-Encoding: chunked\r\n");
            } else {
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
                            String chunkSize = Integer.toHexString(bytesToWrite) + "\r\n";
                            outputStream.write(chunkSize.getBytes());
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
        } catch (Exception e) {
            System.out.println("Error while sending response");
        }
    }

    private void printRequestInfo(HttpRequest httpRequest){
        if(!httpRequest.isCorrupted()){
            System.out.println("-----------------------------");
            System.out.println("Full Request: ");
            System.out.println(httpRequest.getFullRequest());
            System.out.println("-----------------------------");
            System.out.println("Request Headers: ");
            httpRequest.printHeaders();
            System.out.println("-----------------------------");
        }
        else{
            System.out.println("Corrupted Request: ");
            System.out.println(httpRequest.getFullRequest());
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
        } else if (fileName.endsWith(".bmp")) {
            return "image/bmp";
        } else { 
            return "application/octet-stream";
        }
    }

    private String getErrorStatus(int statusCode) {
        // determine content type based on status code
        String content = "";
        switch (statusCode) {
            case 400:
                content = "Bad Request";
                break;
            case 404:
                content = "Not Found";
                break;
            case 500:
                content = "Internal Server Error";
                break;
            case 501:
                content = "Not Implemented";
                break;  
            case 200:
                content = "OK";
                break;      
            default:
                content = "Internal Server Error";
                break;
        }
        return content;
    }   

    private byte[] getContent(int statusCode) {
        // determine content type based on the status code
        String content = "";
        switch (statusCode) {
            case 400:
                content = "Bad request - The request is corrupted";
                break;
            case 404:
                content = "The requested file was not found";
                break;
            case 500:
                content = "Internal Server Error";
                break;
            case 501:
                content = "Not Implemented";
                break;       
            default:
                content = "Internal Server Error";
                break;
        }
        return content.getBytes();
    }
}
