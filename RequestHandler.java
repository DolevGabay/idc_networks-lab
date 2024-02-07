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
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRequest() throws IOException {
        // handle incoming request
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            HttpRequest httpRequest = new HttpRequest(reader);
            httpRequest.printHeaders();

            if ("GET".equals(httpRequest.getMethod())) {
                handleGetRequest(httpRequest, writer);
            } else if ("POST".equals(httpRequest.getMethod())) {
                handlePostRequest(httpRequest, reader, writer);
            } else {
                String content = "Not Implemented";
                byte[] contentBytes = content.getBytes();
                sendResponse(501, "Not Implemented", "text/html", contentBytes, writer, httpRequest);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String content = "Internal Server Error";
            byte[] contentBytes = content.getBytes();
            sendResponse(500, "Internal Server Error", "text/html", contentBytes, null, null);
        }
    }

    private void handleGetRequest(HttpRequest httpRequest, BufferedWriter writer) throws IOException {
        String path = httpRequest.getPath();

        Path filePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), path);

        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            try {
                byte[] fileContent = Files.readAllBytes(filePath);
                sendResponse(200, "OK", determineContentType(filePath), fileContent, writer, httpRequest);
            } catch (IOException e) {
                e.printStackTrace();
                String content = "Internal Server Error";
                byte[] contentBytes = content.getBytes();
                sendResponse(500, "Internal Server Error", "text/html", contentBytes, writer, httpRequest);
            }
        } else {
            String content = "Not Found";
            byte[] contentBytes = content.getBytes();
            sendResponse(404, "Not Found", "text/html", contentBytes, writer, httpRequest);
        }
    }

    private void handlePostRequest(HttpRequest httpRequest, BufferedReader reader, BufferedWriter writer) {
        try {
            HashMap<String, String> parameters = httpRequest.getParameters();
            MultiThreadedWebServer.addEmail(parameters);
            
            StringBuilder dynamicList = new StringBuilder();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                dynamicList.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>");
            }
    
            StringBuilder emailList = new StringBuilder();
            List<HashMap<String, String>> emails = MultiThreadedWebServer.getEmails();
            for (HashMap<String, String> email : emails) {
                for (Map.Entry<String, String> entry : email.entrySet()) {
                    emailList.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>");
                }
            }
    
            String htmlContent = new String(Files.readAllBytes(Paths.get(MultiThreadedWebServer.getRootDirectory(), "param_info.html")));
            htmlContent = htmlContent.replace("{{DynamicList}}", dynamicList.toString());
            htmlContent = htmlContent.replace("{{EmailList}}", emailList.toString());
            byte[] contentBytes = htmlContent.getBytes();
    
            sendResponse(200, "OK", "text/html", contentBytes, writer, httpRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
     

    private void sendResponse(int statusCode, String statusText, String contentType, byte[] content, BufferedWriter writer, HttpRequest httpRequest) throws IOException {
        try {
            StringBuilder responseHeaders = new StringBuilder();
            responseHeaders.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");
    
            // Check if the client requested chunked transfer encoding
            boolean useChunkedTransfer = httpRequest.getHeaders().containsKey("chunked") && "yes".equalsIgnoreCase(httpRequest.getHeaders().get("chunked"));
    
            if (useChunkedTransfer) {
                responseHeaders.append("Transfer-Encoding: chunked\r\n");
            } else {
                responseHeaders.append("Content-Length: ").append(content.length).append("\r\n");
            }
    
            responseHeaders.append("Content-Type: ").append(contentType).append("\r\n");
            responseHeaders.append("\r\n");
    
            System.out.println("Response Headers:\n" + responseHeaders.toString());
    
            writer.write(responseHeaders.toString());
            writer.flush();
    
            if (useChunkedTransfer) {
                // Send content in chunks
                try (OutputStream outputStream = clientSocket.getOutputStream()) {
                    int chunkSize = 1024; // You can adjust the chunk size as needed
                    for (int i = 0; i < content.length; i += chunkSize) {
                        int end = Math.min(content.length, i + chunkSize);
                        outputStream.write(Integer.toHexString(end - i).getBytes());
                        outputStream.write("\r\n".getBytes());
                        outputStream.write(content, i, end - i);
                        outputStream.write("\r\n".getBytes());
                    }
                    // Send the last chunk with size 0 to indicate end of chunks
                    outputStream.write("0\r\n\r\n".getBytes());
                    outputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // Send content in a single chunk
                try (OutputStream outputStream = clientSocket.getOutputStream()) {
                    outputStream.write(content);
                    outputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    
            System.out.println("Sent response header:\nHTTP/1.1 " + statusCode + " " + statusText);
        } catch (Exception e) {
            e.printStackTrace();
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
