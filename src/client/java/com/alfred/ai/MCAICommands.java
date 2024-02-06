package com.alfred.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.alfred.ai.MCAIMod.*;

public class MCAICommands {
    public static void register(CommandDispatcher dispatcher) {
        if (!onServer) { // server has its own AIs
            dispatcher.register(CommandManager.literal("ai")
                            .then(CommandManager.literal("register") // command to add an AI
                                    .then(CommandManager.argument("character_id", StringArgumentType.word())
                                            .executes(context -> registerCharacter(context.getSource(),
                                                    StringArgumentType.getString(context, "character_id"), ""))
                                            .then(CommandManager.argument("aliases", StringArgumentType.greedyString())
                                                    .executes(context -> registerCharacter(context.getSource(),
                                                            StringArgumentType.getString(context, "character_id"),
                                                            StringArgumentType.getString(context, "aliases"))))))
                            .then(CommandManager.literal("unregister") // command to remove an AI completely
                                    .then(CommandManager.argument("character_id", StringArgumentType.word())
                                            .executes(context -> unregisterCharacter(context.getSource(),
                                                    StringArgumentType.getString(context, "character_id")))))
                            .then(CommandManager.literal("talk") // command to talk to an AI (alternative to pinging)
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                                    .executes(context -> talk(context.getSource(),
                                                            StringArgumentType.getString(context, "name"),
                                                            StringArgumentType.getString(context, "text"))))))
                            .then(CommandManager.literal("context") // retrieves the last few messages
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .executes(context -> getContext(context.getSource(),
                                                    StringArgumentType.getString(context, "name"), false))
                                            .then(CommandManager.argument("broadcast", BoolArgumentType.bool())
                                                    .executes(context -> getContext(context.getSource(),
                                                            StringArgumentType.getString(context, "name"),
                                                            BoolArgumentType.getBool(context, "broadcast"))))))
                            .then(CommandManager.literal("reset") // resets the chat and sends the greeting
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .executes(context -> newChat(context.getSource(),
                                                    StringArgumentType.getString(context, "name")))))
                            .then(CommandManager.literal("enable") // enables the AI (allows it to respond to pings)
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .executes(context -> toggleAI(context.getSource(),
                                                    StringArgumentType.getString(context, "name"), false))))
                            .then(CommandManager.literal("disable") // disables the AI (prevents it from responding to pings)
                                    .then(CommandManager.argument("name", StringArgumentType.word())
                                            .executes(context -> toggleAI(context.getSource(),
                                                    StringArgumentType.getString(context, "name"), true))))
                            .then(CommandManager.literal("authorize") // enables the AI (allows it to respond to pings)
                                    .then(CommandManager.argument("token", StringArgumentType.word())
                                            .executes(context -> authorize(context.getSource(),
                                                    StringArgumentType.getString(context, "token")))))
                            .then(CommandManager.literal("reload") // reloads the config
                                    .executes(context -> {
                                        MCAIConfig.load();
                                        context.getSource().sendFeedback(() -> Text.translatable("mcai.messages.config_reload_success"),true);
                                        return 1;}))
                            .then(CommandManager.literal("list") // lists available AIs
                                    .executes(context -> list(context.getSource()))));
        }
    }

    private static int registerCharacter(ServerCommandSource source, String characterID, String aliases) {
        if (MCAIConfig.getInstance().AIs.stream().anyMatch(tuple -> tuple.ID.equals(characterID))) { // character ID is already present as a character
            source.sendError(Text.translatable("mcai.errors.character_id_exists"));
        } else {
            try {
                String[] aliasList = aliases.strip().equals("") ? new String[] {} : aliases.strip().split("\\s+");
                String name = MCAIMod.characterAI.character.getInfo(characterID).get("character").get("name").asText("Unknown");
                MCAIConfig.getInstance().AIs.add(new MCAIConfig.CharacterTuple(name, characterID, "", aliasList));
                MCAIConfig.save();
                MCAIConfig.load();
                //source.sendMessage(Text.translatable("mcai.messages.character_register_success", name, name));
                source.sendFeedback(() -> Text.translatable("mcai.messages.character_register_success", name, name), true);
                return 1; // Command successful
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return 0; // Command unsuccessful
    }

    private static int unregisterCharacter(ServerCommandSource source, String characterID) {
        for (MCAIConfig.CharacterTuple tuple : MCAIConfig.getInstance().AIs) {
            if (tuple.ID.equals(characterID)) {
                MCAIConfig.getInstance().AIs.remove(tuple);
                MCAIConfig.save();
                //source.sendMessage(Text.translatable("mcai.messages.character_unregister_success"));
                source.sendFeedback(() -> Text.translatable("mcai.messages.character_unregister_success", tuple.name), true);
                return 1;
            }
        }
        source.sendError(Text.translatable("errors.nonexistent_character_id"));
        return 0;
    }

    private static int talk(ServerCommandSource source, String name, String text) {
        boolean foundAnAI = false;
        for (MCAIConfig.CharacterTuple tuple : MCAIConfig.getInstance().AIs) {
            if (tuple.name.equalsIgnoreCase(name) || Arrays.stream(tuple.aliases).anyMatch(alias -> alias.equalsIgnoreCase(name))) {
                foundAnAI = true;
                if (!tuple.disabled) {
                    Runnable task = new AIPlayerListResponse(
                            tuple, text,
                            source.getName(),
                            MCAIConfig.getInstance().General.format,
                            MCAIConfig.getInstance().General.replyFormat,
                            source.getServer().getPlayerManager().getPlayerList());
                    Thread thread = new Thread(null, task, "HTTP thread");
                    thread.start();
                    return 1; // Command successful
                }
            }
        }
        source.sendError(Text.translatable(!foundAnAI ? "mcai.errors.ai_not_found" : "mcai.errors.no_suitable_ai_found", name));
        return 0; // Command did not find an AI matching the input text
    }

    public static String sub(String input, String pattern, Function<String, String> replacementFunction) {
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(input);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group();
            String replacement = replacementFunction.apply(match);
            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private static Tuple<String> reverseFormat(String formatString, String formattedString) {
        String regex = sub(
                formatString, "\\{([^{}]+)\\}",
                (match -> String.format("(?<%s>", match.substring(1, match.length() - 1)) + (match.equals("{user}") ? "[^:]" : ".") + "+)"));
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(formattedString);

        if (matcher.find()) {
            return new Tuple(matcher.group("user"), matcher.group("message"));
        } else {
            return new Tuple("", "");
        }
    }

    private static int getContext(ServerCommandSource source, String name, boolean broadcast) {
        for (MCAIConfig.CharacterTuple tuple : MCAIConfig.getInstance().AIs) {
            if (tuple.name.equalsIgnoreCase(name) || Arrays.stream(tuple.aliases).anyMatch(alias -> alias.equalsIgnoreCase(name))) {
                if (tuple.historyID.strip().equals("")) {
                    source.sendError(Text.translatable("mcai.errors.no_history", tuple.name));
                } else {
                    try {
                        JsonNode chat = characterAI.chat.getHistory(tuple.historyID);
                        for (int i = 0; i < 5; i++) {
                            int messageIndex = chat.get("messages").size() + i - 5;
                            if (messageIndex >= 0) {
                                String text = chat.get("messages").get(messageIndex).get("text").asText();
                                Tuple<String> reversedFormat = reverseFormat(MCAIConfig.getInstance().General.format, text);
                                text = MCAIConfig.getInstance().General.replyFormat
                                        .replace("{char}", chat.get("messages").get(messageIndex).get("src__is_human").asBoolean(false) ?
                                                reversedFormat.get(0) :
                                                chat.get("messages").get(messageIndex).get("src_char").get("participant").get("name").asText())
                                        .replace("{message}", reversedFormat.get(1).equals("") ?
                                                text :
                                                reversedFormat.get(1).charAt(0) == ' ' ?
                                                        reversedFormat.get(1).substring(1) : reversedFormat.get(1));
                                if (broadcast) {
                                    sendGlobalMessage(text, source.getServer());
                                } else {
                                    source.sendMessage(Text.literal(text));
                                }
                            } else {
                                break;
                            }
                        }
                        return 1;
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        source.sendError(Text.translatable("mcai.errors.ai_not_found", name));
        return 0;
    }

    private static int newChat(ServerCommandSource source, String name) {
        boolean foundAnAI = false;
        for (MCAIConfig.CharacterTuple tuple : MCAIConfig.getInstance().AIs) {
            if (tuple.name.equalsIgnoreCase(name) || Arrays.stream(tuple.aliases).anyMatch(alias -> alias.equalsIgnoreCase(name))) {
                foundAnAI = true;
                if (!tuple.disabled) {
                    try {
                        JsonNode chat = characterAI.chat.newChat(tuple.ID);
                        tuple.historyID = chat.get("external_id").asText();
                        MCAIConfig.save();
                        MCAIConfig.load();
                        sendGlobalMessage(chat.get("messages").get(0).get("text").asText(), source.getServer());
                        return 1; // Command successful
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }

            }
        }
        source.sendError(Text.translatable(!foundAnAI ? "mcai.errors.ai_not_found" : "mcai.errors.no_suitable_ai_found", name));
        return 0;
    }

    private static int toggleAI(ServerCommandSource source, String name, boolean disable) {
        boolean foundAnAI = false;
        for (MCAIConfig.CharacterTuple tuple : MCAIConfig.getInstance().AIs) {
            if (tuple.name.equalsIgnoreCase(name) || Arrays.stream(tuple.aliases).anyMatch(alias -> alias.equalsIgnoreCase(name))) {
                foundAnAI = true;
                if (tuple.disabled == !disable) {
                    tuple.disabled = disable;
                    MCAIConfig.save();
                    MCAIConfig.load();
                    //source.sendMessage(Text.translatable("mcai.messages.enabled_ai", tuple.name));
                    source.sendFeedback(() -> Text.translatable(String.format("mcai.messages.%sabled_ai", disable ? "dis" : "en"), tuple.name), true);
                    return 1;
                }
            }
        }
        source.sendError(Text.translatable(!foundAnAI ? "mcai.errors.ai_not_found" : "mcai.errors.no_suitable_ai_found", name));
        return 0;
    }

    private static int authorize(ServerCommandSource source, String token) {
        MCAIConfig.getInstance().General.authorization = token;
        MCAIConfig.save();
        MCAIConfig.load();
        //source.sendMessage(Text.translatable("mcai.messages.authorized", token));
        source.sendFeedback(() -> Text.translatable("mcai.messages.authorized", token), true);
        return 1;
    }

    private static int list(ServerCommandSource source) {
        final String[] text = {""};
        MCAIConfig.getInstance().AIs.forEach(tuple ->
                text[0] += String.format("\n - %s %s",
                        tuple.name,
                        Text.translatable(String.format("mcai.messages.%s", tuple.disabled ?
                                "disabled" : "enabled")).getString()) +
                        (tuple.aliases.length > 0 ? ' ' + Arrays.toString(tuple.aliases) : ""));
        //source.sendMessage(Text.translatable("mcai.messages.ai_list", text[0]));
        source.sendFeedback(() -> Text.translatable("mcai.messages.ai_list", text[0]), false);
        return 1;
    }
}

class AIPlayerListResponse implements Runnable {
    private final MCAIConfig.CharacterTuple tuple;
    public final String text;
    public final String playerName;
    public final String format;
    public final String replyFormat;
    public final List<ServerPlayerEntity> players;

    public AIPlayerListResponse(MCAIConfig.CharacterTuple tuple, String text, String playerName, String format, String replyFormat, List<ServerPlayerEntity> players) {
        this.tuple = tuple;
        this.text = text;
        this.playerName = playerName == null ? "Anonymous" : playerName;
        this.format = format;
        this.replyFormat = replyFormat;
        this.players = players;
    }

    @Override
    public void run() {
        try {
            JsonNode chat = (tuple.historyID == null || tuple.historyID.strip().equals("")) ? characterAI.chat.newChat(tuple.ID) : characterAI.chat.getChat(tuple.ID);
            String historyID = chat.get("external_id").asText();
            if (!tuple.historyID.equals(historyID)) {
                tuple.historyID = historyID;
                MCAIConfig.save();
                MCAIConfig.load();
            }
            String tgt = characterAI.chat.getTgt(tuple.ID);
            JsonNode reply = characterAI.chat.sendMessage(
                    historyID,
                    format
                            .replace("{user}", playerName)
                            .replace("{message}", text),
                    tgt);
            sendMessage(replyFormat
                    .replace("{char}", reply.get("src_char").get("participant").get("name").asText())
                    .replace("{message}", reply.get("replies").get(0).get("text").asText()), players);
        } catch (IOException ignored) { }
    }
}