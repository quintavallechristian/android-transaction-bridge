package io.github.transactionbridge;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** One generic POST adapter. It never chooses an endpoint path for the caller. */
public final class WebhookClient {
    public interface ConnectionFactory {
        HttpURLConnection open(URL url) throws IOException;
    }

    public static final class Response {
        public final int statusCode;
        public final String retryAfter;

        Response(int statusCode, String retryAfter) {
            this.statusCode = statusCode;
            this.retryAfter = retryAfter;
        }
    }

    private final ConnectionFactory factory;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public WebhookClient() {
        this(new ConnectionFactory() {
            @Override public HttpURLConnection open(URL url) throws IOException {
                return (HttpURLConnection) url.openConnection();
            }
        }, 15_000, 15_000);
    }

    public WebhookClient(ConnectionFactory factory, int connectTimeoutMillis, int readTimeoutMillis) {
        if (factory == null) throw new IllegalArgumentException("connection factory is required");
        if (connectTimeoutMillis <= 0 || readTimeoutMillis <= 0) throw new IllegalArgumentException("timeouts must be positive");
        this.factory = factory;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public Response post(String endpoint, String payload, String bearerToken) throws IOException {
        URL url = new URL(endpoint);
        if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IllegalArgumentException("webhook endpoint must use HTTPS");
        if (payload == null) throw new IllegalArgumentException("payload is required");

        HttpURLConnection connection = factory.open(url);
        try {
            connection.setConnectTimeout(connectTimeoutMillis);
            connection.setReadTimeout(readTimeoutMillis);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (bearerToken != null && !bearerToken.trim().isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
            return new Response(connection.getResponseCode(), connection.getHeaderField("Retry-After"));
        } finally {
            connection.disconnect();
        }
    }
}
