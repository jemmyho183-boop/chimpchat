package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.chimpchat.api.channel.business.MetaInstagramBusiness;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MetaInstagramBusinessTests {
    @Test
    void subscribesConnectedAccountsToCommentAndMessageWebhooks() throws IOException {
        AtomicReference<String> subscriptionBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v26.0/oauth/access_token", exchange -> respond(exchange, """
                {"access_token":"access-token"}
                """));
        server.createContext("/v26.0/me", exchange -> respond(exchange, """
                {"client_business_id":"business-id"}
                """));
        server.createContext("/v26.0/me/assigned_instagram_accounts", exchange -> respond(exchange, """
                {"data":[{"id":"instagram-account-id","username":"chimpchat"}]}
                """));
        server.createContext("/v26.0/instagram-account-id/subscribed_apps", exchange -> {
            subscriptionBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "{\"success\":true}");
        });
        server.start();

        try {
            String graphUri = "http://127.0.0.1:" + server.getAddress().getPort() + "/v26.0";
            MetaInstagramBusiness business = new MetaInstagramBusiness(
                    new ObjectMapper(),
                    "app-id",
                    "app-secret",
                    "config-id",
                    "http://localhost/callback",
                    "http://localhost/authorize",
                    graphUri);

            MetaInstagramBusiness.MetaConnection connection = business.exchangeCode("authorization-code");

            assertEquals("business-id", connection.clientBusinessId());
            assertEquals(1, connection.accounts().size());
            assertEquals(
                    "subscribed_fields=comments,messages&access_token=access-token",
                    URLDecoder.decode(subscriptionBody.get(), StandardCharsets.UTF_8));
        } finally {
            server.stop(0);
        }
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(response);
        }
    }
}
