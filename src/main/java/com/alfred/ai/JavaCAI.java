package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import okio.ByteString;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class JavaCAI {
    private static final String BASE_URL = "https://plus.character.ai/";
    private static final String NEO_URL = "https://neo.character.ai/";
    private static final String NEO_WS_URL = "wss://neo.character.ai/ws/";
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient client;
    private String accountId;
    public final User user;
    public final Character character;
    public final Chat chat;

    public JavaCAI() {
        this.client = new OkHttpClient();
        this.user = new User();
        this.character = new Character();
        this.chat = new Chat();

        this.updateAuthorization();
    }

    public void tryGetAccountId() {
        if (!MCAIMod.CONFIG.general.authorization.isBlank()) {
            try {
                JsonNode me = request("chat/user/", "GET", null).get("user").get("user");
                accountId = me.get("id").asText();
            } catch (IOException e) {
                MCAIMod.LOGGER.error("Unable to get user data. Will require restart or the `/ai authorize` to be run.");
            }
        }
    }

    public void updateAuthorization() {
        tryGetAccountId();
    }

    public JsonNode request(String url, String method, JsonNode data) throws IOException {
        Request.Builder builder = new Request.Builder().url(url.startsWith("http") ? url : BASE_URL + url);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        if ("GET".equalsIgnoreCase(method)) {
            builder.get();
        } else if ("POST".equalsIgnoreCase(method)) {
            builder.post(RequestBody.create(JSON, data != null ? data.toString() : "{}"));
        } else if ("PUT".equalsIgnoreCase(method)) {
            builder.put(RequestBody.create(JSON, data != null ? data.toString() : "{}"));
        } else if ("PATCH".equalsIgnoreCase(method)) {
            builder.patch(RequestBody.create(JSON, data != null ? data.toString() : "{}"));
        } else {
            throw new IllegalArgumentException("Invalid method: " + method);
        }

        builder.addHeader("Authorization", "Token " + MCAIMod.CONFIG.general.authorization);

        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code: " + response);
            String body = Objects.requireNonNull(response.body()).string();
            return mapper.readTree(body);
        }
    }

    private Headers buildNeoHeaders() {
        Headers.Builder hb = new Headers.Builder();
        hb.add("Authorization", "Token " + MCAIMod.CONFIG.general.authorization);
        hb.add("Accept", "application/json");
        hb.add("Content-Type", "application/json");
        return hb.build();
    }

    public class User {
        public JsonNode fetchUser(String username) throws IOException {
            ObjectNode data = mapper.createObjectNode().put("username", username);
            JsonNode res = request("chat/user/public/", "POST", data);
            if (res.has("public_user")) return res.get("public_user");
            return null;
        }

        public JsonNode fetchUserVoices(String username) throws IOException {
            JsonNode res = request(NEO_URL + "multimodal/api/v1/voices/search?creatorInfo.username=" + username, "GET", null);
            if (res.has("command") && "neo_error".equals(res.get("command").asText())) {
                throw new IOException("Cannot fetch user voices: " + res.get("comment").asText());
            }
            return res;
        }

        public boolean followUser(String username) throws IOException {
            ObjectNode data = mapper.createObjectNode().put("username", username);
            JsonNode res = request("chat/user/follow/", "POST", data);
            return res.has("status") && "OK".equals(res.get("status").asText());
        }

        public boolean unfollowUser(String username) throws IOException {
            ObjectNode data = mapper.createObjectNode().put("username", username);
            JsonNode res = request("chat/user/unfollow/", "POST", data);
            return res.has("status") && "OK".equals(res.get("status").asText());
        }
    }

    public class Character {
        public JsonNode getInfo(String characterId) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterId);
            return request(NEO_URL + "character/v1/get_character_info", "POST", data);
        }
    }

    public class Chat {
        public JsonNode fetchHistories(String characterId, int amount) throws IOException {
            ObjectNode body = mapper.createObjectNode()
                    .put("external_id", characterId)
                    .put("number", amount);
            Request req = new Request.Builder()
                    .url(NEO_URL + "chat/character/histories/")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
                    .headers(buildNeoHeaders())
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (resp.code() == 200) return json;
                throw new IOException("Cannot fetch histories. " + json);
            }
        }

        public JsonNode fetchChats(String characterId, Integer numPreviewTurns) throws IOException {
            int n = numPreviewTurns != null ? numPreviewTurns : 2;
            HttpUrl url = HttpUrl.parse(NEO_URL + "chats/").newBuilder()
                    .addQueryParameter("character_ids", characterId)
                    .addQueryParameter("num_preview_turns", String.valueOf(n))
                    .build();

            Request req = new Request.Builder()
                    .url(url)
                    .headers(buildNeoHeaders())
                    .get()
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (resp.code() == 200) return json;
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot fetch chats. " + json.path("comment").asText());
                }
                throw new IOException("Cannot fetch chats. " + json);
            }
        }

        public JsonNode fetchChat(String chatId) throws IOException {
            Request req = new Request.Builder()
                    .url(NEO_URL + "chat/" + chatId + "/")
                    .headers(buildNeoHeaders())
                    .get()
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (resp.code() == 200) {
                    return json;
                }
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot fetch chat. " + json.path("comment").asText());
                }
                throw new IOException("Cannot fetch chat. " + json);
            }
        }

        public JsonNode fetchRecentChats() throws IOException {
            Request req = new Request.Builder()
                    .url(NEO_URL + "chats/recent/")
                    .headers(buildNeoHeaders())
                    .get()
                    .build();
            try (Response resp = client.newCall(req).execute()) {
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (resp.code() == 200) return json;
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot fetch recent chats. " + json.path("comment").asText());
                }
                throw new IOException("Cannot fetch recent chats. " + json);
            }
        }

        public MessagesPage fetchMessages(String chatId, boolean pinnedOnly, String nextToken) throws IOException {
            String url = NEO_URL + "turns/" + chatId + "/";
            if (nextToken != null && !nextToken.isEmpty()) {
                url += "?next_token=" + URLEncoder.encode(nextToken, StandardCharsets.UTF_8);
            }
            Request req = new Request.Builder()
                    .url(url)
                    .headers(buildNeoHeaders())
                    .get()
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (resp.code() == 200) {
                    List<JsonNode> turns = new ArrayList<>();
                    JsonNode rawTurns = json.path("turns");
                    if (rawTurns.isArray()) {
                        for (JsonNode t : rawTurns) {
                            boolean isPinned = t.path("is_pinned").asBoolean(false);
                            if (!pinnedOnly || isPinned) turns.add(t);
                        }
                    }
                    String newNext = json.path("meta").path("next_token").isMissingNode() ? null : json.path("meta").path("next_token").asText(null);
                    return new MessagesPage(turns, newNext, json);
                }
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot fetch messages. " + json.path("comment").asText());
                }
                throw new IOException("Cannot fetch messages. " + json);
            }
        }

        public List<JsonNode> getHistory(String chatId, boolean pinnedOnly) throws IOException {
            List<JsonNode> all = new ArrayList<>();
            String next = null;
            do {
                MessagesPage page = fetchMessages(chatId, pinnedOnly, next);
                if (page.turns == null || page.turns.isEmpty()) break;
                all.addAll(page.turns);
                next = page.nextToken;
            } while (next != null && !next.isEmpty());
            return all;
        }

        public boolean deleteMessages(String chatId, List<String> turnIds) throws IOException {
            ObjectNode body = mapper.createObjectNode();
            body.putPOJO("turn_ids", turnIds);
            Request req = new Request.Builder()
                    .url(NEO_URL + "turns/" + chatId + "/remove")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
                    .headers(buildNeoHeaders())
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (resp.code() == 200) return true;
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot delete messages. " + json.path("comment").asText());
                }
                throw new IOException("Cannot delete messages. " + json);
            }
        }

        public boolean updateChatName(String chatId, String name) throws IOException {
            ObjectNode body = mapper.createObjectNode().put("name", name);
            Request req = new Request.Builder()
                    .url(NEO_URL + "chat/" + chatId + "/update_name")
                    .patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
                    .headers(buildNeoHeaders())
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                if (resp.code() == 200) return true;
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot update chat name. " + json.path("comment").asText());
                }
                throw new IOException("Cannot update chat name. " + json);
            }
        }

        public boolean archiveChat(String chatId) throws IOException {
            Request req = new Request.Builder()
                    .url(NEO_URL + "chat/" + chatId + "/archive")
                    .patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{}"))
                    .headers(buildNeoHeaders())
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                if (resp.code() == 200) return true;
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot archive chat. " + json.path("comment").asText());
                }
                throw new IOException("Cannot archive chat. " + json);
            }
        }

        public boolean unarchiveChat(String chatId) throws IOException {
            Request req = new Request.Builder()
                    .url(NEO_URL + "chat/" + chatId + "/unarchive")
                    .patch(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{}"))
                    .headers(buildNeoHeaders())
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                if (resp.code() == 200) return true;
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot unarchive chat. " + json.path("comment").asText());
                }
                throw new IOException("Cannot unarchive chat. " + json);
            }
        }

        public String copyChat(String chatId, String endTurnId) throws IOException {
            ObjectNode body = mapper.createObjectNode().put("end_turn_id", endTurnId);
            Request req = new Request.Builder()
                    .url(NEO_URL + "chat/" + chatId + "/copy")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
                    .headers(buildNeoHeaders())
                    .build();

            try (Response resp = client.newCall(req).execute()) {
                String b = Objects.requireNonNull(resp.body()).string();
                JsonNode json = mapper.readTree(b);
                if (resp.code() == 200) {
                    return json.has("new_chat_id") ? json.get("new_chat_id").asText(null) : null;
                }
                if (json.has("command") && "neo_error".equals(json.get("command").asText())) {
                    throw new IOException("Cannot copy chat. " + json.path("comment").asText());
                }
                throw new IOException("Cannot copy chat. " + json);
            }
        }

        public interface StreamListener {
            void onEvent(JsonNode event);
            void onError(Throwable t);
            void onClose();
        }

        // inefficient
        private CompletableFuture<Void> wsSendAndReceive(ObjectNode message, StreamListener listener, long timeoutMillis) {
            CompletableFuture<Void> done = new CompletableFuture<>();
            Request wsReq = new Request.Builder()
                    .url(NEO_WS_URL)
                    .headers(buildNeoHeaders())
                    .build();

            WebSocketListener wl = new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    try {
                        webSocket.send(message.toString());
                    } catch (Exception e) {
                        listener.onError(e);
                        done.completeExceptionally(e);
                        webSocket.close(1000, "error");
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    try {
                        JsonNode node = mapper.readTree(text);
                        listener.onEvent(node);
                    } catch (Exception e) {
                        listener.onError(e);
                        done.completeExceptionally(e);
                        webSocket.close(1000, "error");
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    onMessage(webSocket, bytes.utf8());
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    webSocket.close(1000, null);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    listener.onClose();
                    done.complete(null);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    listener.onError(t);
                    done.completeExceptionally(t);
                }
            };

            WebSocket ws = client.newWebSocket(wsReq, wl);

            // Safety timeout
            if (timeoutMillis > 0) {
                ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
                ses.schedule(() -> {
                    if (!done.isDone()) {
                        done.completeExceptionally(new TimeoutException("WebSocket timed out"));
                        try { ws.close(1000, "timeout"); } catch (Exception ignored) {}
                    }
                    ses.shutdown();
                }, timeoutMillis, TimeUnit.MILLISECONDS);
            }
            return done;
        }

        /**
         * Create a chat via websocket.
         * Returns a CompletableFuture that completes with a Map: keys "chat" -> JsonNode (new chat),
         * and "greeting_turn" -> JsonNode (if greeting true and greeting turn received).
         *
         * Provide a token override if needed (otherwise config token is used).
         */
        public CompletableFuture<Map<String, JsonNode>> newChat(String characterId, boolean greeting, String modelType) {
            CompletableFuture<Map<String, JsonNode>> result = new CompletableFuture<>();

            String requestId = UUID.randomUUID().toString();
            String chatId = UUID.randomUUID().toString();

            ObjectNode payload = mapper.createObjectNode();
            ObjectNode chatNode = mapper.createObjectNode();
            chatNode.put("chat_id", chatId);
            chatNode.put("creator_id", accountId);
            chatNode.put("visibility", "VISIBILITY_PRIVATE");
            chatNode.put("character_id", characterId);
            chatNode.put("type", "TYPE_ONE_ON_ONE");
            if (modelType != null && !modelType.isBlank()) chatNode.put("preferred_model_type", modelType);

            payload.set("chat", chatNode);
            payload.put("with_greeting", greeting);

            ObjectNode message = mapper.createObjectNode();
            message.put("command", "create_chat");
            message.put("request_id", requestId);
            message.set("payload", payload);

            final JsonNode[] newChat = new JsonNode[1];
            final JsonNode[] greetingTurn = new JsonNode[1];

            StreamListener l = new StreamListener() {
                @Override
                public void onEvent(JsonNode event) {
                    String cmd = event.path("command").asText("");
                    switch (cmd) {
                        case "create_chat_response" -> {
                            newChat[0] = event.path("chat");
                            if (!greeting) {
                                Map<String, JsonNode> out = new HashMap<>();
                                out.put("chat", newChat[0]);
                                out.put("greeting_turn", null);
                                result.complete(out);
                            }
                            // otherwise wait for greeting
                        }
                        case "add_turn" -> {
                            JsonNode turn = event.path("turn");
                            greetingTurn[0] = turn;
                            if (newChat[0] == null) {
                                // still accept greeting if create_chat_response comes later
                                // but if we want to require both, we'll wait
                            }
                            Map<String, JsonNode> out = new HashMap<>();
                            out.put("chat", newChat[0]);
                            out.put("greeting_turn", greetingTurn[0]);
                            result.complete(out);
                        }
                        case "neo_error" ->
                                result.completeExceptionally(new RuntimeException("Cannot create a new chat. " + event.path("comment").asText("")));
                        case null, default -> {
                        }
                        // ignore other events
                    }
                }

                @Override
                public void onError(Throwable t) {
                    result.completeExceptionally(t);
                }

                @Override
                public void onClose() {
                    if (!result.isDone()) {
                        result.completeExceptionally(new RuntimeException("Session closed before chat creation finished"));
                    }
                }
            };

            // send via websocket and let listener complete the result
            wsSendAndReceive(message, l, 120_000).exceptionally(ex -> {
                if (!result.isDone()) result.completeExceptionally(ex);
                return null;
            });

            return result;
        }

        /**
         * Send a message via websocket. If streaming == true, the provided StreamListener will be invoked
         * for each incremental event and the returned CompletableFuture completes when the stream ends.
         *
         * If streaming == false, a blocking CompletableFuture<JsonNode> is returned that completes with the final Turn node.
         */
        public CompletableFuture<JsonNode> sendMessage(String characterId, String chatId, String text, boolean streaming) {
            CompletableFuture<JsonNode> finalResult = new CompletableFuture<>();

            String candidateId = UUID.randomUUID().toString();
            String turnId = UUID.randomUUID().toString();
            String requestId = UUID.randomUUID().toString();

            ObjectNode payload = mapper.createObjectNode();
            payload.put("character_id", characterId);
            payload.put("num_candidates", 1);

            ObjectNode prevAnn = mapper.createObjectNode();
            String[] annKeys = new String[]{
                    "bad_memory","boring","ends_chat_early","funny","helpful","inaccurate","interesting","long",
                    "not_bad_memory","not_boring","not_ends_chat_early","not_funny","not_helpful","not_inaccurate","not_interesting","not_long","not_out_of_character","not_repetitive","not_short",
                    "out_of_character","repetitive","short"
            };
            for (String k : annKeys) prevAnn.put(k, 0);
            payload.set("previous_annotations", prevAnn);
            payload.put("selected_language", "");
            payload.put("tts_enabled", false);

            ObjectNode turn = mapper.createObjectNode();
            ObjectNode author = mapper.createObjectNode();
            author.put("author_id", accountId);
            author.put("is_human", true);
            author.put("name", "");
            turn.set("author", author);

            ObjectNode candidate = mapper.createObjectNode();
            candidate.put("candidate_id", candidateId);
            candidate.put("raw_content", text);

            turn.putArray("candidates").add(candidate);
            turn.put("primary_candidate_id", candidateId);

            ObjectNode turnKey = mapper.createObjectNode();
            ObjectNode tk = mapper.createObjectNode();
            tk.put("chat_id", chatId);
            tk.put("turn_id", turnId);
            turn.set("turn_key", tk);

            payload.set("turn", turn);
            payload.put("user_name", "");

            ObjectNode message = mapper.createObjectNode();
            message.put("command", "create_and_generate_turn");
            message.put("origin_id", "web-next");
            message.set("payload", payload);
            message.put("request_id", requestId);

            if (streaming) {
                // streaming path: user should use wsSendAndReceive directly with a StreamListener to receive events.
                // Here, we provide a convenience CompletableFuture that completes when the final event arrives.
                StreamListener l = new StreamListener() {
                    @Override
                    public void onEvent(JsonNode event) {
                        String cmd = event.path("command").asText("");
                        if ("neo_error".equals(cmd)) {
                            finalResult.completeExceptionally(new RuntimeException("Cannot send message. " + event.path("comment").asText("")));
                            return;
                        }
                        if ("add_turn".equals(cmd) || "update_turn".equals(cmd)) {
                            JsonNode turnNode = event.path("turn");
                            boolean authorIsHuman = turnNode.path("author").path("is_human").asBoolean(false);
                            if (authorIsHuman) {
                                // skip first human echo
                                return;
                            }
                            // emit progress by overriding: here we set the final result when candidate is final
                            JsonNode primaryCandidate = null;
                            try {
                                if (turnNode.has("candidates") && turnNode.get("candidates").isArray() && !turnNode.get("candidates").isEmpty()) {
                                    primaryCandidate = turnNode.get("candidates").get(0);
                                }
                            } catch (Exception ignored) {}

                            boolean isFinal = primaryCandidate != null && primaryCandidate.path("is_final").asBoolean(false);
                            if (isFinal) {
                                finalResult.complete(turnNode);
                            } else {
                                // intermediate; optionally could callback to user if they pass their own listener
                            }
                        } else if ("filter_user_input_self_harm".equals(cmd)) {
                            finalResult.completeExceptionally(new RuntimeException("Cannot send message. Self harm message detected"));
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        finalResult.completeExceptionally(t);
                    }

                    @Override
                    public void onClose() {
                        if (!finalResult.isDone()) {
                            finalResult.completeExceptionally(new RuntimeException("Stream closed without final result"));
                        }
                    }
                };

                wsSendAndReceive(message, l, 120_000).exceptionally(ex -> {
                    if (!finalResult.isDone()) finalResult.completeExceptionally(ex);
                    return null;
                });

            } else {
                // non-streaming: collect events until final candidate, then complete
                StreamListener l = new StreamListener() {
                    @Override
                    public void onEvent(JsonNode event) {
                        String cmd = event.path("command").asText("");
                        if ("neo_error".equals(cmd)) {
                            finalResult.completeExceptionally(new RuntimeException("Cannot send message. " + event.path("comment").asText("")));
                            return;
                        }
                        if ("add_turn".equals(cmd) || "update_turn".equals(cmd)) {
                            JsonNode turnNode = event.path("turn");
                            boolean authorIsHuman = turnNode.path("author").path("is_human").asBoolean(false);
                            if (authorIsHuman) return;
                            JsonNode primaryCandidate = null;
                            if (turnNode.has("candidates") && turnNode.get("candidates").isArray() && !turnNode.get("candidates").isEmpty()) {
                                primaryCandidate = turnNode.get("candidates").get(0);
                            }
                            if (primaryCandidate != null && primaryCandidate.path("is_final").asBoolean(false)) {
                                finalResult.complete(turnNode);
                            }
                        } else if ("filter_user_input_self_harm".equals(cmd)) {
                            finalResult.completeExceptionally(new RuntimeException("Cannot send message. Self harm message detected"));
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        finalResult.completeExceptionally(t);
                    }

                    @Override
                    public void onClose() {
                        if (!finalResult.isDone()) {
                            finalResult.completeExceptionally(new RuntimeException("Stream closed without final result"));
                        }
                    }
                };

                wsSendAndReceive(message, l, 120_000).exceptionally(ex -> {
                    if (!finalResult.isDone()) finalResult.completeExceptionally(ex);
                    return null;
                });
            }

            return finalResult;
        }

        public CompletableFuture<JsonNode> anotherResponse(String characterId, String chatId, String turnId) {
            CompletableFuture<JsonNode> finalResult = new CompletableFuture<>();
            String requestId = UUID.randomUUID().toString();

            ObjectNode payload = mapper.createObjectNode();
            payload.put("character_id", characterId);

            ObjectNode prevAnn = mapper.createObjectNode();
            String[] annKeys = new String[]{
                    "bad_memory","boring","ends_chat_early","funny","helpful","inaccurate","interesting","long",
                    "not_bad_memory","not_boring","not_ends_chat_early","not_funny","not_helpful","not_inaccurate","not_interesting","not_long","not_out_of_character","not_repetitive","not_short",
                    "out_of_character","repetitive","short"
            };
            for (String k : annKeys) prevAnn.put(k, 0);
            payload.set("previous_annotations", prevAnn);
            payload.put("selected_language", "");
            payload.put("tts_enabled", false);

            ObjectNode tk = mapper.createObjectNode();
            tk.put("chat_id", chatId);
            tk.put("turn_id", turnId);
            payload.set("turn_key", tk);
            payload.put("user_name", "");

            ObjectNode message = mapper.createObjectNode();
            message.put("command", "generate_turn_candidate");
            message.put("origin_id", "web-next");
            message.set("payload", payload);
            message.put("request_id", requestId);

            StreamListener l = new StreamListener() {
                @Override
                public void onEvent(JsonNode event) {
                    String cmd = event.path("command").asText("");
                    if ("neo_error".equals(cmd)) {
                        finalResult.completeExceptionally(new RuntimeException("Cannot generate another response. " + event.path("comment").asText("")));
                        return;
                    }
                    if ("update_turn".equals(cmd)) {
                        JsonNode turn = event.path("turn");
                        JsonNode primaryCandidate = null;
                        if (turn.has("candidates") && turn.get("candidates").isArray() && turn.get("candidates").size() > 0) {
                            primaryCandidate = turn.get("candidates").get(0);
                        }
                        if (primaryCandidate != null && primaryCandidate.path("is_final").asBoolean(false)) {
                            finalResult.complete(turn);
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    finalResult.completeExceptionally(t);
                }

                @Override
                public void onClose() {
                    if (!finalResult.isDone()) finalResult.completeExceptionally(new RuntimeException("Session closed"));
                }
            };

            wsSendAndReceive(message, l, 120_000).exceptionally(ex -> {
                if (!finalResult.isDone()) finalResult.completeExceptionally(ex);
                return null;
            });

            return finalResult;
        }

        /**
         * Edit a message's candidate via websocket.
         * Returns CompletableFuture<JsonNode> with the updated turn.
         */
        public CompletableFuture<JsonNode> editMessage(String chatId, String turnId, String candidateId, String newText) {
            CompletableFuture<JsonNode> finalResult = new CompletableFuture<>();
            String requestId = UUID.randomUUID().toString();

            ObjectNode payload = mapper.createObjectNode();
            ObjectNode turnKey = mapper.createObjectNode();
            turnKey.put("chat_id", chatId);
            turnKey.put("turn_id", turnId);
            payload.set("turn_key", turnKey);
            payload.put("current_candidate_id", candidateId);
            payload.put("new_candidate_raw_content", newText);

            ObjectNode message = mapper.createObjectNode();
            message.put("command", "edit_turn_candidate");
            message.put("request_id", requestId);
            message.set("payload", payload);
            message.put("origin_id", "web-next");

            StreamListener l = new StreamListener() {
                @Override
                public void onEvent(JsonNode event) {
                    String cmd = event.path("command").asText("");
                    if ("neo_error".equals(cmd)) {
                        finalResult.completeExceptionally(new RuntimeException("Cannot edit message. " + event.path("comment").asText("")));
                        return;
                    }
                    if ("update_turn".equals(cmd)) {
                        finalResult.complete(event.path("turn"));
                    }
                }

                @Override
                public void onError(Throwable t) {
                    finalResult.completeExceptionally(t);
                }

                @Override
                public void onClose() {
                    if (!finalResult.isDone()) finalResult.completeExceptionally(new RuntimeException("Session closed"));
                }
            };

            wsSendAndReceive(message, l, 60_000).exceptionally(ex -> {
                if (!finalResult.isDone()) finalResult.completeExceptionally(ex);
                return null;
            });

            return finalResult;
        }

        /**
         * Delete messages (REST wrapper).
         */
        public boolean deleteMessage(String chatId, String turnId) throws IOException {
            return deleteMessages(chatId, Collections.singletonList(turnId));
        }

        /**
         * Pin a message via websocket.
         */
        public CompletableFuture<Boolean> pinMessage(String chatId, String turnId) {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            String reqId = UUID.randomUUID().toString();

            ObjectNode payload = mapper.createObjectNode();
            ObjectNode turnKey = mapper.createObjectNode();
            turnKey.put("chat_id", chatId);
            turnKey.put("turn_id", turnId);
            payload.put("is_pinned", true);
            payload.set("turn_key", turnKey);

            ObjectNode message = mapper.createObjectNode();
            message.put("command", "set_turn_pin");
            message.put("origin_id", "web-next");
            message.set("payload", payload);
            message.put("request_id", reqId);

            StreamListener l = new StreamListener() {
                @Override
                public void onEvent(JsonNode event) {
                    String cmd = event.path("command").asText("");
                    if ("neo_error".equals(cmd)) {
                        result.completeExceptionally(new RuntimeException("Cannot pin message. " + event.path("comment").asText("")));
                        return;
                    }
                    if ("update_turn".equals(cmd)) {
                        boolean isPinned = event.path("turn").path("is_pinned").asBoolean(false);
                        result.complete(isPinned);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    result.completeExceptionally(t);
                }

                @Override
                public void onClose() {
                    if (!result.isDone()) result.completeExceptionally(new RuntimeException("Session closed"));
                }
            };

            wsSendAndReceive(message, l, 30_000).exceptionally(ex -> {
                if (!result.isDone()) result.completeExceptionally(ex);
                return null;
            });

            return result;
        }

        /**
         * Unpin a message via websocket.
         */
        public CompletableFuture<Boolean> unpinMessage(String chatId, String turnId) {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            String reqId = UUID.randomUUID().toString();

            ObjectNode payload = mapper.createObjectNode();
            ObjectNode turnKey = mapper.createObjectNode();
            turnKey.put("chat_id", chatId);
            turnKey.put("turn_id", turnId);
            payload.put("is_pinned", false);
            payload.set("turn_key", turnKey);

            ObjectNode message = mapper.createObjectNode();
            message.put("command", "set_turn_pin");
            message.put("origin_id", "web-next");
            message.set("payload", payload);
            message.put("request_id", reqId);

            StreamListener l = new StreamListener() {
                @Override
                public void onEvent(JsonNode event) {
                    String cmd = event.path("command").asText("");
                    if ("neo_error".equals(cmd)) {
                        result.completeExceptionally(new RuntimeException("Cannot unpin message. " + event.path("comment").asText("")));
                        return;
                    }
                    if ("update_turn".equals(cmd)) {
                        boolean isPinned = event.path("turn").path("is_pinned").asBoolean(true);
                        result.complete(!isPinned);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    result.completeExceptionally(t);
                }

                @Override
                public void onClose() {
                    if (!result.isDone()) result.completeExceptionally(new RuntimeException("Session closed"));
                }
            };

            wsSendAndReceive(message, l, 30_000).exceptionally(ex -> {
                if (!result.isDone()) result.completeExceptionally(ex);
                return null;
            });

            return result;
        }

        /**
         * Update primary candidate (websocket).
         */
        public CompletableFuture<Boolean> updatePrimaryCandidate(String chatId, String turnId, String candidateId) {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            ObjectNode payload = mapper.createObjectNode();
            payload.put("candidate_id", candidateId);

            ObjectNode turnKey = mapper.createObjectNode();
            turnKey.put("chat_id", chatId);
            turnKey.put("turn_id", turnId);
            payload.set("turn_key", turnKey);

            ObjectNode wsMessage = mapper.createObjectNode();
            wsMessage.put("command", "update_primary_candidate");
            wsMessage.put("origin_id", "web-next");
            wsMessage.set("payload", payload);

            StreamListener l = new StreamListener() {
                @Override
                public void onEvent(JsonNode event) {
                    String cmd = event.path("command").asText("");
                    if ("neo_error".equals(cmd)) {
                        result.completeExceptionally(new RuntimeException("Cannot update primary candidate. " + event.path("comment").asText("")));
                        return;
                    }
                    if ("ok".equals(cmd)) {
                        result.complete(true);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    result.completeExceptionally(t);
                }

                @Override
                public void onClose() {
                    if (!result.isDone()) result.complete(false);
                }
            };

            wsSendAndReceive(wsMessage, l, 30_000).exceptionally(ex -> {
                if (!result.isDone()) result.completeExceptionally(ex);
                return null;
            });

            return result;
        }
    }

    public JsonNode ping() throws IOException {
        return request("ping/", "GET", null);
    }

    public record MessagesPage(List<JsonNode> turns, String nextToken, JsonNode raw) { }
}
