package io.github.transactionbridge;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;

public final class WebhookClientTest {
    @Test public void postsJsonAndBearerTokenToHttps() throws Exception {
        FakeConnection connection = new FakeConnection(202, "accepted");
        WebhookClient client = new WebhookClient(url -> connection, 1000, 1000);
        WebhookClient.Response response = client.post("https://example.test/hook", "{\"id\":\"x\"}", "secret");
        assertEquals(202, response.statusCode);
        assertEquals("{\"id\":\"x\"}", connection.body.toString("UTF-8"));
        assertEquals("Bearer secret", connection.getRequestProperty("Authorization"));
    }

    @Test public void rejectsPlainHttpBeforeOpeningConnection() throws Exception {
        try {
            new WebhookClient(url -> { throw new AssertionError("must not open"); }, 1000, 1000)
                    .post("http://example.test/hook", "{}", "");
            fail("plain HTTP must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("HTTPS"));
        }
    }

    private static final class FakeConnection extends HttpURLConnection {
        private final int status;
        private final byte[] response;
        private final ByteArrayInputStream input;
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();

        FakeConnection(int status, String response) throws Exception {
            super(new URL("https://example.test/hook"));
            this.status = status;
            this.response = response.getBytes("UTF-8");
            this.input = new ByteArrayInputStream(this.response);
        }

        @Override public int getResponseCode() { return status; }
        @Override public java.io.OutputStream getOutputStream() { return body; }
        @Override public java.io.InputStream getInputStream() { return input; }
        @Override public void disconnect() {}
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() throws IOException {}
    }
}
