package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.chimpchat.api.automation.business.InstagramCommentAutomationMetaBusiness;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class InstagramCommentAutomationMetaBusinessTests {
    @Test
    void sendsPublicRepliesAsGraphRequests() throws IOException {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestQuery = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v26.0/comment-id/replies", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestQuery.set(exchange.getRequestURI().getRawQuery());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"id\":\"reply-id\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        server.start();

        try {
            InstagramCommentAutomationMetaBusiness business = createBusiness(server);
            InstagramCommentAutomationMetaBusiness.MetaReply reply =
                    business.replyToComment("comment-id", "token value", "Hello & welcome");

            assertEquals("reply-id", reply.id());
            assertEquals("/v26.0/comment-id/replies", requestPath.get());
            assertEquals("Hello & welcome", queryValue(requestQuery.get(), "message"));
            assertEquals("token value", queryValue(requestQuery.get(), "access_token"));
            assertEquals("", requestBody.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sendsPrivateRepliesThroughTheInstagramAccountMessagesEndpoint() throws IOException {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v26.0/instagram-account-id/messages", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"message_id\":\"dm-id\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        server.start();

        try {
            InstagramCommentAutomationMetaBusiness business = createBusiness(server);
            InstagramCommentAutomationMetaBusiness.MetaReply reply =
                    business.sendPrivateReply(
                            "instagram-account-id", "comment-id", "user-token", "Here: https://example.test/a&b");

            assertEquals("dm-id", reply.id());
            assertEquals("/v26.0/instagram-account-id/messages", requestPath.get());
            assertEquals("Bearer user-token", authorization.get());
            assertEquals("application/json", contentType.get());
            var payload = new ObjectMapper().readTree(requestBody.get());
            assertEquals("comment-id", payload.path("recipient").path("comment_id").asText());
            assertEquals(
                    "Here: https://example.test/a&b",
                    payload.path("message").path("text").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void listsRecentInstagramMediaAndParsesMetaTimestamps() throws IOException {
        AtomicReference<String> requestPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v26.0/account-id/media", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            byte[] response = """
                    {"data":[{"id":"media-id","caption":"Post caption","media_type":"IMAGE","permalink":"https://instagram.test/post","timestamp":"2026-11-13T10:00:00+0000"}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        server.start();

        try {
            InstagramCommentAutomationMetaBusiness business = createBusiness(server);
            var media = business.listMedia("account-id", "token value");

            assertEquals("/v26.0/account-id/media", requestPath.get());
            assertEquals(1, media.size());
            assertEquals("media-id", media.get(0).id());
            assertEquals("Post caption", media.get(0).caption());
            assertEquals(Instant.parse("2026-11-13T10:00:00Z"), media.get(0).timestamp());
        } finally {
            server.stop(0);
        }
    }

    private InstagramCommentAutomationMetaBusiness createBusiness(HttpServer server) {
        String graphUri = "http://127.0.0.1:" + server.getAddress().getPort() + "/v26.0";
        return new InstagramCommentAutomationMetaBusiness(new ObjectMapper(), graphUri);
    }

    private String queryValue(String query, String name) {
        return formValue(query, name);
    }

    private String formValue(String encoded, String name) {
        for (String part : encoded.split("&")) {
            String[] entry = part.split("=", 2);
            if (URLDecoder.decode(entry[0], StandardCharsets.UTF_8).equals(name)) {
                return entry.length == 1
                        ? ""
                        : URLDecoder.decode(entry[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
