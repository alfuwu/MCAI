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

    public JavaCAI(String token) {
        this.token = token;
        this.client = new OkHttpClient();
    }

    public User user = new User();
    public Post post = new Post();
    public Character character = new Character();
    public Chat chat = new Chat();

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
                //System.out.println(text);
                JsonNode jsonNode = objectMapper.readTree(text);
                //System.out.println(jsonNode);
                return jsonNode.get(jsonNode.size() - 1);
            }
            return objectMapper.readTree(Objects.requireNonNull(response.body()).string());
        }
    }

    public class User {
        /*
            A class for obtaining info about users.

            user.getInfo()
            user.getUserProfile('USERNAME')
            user.getFollowers()
            user.getFollowing()
            user.update('USERNAME') // not implementing
        */
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

    public class Post {
        /*
            A class for messing about with posts on the site.

            post.getPost('POST_ID')
            post.getMyPosts()
            post.getPosts('USERNAME')
            post.upvote('POST_ID')
            post.undoUpvote('POST_ID')
            post.sendComment('POST_ID', 'TEXT')
            post.deleteComment('MESSAGE_ID', 'POST_ID')
            post.create('HISTORY_ID', 'TITLE')
            post.delete('POST_ID')
        */

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

    public class Character {
        /*
            A class for managing and obtaining information about characters.

            character.create()
            character.update()
            character.getTrending()
            character.getRecommended()
            character.getCategories()
            character.getInfo('CHAR')
            character.search('QUERY')
            character.getVoices()
        */
        public JsonNode create(
                String greeting, String identifier, String name,
                String shortDescription, String[] categories, String definition,
                boolean copyable, String description, String visibility
        ) throws IOException {
            /*
                Creates a new character.
                Arguments:
                    greeting: String, the initial message the character sends at the start of a chat
                    identifier: String, character `tgt`. the site generates a special value, but you can write anything there
                    name: String, the name of the character
                    avatarRelPath: String, the path to the avatar image on the server. you can create this path with the uploadImage() function
                    baseImgPrompt: String, most likely so you can do a basic setup of prompts for images
                    imgGenEnabled: boolean, enables the character image generation abilities
                    shortDescription: String, a very short description of the character, max 50 characters
                    categories: String[], the categories the AI falls under
                    definition: String, a long description of the character, max 32k characters (cuts off at ~3.2k chars)
                    copyable: boolean, whether the character's details are public
                    description: String, a 500-character-long description of the character
                    visibility: "PUBLIC", "UNLISTED", "PRIVATE", the visibility of the character ("public" means everyone can see the character, "unlisted" means anyone with a link can see the character, and "private" means only the creator can see the character)
            */
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

        public JsonNode update(
                String characterID, String greeting, String name,
                String shortDescription, String[] categories, String definition,
                boolean copyable, String description, String visibility
        ) throws IOException {
            /*
                Updates a character's properties.
                Arguments:
                    characterID: String, the character's ID
                    greeting: String,
                    shortDescription: String, ???
                    categories: String[],
                    definition: String,
                    copyable: boolean,
                    description: String,
                    visibility: "PUBLIC", "UNLISTED", "PRIVATE",
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterID)
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

        public JsonNode getTrending() throws IOException {
            /*
                Gets metadata about trending characters.
            */
            return request("chat/characters/trending", "GET", null);
        }

        public JsonNode getRecommended() throws IOException {
            /*
                Gets metadata about recommended characters.
            */
            return request("chat/characters/recommended", "GET", null);
        }

        public JsonNode getCategories() throws IOException {
            /*
                Retrieves all the categories you can give to a character.
            */
            return request("chat/character/categories", "GET", null);
        }

        public JsonNode deprecatedGetInfo(String characterID) throws IOException {
            /*
                [Deprecated]
                Obtains info about a specific character.
                Arguments:
                    characterID: String, the character's ID
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterID);
            return request("chat/character/", "POST", data);
        }

        public JsonNode getInfo(String characterID) throws IOException {
            /*
                Obtains info about a specific character, even if you do not have the necessary permissions to view that information.
                Arguments:
                    characterID: String, the character's ID
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterID);
            return request("chat/character/info/", "POST", data);
        }

        public JsonNode getVoices() throws IOException {
            /*
                Retrieves all available voices you can give to a character.
            */
            return request("chat/character/voices", "GET", null);
        }

        public JsonNode search(String query) throws IOException {
            /*
                Gets metadata about relevant characters according to a query.
                Arguments:
                    query: String, the search topic
            */
            return request(String.format("chat/characters/search/?query=%s", query), "GET", null);
        }
    }

    public class Chat {
        /*
            Managing a chat with a character.

            chat.createRoom('CHARACTERS', 'NAME', 'TOPIC')
            chat.rate(NUM, 'HISTORY_ID', 'MESSAGE_ID')
            chat.nextMessage('CHAR', 'MESSAGE')
            chat.getHistories('CHAR')
            chat.getHistory('HISTORY_EXTERNAL_ID')
            chat.getChat('CHAR')
            chat.sendMessage('CHAR', 'MESSAGE')
            chat.deleteMessage('HISTORY_ID', 'UUIDS_TO_DELETE')
            chat.newChat('CHAR')
        */

        public JsonNode createRoom(String[] characters, String name, String topic) throws IOException {
            /*
                Creates a Character.AI Room (a chat with multiple characters).
                Arguments:
                    characters: String[], list of character IDs to be present in the room
                    name: String, the name of the room
                    topic: String, the starting message of the room (leave '' for no topic)
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .putPOJO("characters", characters)
                    .put("name", name)
                    .put("topic", topic)
                    .put("visibility", "PRIVATE");
            return request("../chat/room/create/", "POST", data);
        }

        public JsonNode rate(int rating, String historyID, String messageID) throws IOException {
            /*
                Rate a message from 1-4, where:
                    1 = terrible (1 star)
                    2 = bad (2 stars)
                    3 = good (3 stars)
                    4 = fantastic (4 stars)
                Arguments:
                    rating: int (1-4), the rating to give the message
                    historyID: String, the history ID containing the message to rate
                    messageID: String, the message ID of the message you want to rate
            */
            int[] label = new int[][] {
                    {234, 238, 241, 244}, // terrible
                    {235, 237, 241, 244}, // bad
                    {235, 238, 240, 244}, // good
                    {235, 238, 241, 243} // fantastic
            }[rating-1]; // select based off of rating

            JsonNode data = new ObjectMapper().createObjectNode()
                    .putPOJO("label_ids", label)
                    .put("history_external_id", historyID)
                    .put("message_uuid", messageID);
            return request("chat/annotations/label/", "PUT", data);
        }

        public JsonNode nextMessage(String historyID, String messageID, String tgt) throws IOException {
            /*
                Retry a response.
                Arguments:
                    historyID: String, the history ID containing the message
                    messageID: String, the message ID of the message you want to rate
                    tgt: String,
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("history_external_id", historyID)
                    .put("parent_msg_uuid", messageID)
                    .put("tgt", tgt);
            return request("chat/streaming/", "POST-SPLIT", data);
        }

        public JsonNode getHistories(String characterID, int number) throws IOException {
            /*
                Get history data from a character.
                Arguments:
                    characterID: String, the ID of the character in which you want to obtain the histories from
                    number: int, the amount of histories to retrieve
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("external_id", characterID)
                    .put("number", number);
            return request("chat/character/histories_v2/", "POST", data);
        }

        public JsonNode getHistory(String historyID) throws IOException {
            /*
                Gets messages from a history.
                Arguments:
                    historyID: String, the history ID
            */
            return request("chat/history/msgs/user/?history_external_id=" + historyID, "GET", null);
        }

        public JsonNode getChat(String characterID) throws IOException {
            /*
                Gets metadata about the last chat you had with a character.
                Arguments:
                    characterID: String, the character's ID
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("character_external_id", characterID);
            return request("chat/history/continue/", "POST", data);
        }

        public String getTgt(String characterID) throws IOException {
            /*
                Gets the mysterious `tgt` variable, when provided with a character ID.
                Arguments:
                    characterID: String, the character's ID
            */
            JsonNode chat = getChat(characterID);
            return chat.get("participants").get(0).get("is_human").asBoolean() ?
                   chat.get("participants").get(1).get("user").get("username").asText() :
                   chat.get("participants").get(0).get("user").get("username").asText();
        }

        public JsonNode sendMessage(String historyID, String text, String tgt) throws IOException {
            /*
                Send a message to a character.
                Arguments:
                    historyID: String, the history ID you want to send a message in
                    text: String, the text you want to send to a character
                    tgt: String,
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("history_external_id", historyID)
                    .put("tgt", tgt)
                    .put("text", text);
            return request("chat/streaming/", "POST-SPLIT", data);
        }

        public JsonNode deleteMessages(String historyID, String[] messageUUIDs) throws IOException {
            /*
                Send a message to a character.
                Arguments:
                    historyID: String, the history ID you want to send a message in
                    messageUUIDs: String[], the messages to delete
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("history_id", historyID)
                    .putPOJO("uuids_to_delete", messageUUIDs);
            return request("chat/history/msgs/delete", "POST", data);
        }

        public JsonNode newChat(String characterID) throws IOException {
            /*
                Creates a new chat with a specified character.
                Arguments:
                    characterID: String, the character you want to start a new chat with
            */
            JsonNode data = new ObjectMapper().createObjectNode()
                    .put("character_external_id", characterID);
            return request("chat/history/create/", "POST", data);
        }
    }

    public JsonNode ping() throws IOException {
        return request("ping/", "GET", null);
    }

    public static void example() {
        try {
            JavaCAI cai = new JavaCAI("TOKEN");

            // Sample implementation for creating a chat with a character and sending a message
            String characterID = "mB0pRucyl2P2LDomw13HB8Hs3VR0exNFq8Fr4A5uIVQ"; // MiuNull's id
            JsonNode chatCreateResponse = cai.chat.getChat(characterID);//cai.chat.newChat(characterID);
            //System.out.println(chatCreateResponse);
            String historyID = chatCreateResponse.get("external_id").asText();

            // Sending a message to the created chat
            String tgt = cai.chat.getTgt(characterID);
            String messageText = "heyo whats up?";
            JsonNode sendMessageResponse = cai.chat.sendMessage(historyID, messageText, tgt);

            System.out.println(sendMessageResponse);
            System.out.println("Message sent successfully!");
            System.out.printf(
                    "%s: %s\n",
                    sendMessageResponse.get("src_char").get("participant").get("name").asText(),
                    sendMessageResponse.get("replies").get(0).get("text").asText());
            System.out.println(sendMessageResponse.get("replies").get(0).get("text").asText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
