import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private String method;
    private String path;
    private Map<String, String> headers;
    private String fullRequest;
    private Boolean Corrupted;
    
    private String requestedPage;
    private boolean isImage;
    private int contentLength;
    private String referer;
    private String userAgent;
    private HashMap<String, String> parameters;
    private boolean isChunked;

    public HttpRequest(BufferedReader reader) throws IOException {
        this.Corrupted = false;
        parseRequest(reader);
    }
    

    private void parseRequest(BufferedReader reader) {
        try {
            String requestLine = reader.readLine();
            if (requestLine == null) {
                System.out.println("Error reading request. Ignoring");
                Corrupted = true;
                return;
            }

            fullRequest = requestLine;

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length == 3 || requestParts.length == 2) {
                method = requestParts[0];
                
                String fullPath = requestParts[1];
                parseParameters(fullPath);
                String[] pathAndParams = requestParts[1].split("\\?");
                path = pathAndParams[0];
                if (requestParts.length == 3) {
                    if (!requestParts[2].equals("HTTP/1.1") && !requestParts[2].equals("HTTP/1.0")) {
                        Corrupted = true;
                        return;
                    }
                }                
            }
            else {
                Corrupted = true;
                return;
            }

            headers = new HashMap<>();
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                fullRequest += "\r\n" + headerLine;
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
            isChunked = headers.containsKey("chunked") && headers.get("chunked").equals("yes") ? true : false;
            
            parseBodyParameters(reader);
            
            if (!parameters.isEmpty() && !path.equals("/bonus.html/delete-parameter")) { // If the request is not for the bonus page
                MultiThreadedWebServer.addParams(parameters);
                Path filePath = Paths.get(MultiThreadedWebServer.getRootDirectory(), "params_info.html");

                if (!Files.exists(filePath)) {
                    // File does not exist, create it
                    Files.createFile(filePath);
                }
                
                fillParamsInfoFile(filePath, parameters);
            }
            
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error parsing request");
            Corrupted = true;
        }
    }

    public static void fillParamsInfoFile(Path filePath, HashMap<String, String> parameters) throws IOException {
        // Truncate the file to delete existing content
        Files.write(filePath, new byte[0]);
    
        // Now write new content to the file
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><head><title>Parameters</title></head><body>\n"); 
        htmlContent.append("<h1>Last Parameters</h1>\n");
        htmlContent.append("<table border=\"1\">\n"); 
        htmlContent.append("<tr><th>Parameter</th><th>Value</th></tr>\n");
        if(parameters != null){
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                htmlContent.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>\n"); 
            }
        }
        htmlContent.append("</table>\n"); 

        htmlContent.append("<h1>All Parameters sent to the server (updated)</h1>\n");
        htmlContent.append("<table border=\"1\">\n"); 
        htmlContent.append("<tr><th>Parameter</th><th>Value</th></tr>\n");
        if(MultiThreadedWebServer.getServerParam() != null){
            for (Map.Entry<String, String> entry : MultiThreadedWebServer.getServerParam().entrySet()) {
                htmlContent.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>\n"); 
            }
        }
        htmlContent.append("</table>\n"); 


        htmlContent.append("</body></html>\n"); 
    
        Files.write(filePath, htmlContent.toString().getBytes());
    } 

    private void parseParameters(String fullPath) {
        parameters = new HashMap<>();
        try {
            if (fullPath.contains("?")) {
                String queryString = fullPath.split("\\?")[1];
                String[] pairs = queryString.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        parameters.put(keyValue[0], keyValue[1]);
                    } else {
                        System.err.println("Error: Invalid key-value pair format in query string");
                    }
                }
            } 
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error: Malformed query string");
        }
    }
    

    private void parseBodyParameters(BufferedReader reader) {
        try {
            StringBuilder requestBody = new StringBuilder();
            int contentLengthToRead = contentLength;
    
            // Check if there is content to read
            if (contentLengthToRead > 0) {
                while (contentLengthToRead > 0) {
                    int bytesRead = reader.read();
                    if (bytesRead == -1) {
                        break;
                    }
                    requestBody.append((char) bytesRead);
                    contentLengthToRead--;
                }
    
                // Process the request body only if it was read
                String[] pairs = requestBody.toString().split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        parameters.put(keyValue[0], keyValue[1]);
                    }
                }
                if(!parameters.containsKey("important")){
                    parameters.put("important", "off");
                }
                fullRequest += "\r\n" + requestBody.toString();
            }
        } catch (IOException e) {
            System.out.println("Error reading request body");
        }
    }

    public void printHeaders() {
        try {
            StringBuilder stringHeaders = new StringBuilder();
            stringHeaders.append("Request Method: ").append(this.method).append("\r\n");
            stringHeaders.append("Requested Page: ").append(this.requestedPage).append("\r\n");
            stringHeaders.append("IsImage: ").append(this.isImage).append("\r\n");
            stringHeaders.append("Content Length: ").append(this.contentLength).append("\r\n");
            stringHeaders.append("Referer: ").append(this.referer).append("\r\n");
            stringHeaders.append("User Agent: ").append(this.userAgent).append("\r\n");
            stringHeaders.append("Is Chunked: ").append(this.isChunked).append("\r\n");
            stringHeaders.append("Corrupted: ").append(this.Corrupted).append("\r\n");
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
            System.out.println("Error printing headers");
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

    public String getFullRequest() {
        if (fullRequest == null) {
            return "the request is empty";
        }
        return fullRequest;
    }

    public Boolean isCorrupted() {
        return Corrupted;
    }

    public boolean isChunked() {
        return isChunked;
    }
}
