import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


public final class WebServer {
    // For debugging purposes
    static final boolean DEBUG = true;

    // Store mime-types in Hashtable
    static Map<String, String> mimeTypesMap = Collections.synchronizedMap(new HashMap<String, String>());
    static boolean isMimeFlagSet = false;

    public static void main(String args[]) throws Exception {
        String mimeFilePath = null;

        // Check if -mime flag set, store mime.file content
        if (args.length > 1 && args[0].equals("-mime")) {
            isMimeFlagSet = true;
            mimeFilePath = args[1];

            // Open file and store mime types
            File mimeFile = new File(mimeFilePath);
            try (BufferedReader reader = new BufferedReader(new FileReader(mimeFile))) {
                String line;
                
                while ( (line = reader.readLine()) != null ) {
                    // Ignore garbage from file
                    if (line.isEmpty()) continue;
                    else if (line.startsWith(" ")) continue;
                    else if (line.startsWith("##")) continue;
                    else if (line.startsWith("# ")) continue;
                    if (line.length() == 1 && line.startsWith("#")) continue;
                    
                    // Separate line componentes
                    String[] lineParts = line.split("\\s+");
                    if (lineParts.length > 1) {
                        // 1st element always our value
                        for (int i = 1; i < lineParts.length; i++) {
                            mimeTypesMap.put(lineParts[i], lineParts[0]);
                        }
                    }
                }
            }
            catch (Exception ex) {
                System.out.println(ex);
            }
        }

        // Set the port number
        int port = 6789;

        // Establishing the listen socket
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Process HTTP service requests
            while (true) {
                Socket s = serverSocket.accept();
                if (s.isConnected()) {
                    // Process the HTTP request
                    HttpRequest request = new HttpRequest(s);
                    // Create new Threada to process request
                    Thread thread = new Thread(request);
                    // Starting the thread
                    thread.start();
                }
            }
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }
}

final class HttpRequest implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }

    public void run() {
        try {
            processHttpRequest();
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private void processHttpRequest() throws Exception {
        // Flags for request and request method
        boolean isValidRequestMethod = true;
        boolean isValidRequest = true;

        // Entity body parameters
        Map<String, String> entityBodyParams = null;

        // Get host IP for and display later in 404 Page
        String hostIp = socket.getInetAddress().getHostAddress();

        // Get reference to sockets input and output streams
        InputStream inputStream = socket.getInputStream();
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        // Set up input stream filters
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        
        // Get request line of HTTP request message
        String requestLine = bufferedReader.readLine();

        // Display request line
        System.out.println();
        System.out.println("Sending Request...");
        System.out.println(requestLine);

        // Get and display the header line
        String headerLine;
        String userAgent = "";
        int contentLength = 0;
        while ((headerLine = bufferedReader.readLine()).length() != 0) {
            if (
                headerLine.length() > "user-agent".length() &&
                headerLine.substring(0, "user-agent".length()).toLowerCase().startsWith("user-agent")
            ) {
                userAgent = headerLine;
            }
            else if (
                headerLine.length() > "content-length".length() &&
                headerLine.substring(0, "content-length".length()).toLowerCase().startsWith("content-length")
            ) {
                try { contentLength = Integer.parseInt(headerLine.split("\\s")[1]); }
                catch (Exception ex) { contentLength = 0; }
            }

            System.out.println(headerLine);
        }

        // Check if valid request
        String requestMethod;
        String fileName;
        String httpTagAndVersion;
        String requestLineParts[] = requestLine.split("\\s");
        if (requestLineParts.length != 3) { isValidRequest = false; }
        else {
            // Extract request method and fileName from request line
            StringTokenizer tokens = new StringTokenizer(requestLine);
            requestMethod = tokens.nextToken();
            fileName = tokens.nextToken();
            httpTagAndVersion = tokens.nextToken();

            // Check if request method is valid and change fileName to ./fileName
            isValidRequestMethod = validateRequestMethod(requestMethod);
            fileName = prepareFileName(fileName);

            // Check if HTTP method is 1.0
            if (httpTagAndVersion.split("/").length != 2) { isValidRequest = false; }
            else {
                if (
                    httpTagAndVersion.split("/")[0].equals("HTTP") &&
                    httpTagAndVersion.split("/")[1].equals("1.0") &&
                    isValidRequestMethod
                ) {    
                    isValidRequest = true;
                }
                else {
                    isValidRequest = false;
                }
            }

            // Extract entity body from POST requests
            if (isValidRequest && requestMethod.equals("POST") && contentLength > 0) {
                String entityBody;
                entityBodyParams = new HashMap<String, String>();

                // Read contents of entity body
                char[] buffer = new char[contentLength];
                //bufferedReader.read(buffer, 0, contentLength);
                int actualCharsRead = bufferedReader.read(buffer, 0, contentLength);
                
                if (actualCharsRead < contentLength) {
                    isValidRequest = false;
                }
                else {
                    entityBody = new String(buffer, 0, buffer.length);
            
                    // Check if parameters of form foo=bar&lorem=ipsum
                    // otherwise invalid request
                    try {
                        for (String entry : entityBody.split("&")) {
                            entityBodyParams.put(entry.split("=")[0], entry.split("=")[1]);
                        }
                    }
                    catch (Exception ex) {
                        isValidRequest = false;
                    }
                    if (WebServer.DEBUG) System.out.print("Entity-Body: " + entityBody);
                }
            }
            
            // Prepare response msg
            prepareResponse(
                isValidRequestMethod,
                requestMethod,
                isValidRequest,
                fileName,
                hostIp,
                userAgent,
                contentLength,
                outputStream
            );
        }

        inputStream.close();
        outputStream.close();
        inputStreamReader.close();
        bufferedReader.close();
        socket.close();
    }

    private boolean validateRequestMethod(String method) {
        return method.equals("GET") || method.equals("HEAD") || method.equals("POST");
    }

    private String prepareFileName(String fileName) {
        return "." + fileName;
    }

    private void prepareResponse(
        boolean isValidRequestMethod,
        String requestMethod,
        boolean isValidRequest,
        String fileName,
        String hostIp,
        String userAgent,
        int contentLength,
        DataOutputStream outputStream
    ) throws Exception {
        // Open requested file
        FileInputStream fileInputStream = null;
        boolean isFileExisting = true;
        try {
            fileInputStream = new FileInputStream(fileName);
        }
        catch (FileNotFoundException ex) {
            isFileExisting = false;
        }

        // Construct response msg
        String statusLine = null;
        int statusCode;
        String statusText;
        String contentTypeLine = null;
        String entityBody = null;

        // Preapare '501 Not Implemented' response
        if (!isValidRequestMethod) {
            statusCode = 501;
            statusText = "Not Implemented";

            statusLine = "HTTP/1.0 " + statusCode + " " + statusText + CRLF;
            contentTypeLine = "Content-type: text/html " + CRLF;
            entityBody = 
                "<HTML>" +
                    "<HEAD>" +
                        "<TITLE>Not Implemented</TITLE>" +
                    "</HEAD>" +
                    "<BODY>Not Implemented</BODY>" + 
                "</HTML>";
        }
        else {
            if (!isValidRequest) {
                statusCode = 400;
                statusText = "Bad Request";
    
                statusLine = "HTTP/1.0 " + statusCode + " " + statusText + CRLF;
                contentTypeLine = "Content-type: text/html " + CRLF;
                entityBody = 
                    "<HTML>" +
                        "<HEAD>" +
                            "<TITLE>Bad Request</TITLE>" +
                        "</HEAD>" +
                        "<BODY>Bad Request</BODY>" +
                    "</HTML>";
            }
            else if (isFileExisting) {
                statusCode = 200;
                statusText = "OK";
    
                statusLine = "HTTP/1.0 " + statusCode + " " + statusText + CRLF;
                contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
            }
            else {
                statusCode = 404;
                statusText = "Not Found";
    
                statusLine = "HTTP/1.0 " + statusCode + " " + statusText + CRLF;
                contentTypeLine = "Content-type: text/html " + CRLF;
                entityBody = 
                    "<HTML>" +
                        "<HEAD>" +
                            "<TITLE>Not Found</TITLE>" +
                        "</HEAD>" +
                        "<BODY>" +
                            "Not Found<BR/>" + 
                            "Host-IP: " + hostIp + "<BR/>" +
                            userAgent + "<BR/>" + 
                        "</BODY>" +
                    "</HTML>";
            }
        }

        System.out.println();
        System.out.println("Sending Response...");
        // Send status line
        outputStream.writeBytes(statusLine);
        if (WebServer.DEBUG) System.out.print(statusLine);

        // Send content type line
        outputStream.writeBytes(contentTypeLine);
        if (WebServer.DEBUG) System.out.print(contentTypeLine);
        
        // Send a blank line to indicate end of header lines
        outputStream.writeBytes(CRLF);
        if (WebServer.DEBUG) System.out.println();

        // Send the entity body, except for HEAD requests
        if (!requestMethod.equals("HEAD")) {
            if (isValidRequest && isFileExisting) {
                sendBytes(fileInputStream, outputStream);
                fileInputStream.close();
            }
            else {
                outputStream.writeBytes(entityBody);
                if (WebServer.DEBUG) System.out.println(entityBody);
            }
        }
        outputStream.flush();
    }

    private static String contentType(String fileName) {
        String[] fileParts = fileName.split("\\.");
        if (WebServer.mimeTypesMap.containsKey(fileParts[fileParts.length-1]))
            return WebServer.mimeTypesMap.get(fileParts[fileParts.length-1]);
        
        return "application/octet-stream";
    }

    private static void sendBytes(FileInputStream fileInputStream, OutputStream outputStream) throws Exception{
        // Construct a 1K buffer to hold bytes on their way to the socket
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copy requested file into the socket's output stream
        while ((bytes = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytes);
        }
    }
}