package com.example.studysprint.utils;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

/**
 * Embedded HTTP server that serves static files from the classpath.
 * Required so that JavaFX WebView treats the page as a "secure context"
 * and allows getUserMedia (camera access).
 *
 * Usage:
 *   int port = LocalWebServer.start();
 *   webEngine.load("http://localhost:" + port + "/html/face-recognition.html");
 */
public class LocalWebServer {

    private static HttpServer server = null;
    private static int port = -1;

    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("js",   "application/javascript");
        MIME_TYPES.put("css",  "text/css");
        MIME_TYPES.put("png",  "image/png");
        MIME_TYPES.put("jpg",  "image/jpeg");
        MIME_TYPES.put("json", "application/json");
    }

    /**
     * Start the server (only once). Returns the port number.
     */
    public static synchronized int start() throws IOException {
        if (server != null) return port;

        port = findFreePort();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        // Serve ANY resource from the classpath root
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();

            // Normalize: strip leading slash to get classpath-relative path
            if (path.startsWith("/")) path = path.substring(1);
            if (path.isEmpty()) path = "html/face-recognition.html";

            InputStream resource = LocalWebServer.class.getClassLoader().getResourceAsStream(path);
            if (resource == null) {
                String body = "404 Not Found: " + path;
                exchange.sendResponseHeaders(404, body.length());
                exchange.getResponseBody().write(body.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            // Determine MIME type
            String ext = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : "bin";
            String mime = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

            byte[] data = resource.readAllBytes();
            resource.close();

            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        });

        server.setExecutor(null); // uses default executor
        server.start();
        System.out.println("[LocalWebServer] Started on port " + port);
        return port;
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("[LocalWebServer] Stopped.");
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    /**
     * Returns the full URL for a given classpath resource path.
     * Example: getUrl("html/face-recognition.html")
     */
    public static String getUrl(String resourcePath) {
        if (port < 0) throw new IllegalStateException("Server not started. Call LocalWebServer.start() first.");
        return "http://localhost:" + port + "/" + resourcePath;
    }
}
