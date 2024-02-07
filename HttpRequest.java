import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private String method;
    private String path;
    private Map<String, String> headers;
    
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

            if ("/".equals(path)) {
                path = MultiThreadedWebServer.getDefaultPage();
            }
    
            path = path.replaceAll("\\.\\./", "/");
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

    public void printHeaders() {
        try {
            StringBuilder stringHeaders = new StringBuilder();
            stringHeaders.append("Request headers: ").append("\r\n");
            stringHeaders.append("Request Method: ").append(this.method).append("\r\n");
            stringHeaders.append("Requested Page: ").append(this.requestedPage).append("\r\n");
            stringHeaders.append("IsImage: ").append(this.isImage).append("\r\n");
            stringHeaders.append("Content Length: ").append(this.contentLength).append("\r\n");
            stringHeaders.append("Referer: ").append(this.referer).append("\r\n");
            stringHeaders.append("User Agent: ").append(this.userAgent).append("\r\n");
            if(this.parameters != null) {
                if (this.parameters.size() > 0) {
                    stringHeaders.append("Parameters: ").append("\r\n");
                    for (String key : this.parameters.keySet()) {
                        stringHeaders.append("\t").append(key).append(": ").append(this.parameters.get(key)).append("\r\n");
                    }
                }
            }
            
            System.out.println(stringHeaders.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public HashMap<String, String> getParameters() {
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
