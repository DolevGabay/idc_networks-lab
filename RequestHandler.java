// File: RequestHandler.java
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            HttpRequest httpRequest = new HttpRequest(reader);
            httpRequest.printParameters();

            if ("GET".equals(httpRequest.getMethod())) {
                handleGetRequest(httpRequest.getPath(), writer);
            } else if ("POST".equals(httpRequest.getMethod())) {
                handlePostRequest(httpRequest.getPath(), reader, writer);
            } else {
                String content = "Not Implemented";
                byte[] contentBytes = content.getBytes();
                sendResponse(501, "Not Implemented", "text/html", contentBytes, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String content = "Internal Server Error";
            byte[] contentBytes = content.getBytes();
            sendResponse(500, "Internal Server Error", "text/html", contentBytes, null);
        }
    }

    private void handleGetRequest(String path, BufferedWriter writer) throws IOException {
        System.out.println("GET request for path: " + path);

        if ("/".equals(path)) {
            path = MultiThreadedWebServer.getDefaultPage();
        }

        path = path.replaceAll("\\.\\./", "/");
        Path filePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), path);

        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            try {
                byte[] fileContent = Files.readAllBytes(filePath);
                sendResponse(200, "OK", determineContentType(filePath), fileContent, writer);
            } catch (IOException e) {
                e.printStackTrace();
                String content = "Internal Server Error";
                byte[] contentBytes = content.getBytes();
                sendResponse(500, "Internal Server Error", "text/html", contentBytes, writer);
            }
        } else {
            String content = "Not Found";
            byte[] contentBytes = content.getBytes();
            sendResponse(404, "Not Found", "text/html", contentBytes, writer);
        }
    }

    private void handlePostRequest(String path, BufferedReader reader, BufferedWriter writer) throws IOException {
        System.out.println("POST request for path: " + path);

        String content = "Post Request Handled";
        byte[] contentBytes = content.getBytes();
        sendResponse(200, "OK", "text/html", contentBytes, writer);
    }

    private void sendResponse(int statusCode, String statusText, String contentType, byte[] content, BufferedWriter writer) throws IOException {
        // Implement logic to send HTTP response
        // Include necessary headers and content
        try {
            writer.write("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
            writer.write("Content-Type: " + contentType + "\r\n");
            writer.write("Content-Length: " + content.length + "\r\n");
            writer.write("\r\n");
            writer.flush();

            try (OutputStream outputStream = clientSocket.getOutputStream()) {
                outputStream.write(content);
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Sent response header:\nHTTP/1.1 " + statusCode + " " + statusText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String determineContentType(Path filePath) {
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
            return "image/x-icon";
        } else {
            return "application/octet-stream";
        }
    }
}
