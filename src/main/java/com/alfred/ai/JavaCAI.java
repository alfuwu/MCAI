package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.Objects;

public class JavaCAI {
    private static final String BASE_URL = "https://beta.character.ai/";
    private final String token;
    private final OkHttpClient client;
    public final User user;
    public final Post post;
    public final Character character;
    public final Chat chat;

    public JavaCAI(String token) {
        this.token = token;
        this.client = new OkHttpClient();
        this.user = new User();
        this.post = new Post();
        this.character = new Character();
        this.chat = new Chat();
    }

    public JsonNode request(String url, String method, JsonNode data) throws IOException {
        String link = BASE_URL + url;

        Request.Builder requestBuilder;

        if ("GET".equals(method)) {
            requestBuilder = new Request.Builder().url(link);
        } else if ("POST".equals(method) || "POST-SPLIT".equals(method)) {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, data.toString());
            requestBuilder = new Request.Builder().url(link).post(requestBody);
        } else if ("PUT".equals(method) || "PUT-SPLIT".equals(method)) {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, data.toString());
            requestBuilder = new Request.Builder().url(link).put(requestBody);
        } else {
            throw new IllegalArgumentException("Invalid HTTP method: " + method);
        }

        Request request = requestBuilder
                .addHeader("Authorization", "Token " + token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            ObjectMapper objectMapper = new ObjectMapper();
            if (method.endsWith("-SPLIT")) {
                String text = "[" + Objects.requireNonNull(response.body()).string().replace("\n", "").replace("}{", "},{") + "]";
                JsonNode jsonNode = objectMapper.readTree(text);
                return jsonNode.get(jsonNode.size() - 1);
            }
            return objectMapper.readTree(Objects.requireNonNull(response.body()).string());
        }
    }

    /**
     * A class for obtaining info about users.
     * <p></p>
     * <code>user.getInfo()</code><br></br>
     * <code>user.getUserProfile('USERNAME')</code><br></br>
     * <code>user.getFollowers()</code><br></br>
     * <code>user.getFollowing()</code><br></br>
     * <code>user.update('USERNAME')</code> -- not implementing
     */
    public class User {
        public JsonNode getInfo() throws IOException {
            return request("chat/user/", "GET", null);
        }

        public JsonNode getUserProfile(String username) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("username", username);
            return request("chat/user/public", "GET", data);
        }

        public JsonNode getFollowers() throws IOException {
            return request("chat/user/followers", "GET", null);
        }

        public JsonNode getFollowing() throws IOException {
            return request("chat/user/following", "GET", null);
        }

        public JsonNode recent() throws IOException {
            return request("chat/user/recent", "GET", null);
        }

        public JsonNode characters() throws IOException {
            return request("chat/characters/?scope=user", "GET", null);
        }

        /*/ public JsonNode update(String username) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("username", username);
            return request("chat/user/public", "GET", data);
        } /*/
    }

    /**
     * A class for messing about with posts on the site.
     * <p></p>
     * <code>post.getPost('POST_ID')
     * <code>post.getMyPosts()</code><br></br>
     * <code>post.getPosts('USERNAME')</code><br></br>
     * <code>post.upvote('POST_ID')</code><br></br>
     * <code>post.undoUpvote('POST_ID')</code><br></br>
     * <code>post.sendComment('POST_ID', 'TEXT')</code><br></br>
     * <code>post.deleteComment('MESSAGE_ID', 'POST_ID')</code><br></br>
     * <code>post.create('HISTORY_ID', 'TITLE')</code><br></br>
     * <code>post.delete('POST_ID')</code>
     */
    public class Post {

        public JsonNode getPost(String postId) throws IOException {
            return request("chat/post/?post=" + postId, "GET", null);
        }

        public JsonNode getMyPosts(int postsPage, int postsToLoad) throws IOException {
            String url = String.format("chat/posts/user/?scope=user&page=%d&posts_to_load=%d", postsPage, postsToLoad);
            return request(url, "GET", null);
        }

        /*
        def my(
            self, *, posts_page: int = 1,
            posts_to_load: int = 5, token: str = None
        ):
            return PyCAI.request(
                f'chat/posts/user/?scope=user&page={posts_page}'
                f'&posts_to_load={posts_to_load}/',
                self.session
            )

        def get_posts(
            self, username: str, *,
            posts_page: int = 1, posts_to_load: int = 5,
        ):
            return PyCAI.request(
                f'chat/posts/user/?username={username}'
                f'&page={posts_page}&posts_to_load={posts_to_load}/',
                self.session
            )

        def upvote(
            self, post_external_id: str,
            *, token: str = None
        ):
            return PyCAI.request(
                'chat/post/upvote/', self.session,
                token=token, method='POST',
                data={
                    'post_external_id': post_external_id
                }
            )

        def undo_upvote(
            self, post_external_id: str,
            *, token: str = None
        ):
            return PyCAI.request(
                'chat/post/undo-upvote/', self.session,
                token=token, method='POST',
                data={
                    'post_external_id': post_external_id
                }
            )

        def send_comment(
            self, post_id: str, text: str, *,
            parent_uuid: str = None, token: str = None
        ):
            return PyCAI.request(
                'chat/comment/create/', self.session,
                token=token, method='POST',
                data={
                    'post_external_id': post_id,
                    'text': text,
                    'parent_uuid': parent_uuid
                }
            )

        def delete_comment(
            self, message_id: int, post_id: str,
            *, token: str = None
        ):
            return PyCAI.request(
                'chat/comment/delete/', self.session,
                token=token, method='POST',
                data={
                    'external_id': message_id,
                    'post_external_id': post_id
                }
            )

        def create(
            self, post_type: str, external_id: str,
            title: str, text: str = '',
            post_visibility: str = 'PUBLIC',
            token: str = None, **kwargs
        ):
            if post_type == 'POST':
                post_link = 'chat/post/create/'
                data = {
                    'post_title': title,
                    'topic_external_id': external_id,
                    'post_text': text,
                    **kwargs
                }
            elif post_type == 'CHAT':
                post_link = 'chat/chat-post/create/'
                data = {
                    'post_title': title,
                    'subject_external_id': external_id,
                    'post_visibility': post_visibility,
                    **kwargs
                }
            else:
                raise errors.PostTypeError('Invalid post_type')

            return PyCAI.request(
                post_link, self.session,
                token=token, method='POST'
            )

        def delete(
            self, post_id: str, *,
            token: str = None
        ):
            return PyCAI.request(
                'chat/post/delete/', self.session,
                token=token, method='POST',
                data={
                    'external_id': post_id
                }
            )

        def get_topics(self):
            return PyCAI.request(
                'chat/topics/', self.session
            )

        def feed(
            self, topic: str, num: int = 1,
            load: int = 5, sort: str = 'top', *,
            token: str = None
        ):
            return PyCAI.request(
                f'chat/posts/?topic={topic}&page={num}'
                f'&posts_to_load={load}&sort={sort}',
                self.session, token=token
            )
        */
    }

    /**
     * A class for managing and obtaining information about characters.
     * <p></p>
     * <code>character.create()</code><br></br>
     * <code>character.update()</code><br></br>
     * <code>character.getTrending()</code><br></br>
     * <code>character.getRecommended()</code><br></br>
     * <code>character.getCategories()</code><br></br>
     * <code>character.getInfo('CHAR')</code><br></br>
     * <code>character.search('QUERY')</code><br></br>
     * <code>character.getVoices()</code><br>
     */
    public class Character {
        /*
         * Creates a new character.
         * Arguments:
         *     greeting: String, the initial message the character sends at the start of a chat
         *     identifier: String, character `tgt`. the site generates a special value, but you can write anything there
         *     name: String, the name of the character
         *     avatarRelPath: String, the path to the avatar image on the server. you can create this path with the uploadImage() function
         *     baseImgPrompt: String, most likely so you can do a basic setup of prompts for images
         *     imgGenEnabled: boolean, enables the character image generation abilities
         *     shortDescription: String, a very short description of the character, max 50 characters
         *     categories: String[], the categories the AI falls under
         *     definition: String, a long description of the character, max 32k characters (cuts off at ~3.2k chars)
         *     copyable: boolean, whether the character's details are public
         *     description: String, a 500-character-long description of the character
         *     visibility: "PUBLIC", "UNLISTED", "PRIVATE", the visibility of the character ("public" means everyone can see the character, "unlisted" means anyone with a link can see the character, and "private" means only the creator can see the character)
         */
        public JsonNode create(
                String greeting, String identifier, String name,
                String shortDescription, String[] categories, String definition,
                boolean copyable, String description, String visibility
        ) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("identifier", identifier)
                    .put("name", name)
                    .putPOJO("categories", categories)
                    .put("title", shortDescription)
                    .put("visibility", visibility)
                    .put("copyable", copyable)
                    .put("description", description)
                    .put("greeting", greeting)
                    .put("definition", definition);
            return request("../chat/character/create/", "POST", data);
        }

        /*
         * Updates a character's properties.
         * Arguments:
         *     characterId: String, the character's ID
         *     greeting: String,
         *     shortDescription: String, a short, ~5-word description of the character
         *     categories: String[],
         *     definition: String,
         *     copyable: boolean,
         *     description: String,
         *     visibility: "PUBLIC", "UNLISTED", "PRIVATE",
         */
        public JsonNode update(
                String characterId, String greeting, String name,
                String shortDescription, String[] categories, String definition,
                boolean copyable, String description, String visibility
        ) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterId)
                    .put("name", name)
                    .putPOJO("categories", categories)
                    .put("title", shortDescription)
                    .put("visibility", visibility)
                    .put("copyable", copyable)
                    .put("description", description)
                    .put("greeting", greeting)
                    .put("definition", definition);
            return request("../chat/character/update/", "POST", data);
        }

        /*
         * Gets metadata about trending characters.
         */
        public JsonNode getTrending() throws IOException {
            return request("chat/characters/trending", "GET", null);
        }

        /*
         * Gets metadata about recommended characters.
         */
        public JsonNode getRecommended() throws IOException {
            return request("chat/characters/recommended", "GET", null);
        }

        /*
         * Retrieves all the categories you can give to a character.
         */
        public JsonNode getCategories() throws IOException {
            return request("chat/character/categories", "GET", null);
        }

        /**
         * @deprecated
         */
        /*
         * Obtains info about a specific character.
         * Arguments:
         *     characterId: String, the character's ID
         */
        public JsonNode deprecatedGetInfo(String characterId) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterId);
            return request("chat/character/", "POST", data);
        }

        /*
         * Obtains info about a specific character, even if you do not have the necessary permissions to view that information.
         * Arguments:
         *     characterId: String, the character's ID
         */
        public JsonNode getInfo(String characterId) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterId);
            return request("chat/character/info/", "POST", data);
        }

        /*
         * Retrieves all available voices you can give to a character.
         */
        public JsonNode getVoices() throws IOException {
            return request("chat/character/voices", "GET", null);
        }

        /*
         * Gets metadata about relevant characters according to a query.
         * Arguments:
         *     query: String, the search topic
         */
        public JsonNode search(String query) throws IOException {
            return request(String.format("chat/characters/search/?query=%s", query), "GET", null);
        }
    }

    /**
     * Managing a chat with a character.
     * <p></p>
     * <code>chat.createRoom('CHARACTERS', 'NAME', 'TOPIC')</code><br></br>
     * <code>chat.rate(NUM, 'HISTORY_ID', 'MESSAGE_ID')</code><br></br>
     * <code>chat.nextMessage('CHAR', 'MESSAGE')</code><br></br>
     * <code>chat.getHistories('CHAR')</code><br></br>
     * <code>chat.getHistory('HISTORY_EXTERNAL_ID')</code><br></br>
     * <code>chat.getChat('CHAR')</code><br></br>
     * <code>chat.sendMessage('CHAR', 'MESSAGE')</code><br></br>
     * <code>chat.deleteMessage('HISTORY_ID', 'UUIDS_TO_DELETE')</code><br></br>
     * <code>chat.newChat('CHAR')</code>
    */
    public class Chat {
        /*
         * Creates a Character.AI Room (a chat with multiple characters).
         * Arguments:
         *     characters: String[], list of character IDs to be present in the room
         *     name: String, the name of the room
         *     topic: String, the starting message of the room (leave '' for no topic)
         */
        public JsonNode createRoom(String[] characters, String name, String topic) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .putPOJO("characters", characters)
                    .put("name", name)
                    .put("topic", topic)
                    .put("visibility", "PRIVATE");
            return request("../chat/room/create/", "POST", data);
        }

        /*
         * Rate a message from 1-4, where:
         *     1 = terrible (1 star)
         *     2 = bad (2 stars)
         *     3 = good (3 stars)
         *     4 = fantastic (4 stars)
         * Arguments:
         *     rating: int (1-4), the rating to give the message
         *     historyId: String, the history ID containing the message to rate
         *     messageID: String, the message ID of the message you want to rate
         */
        public JsonNode rate(int rating, String historyId, String messageID) throws IOException {
            int[] label = new int[][] {
                    {234, 238, 241, 244}, // terrible
                    {235, 237, 241, 244}, // bad
                    {235, 238, 240, 244}, // good
                    {235, 238, 241, 243} // fantastic
            }[rating-1]; // select based off of rating

            JsonNode data = new ObjectMapper().createObjectNode()
                    .putPOJO("label_ids", label)
                    .put("history_external_id", historyId)
                    .put("message_uuid", messageID);
            return request("chat/annotations/label/", "PUT", data);
        }

        /*
         * Retry a response.
         * Arguments:
         *     historyId: String, the history ID containing the message
         *     messageID: String, the message ID of the message you want to rate
         *     tgt: String,
         */
        public JsonNode nextMessage(String historyId, String messageID, String tgt) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("history_external_id", historyId)
                    .put("parent_msg_uuid", messageID)
                    .put("tgt", tgt);
            return request("chat/streaming/", "POST-SPLIT", data);
        }

        /*
         * Get history data from a character.
         * Arguments:
         *     characterId: String, the ID of the character in which you want to obtain the histories from
         *     number: int, the amount of histories to retrieve
         */
        public JsonNode getHistories(String characterId, int number) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterId)
                    .put("number", number);
            return request("chat/character/histories_v2/", "POST", data);
        }

        /*
         * Gets messages from a history.
         * Arguments:
         *     historyId: String, the history ID
         */
        public JsonNode getHistory(String historyId) throws IOException {
            return request("chat/history/msgs/user/?history_external_id=" + historyId, "GET", null);
        }

        /*
         * Gets metadata about the last chat you had with a character.
         * Arguments:
         *     characterId: String, the character's ID
         */
        public JsonNode getChat(String characterId) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("character_external_id", characterId);
            return request("chat/history/continue/", "POST", data);
        }

        /*
         * Gets the `tgt` variable, when provided with a character ID.
         * Arguments:
         *     characterId: String, the character's ID
         */
        public String getTgt(String characterId) throws IOException {
            JsonNode chat = getChat(characterId);
            return chat.get("participants").get(0).get("is_human").asBoolean() ?
                   chat.get("participants").get(1).get("user").get("username").asText() :
                   chat.get("participants").get(0).get("user").get("username").asText();
        }

        /*
         * Send a message to a character.
         * Arguments:
         *     historyId: String, the history ID you want to send a message in
         *     text: String, the text you want to send to a character
         *     tgt: String,
         */
        public JsonNode sendMessage(String historyId, String text, String tgt) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("history_external_id", historyId)
                    .put("tgt", tgt)
                    .put("text", text);
            return request("chat/streaming/", "POST-SPLIT", data);
        }

        /*
         * Send a message to a character.
         * Arguments:
         *     historyId: String, the history ID you want to send a message in
         *     messageUuids: String[], the messages to delete
         */
        public JsonNode deleteMessages(String historyId, String[] messageUuids) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("history_id", historyId)
                    .putPOJO("uuids_to_delete", messageUuids);
            return request("chat/history/msgs/delete", "POST", data);
        }

        /*
         * Creates a new chat with a specified character.
         * Arguments:
         *     characterId: String, the character you want to start a new chat with
         */
        public JsonNode newChat(String characterId) throws IOException {
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("character_external_id", characterId);
            return request("chat/history/create/", "POST", data);
        }
    }

    public JsonNode ping() throws IOException {
        return request("ping/", "GET", null);
    }
}
