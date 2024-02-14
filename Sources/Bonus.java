import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class Bonus {

    public static void dispalyBonusPage(BufferedWriter writer, HttpRequest request) {
        try {
            writer.write("HTTP/1.1 200 OK\r\n");
            writer.write("Content-Type: text/html\r\n");
            writer.write("\r\n");
            writer.write("<html><head><title>Bonus Page</title></head><body><h1>Bonus Page</h1>");
            
            // Form to enter the parameter to delete
            writer.write("<form method=\"post\" action=\"/bonus.html/delete-parameter\">");
            writer.write("<label for=\"paramToDelete\">Enter parameter key to delete:</label><br>");
            writer.write("<input type=\"text\" id=\"paramToDelete\" name=\"paramToDelete\"><br>");
            writer.write("<input type=\"submit\" value=\"Submit\">");
            writer.write("</form>");

            writer.write("<p>Here are the current parameters:</p><ul>");

            for (Map.Entry<String, String> entry : MultiThreadedWebServer.getServerParam().entrySet()) {
                writer.write("<li>" + entry.getKey() + ": " + entry.getValue() + "</li>");
            }

            writer.write("</ul></body></html>");
            writer.write("\r\n");
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error writing response to client: " + e.getMessage());
        }
    }

    public static void deleteParameter(BufferedWriter writer, HttpRequest request) {
        String paramToDelete = request.getParameters().get("paramToDelete");
        if (paramToDelete != null) {
            MultiThreadedWebServer.removeParam(paramToDelete); // update the params_info.html file
            dispalyBonusPage(writer, request);
            try{
                HttpRequest.fillParamsInfoFile(Paths.get(MultiThreadedWebServer.getRootDirectory(), "params_info.html"), null);
            } catch (IOException e) {
                System.out.println("Error writing response to client" );
            }
        }
        else {
            try {
                writer.write("HTTP/1.1 400 Bad Request\r\n");
                writer.write("Content-Type: text/html\r\n");
                writer.write("\r\n");
                writer.write("<html><head><title>Bad Request</title></head><body><h1>Bad Request</h1>");
                writer.write("<p>Parameter to delete not found</p>");
                writer.write("</body></html>");
                writer.write("\r\n");
                writer.flush();
            } catch (IOException e) {
                System.out.println("Error writing response to client: " + e.getMessage());
            }
        }
    }
}
