// File: HttpRequest.java
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private String method;
    private String path;
    private Map<String, String> headers;
    
    private String type;
    private String requestedPage;
    private boolean isImage;
    private int contentLength;
    private String referer;
    private String userAgent;
    private HashMap<String, String> parameters;

    public HttpRequest(BufferedReader reader) throws IOException {
        parseRequest(reader);
    }

    private void parseRequest(BufferedReader reader) {
        try {
            String requestLine = reader.readLine();
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length == 3) {
                method = requestParts[0];
                path = requestParts[1];
            }
    
            headers = new HashMap<>();
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                String[] headerParts = headerLine.split(": ");
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }
    
            type = headers.get("Content-Type");
            requestedPage = path;
            isImage = path.endsWith(".bmp") || path.endsWith(".gif") || path.endsWith(".png") || path.endsWith(".jpg");
            contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
            referer = headers.getOrDefault("Referer", "");
            userAgent = headers.getOrDefault("User-Agent", "");
    
            if ("GET".equals(method)) {
                parseParameters();
            } else if ("POST".equals(method)) {
                parsePostParameters(reader);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace(); 
        }
    }

    private void parseParameters() {

        parameters = new HashMap<>();
        String queryString = path.contains("?") ? path.split("\\?")[1] : "";
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                parameters.put(keyValue[0], keyValue[1]);
            }
        }
    }

    private void parsePostParameters(BufferedReader reader) {
        try {
            System.out.println("Parsing POST parameters");
            StringBuilder requestBody = new StringBuilder();
            int contentLengthToRead = contentLength;
    
            while (contentLengthToRead > 0) {
                int bytesRead = reader.read();
                if (bytesRead == -1) {
                    break;
                }
                requestBody.append((char) bytesRead);
                contentLengthToRead--;
            }
    
            parameters = new HashMap<>();
            String[] pairs = requestBody.toString().split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    parameters.put(keyValue[0], keyValue[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); 
        }
    }
    
    public String getType() {
        return type;
    }

    public String getRequestedPage() {
        return requestedPage;
    }

    public boolean isImage() {
        return isImage;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void printParameters() {
        System.out.println("Parameters:");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }
}
