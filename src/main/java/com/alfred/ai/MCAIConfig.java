package com.alfred.ai;

import com.google.common.collect.Lists;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Config(name = "mcai")
public class MCAIConfig implements ConfigData {
    @ConfigEntry.Gui.TransitiveObject
    @ConfigEntry.Category("general")
    public General general = new General();
    @ConfigEntry.Category("ais")
    public List<CharacterTuple> ais = Lists.newArrayList();

    public static MCAIConfig getInstance() {
        return AutoConfig.getConfigHolder(MCAIConfig.class).getConfig();
    }
    public static void save() {
        AutoConfig.getConfigHolder(MCAIConfig.class).save();
    }
    public static void load() {
        AutoConfig.getConfigHolder(MCAIConfig.class).load();
    }

    public static class General implements ConfigData {
        @Comment("Authorization key for Character AI. Required for the mod to function.")
        @ConfigEntry.Gui.RequiresRestart
        public String authorization = "";
        @Comment("This field dictates how messages sent to the AI will be formatted.")
        public String format = "{user}:{message}";
        @Comment("This field dictates how messages sent from the AI will be formatted in chat.")
        public String replyFormat = "<{char}> {message}";
        @Comment("The permission level required to use the commands this mod provides.")
        public int adminPermissionLevel = 3;
        @Comment("Prevents users from typing @everyone to mention all the AIs.")
        public boolean disableEveryonePing = true;
        @Comment("Prevents AIs from responding to users gaining an advancement.")
        public boolean disableAdvancementResponses = false;
        @Comment("Prevents AIs from responding to user death messages.")
        public boolean disableDeathMessageResponses = false;
        @Comment("Prevents AIs from randomly responding to chat messages.")
        public boolean disableRandomResponses = false;
        @Comment("Prevents AIs from randomly speaking in chat unprompted.")
        public boolean disableRandomTalking = true;
        @Comment("Prevents AIs from responding to join messages.")
        public boolean disableJoinResponses = false;
        @Comment("Prevents AIs from responding to leave messages.")
        public boolean disableLeaveResponses = false;
        @Comment("Prevents AIs from responding to users learning recipes.")
        public boolean disableRecipeResponses = true;
        @Comment("The message format to use for random talking AIs.")
        public String randomTalkMessage = "Nobody has talked for {time}.";
        @Comment("The message format to use when a player gains an advancement.")
        public String advancementMessage = "{player} has gained the advancement **[{advancement}]**!\n*{advancement_desc}*";
        @Comment("The message format to use for when a player joined the server.")
        public String joinMessage = "{player} joined!";
        @Comment("The message format to use for when a player left the server.")
        public String leaveMessage = "{player} left.";
        @Comment("The system's name.")
        public String systemName = "SYSTEM";

        public General() { }
    }

    public static class CharacterTuple implements ConfigData {
        @Comment("The name of the AI. Used for detecting things such as pings, formatting AI chat messages, etc.")
        public String name;
        @Comment("The AI's ID. Changing this will change the character.")
        public String id;
        @Comment("The chat history ID. Recommended to leave alone unless you know what you're doing.")
        public String historyId;
        @Comment("A list of aliases for the AI that are used for detecting pings.")
        public String[] aliases;
        @Comment("The chance for the AI to respond to an advancement message.")
        public float advancementResponseChance = 0.5f;
        @ConfigEntry.Gui.Excluded // no GUI provider for Map class
        @Comment("An override for the advancementResponseChance that changes the response chance for specific advancements.")
        public Map<String, Float> advancementResponseOverrideChances = new HashMap<>();
        @Comment("The chance for the AI to respond to a death message.")
        public float deathMessageResponseChance = 0.5f;
        @Comment("The chance for the AI to randomly respond to a chat message.")
        public float randomResponseChance = 0.069f;
        @Comment("The chance (every tick) for the AI to randomly say something in chat unprompted.")
        public float randomTalkChance = 0.00004f;
        @Comment("The chance for the AI to respond to a join message.")
        public float joinResponseChance = 0.5f;
        @Comment("The chance for the AI to respond to a leave message.")
        public float leaveResponseChance = 0.5f;
        @Comment("The minimum amount of time (in seconds) before an AI can randomly say something in chat unprompted.")
        public double minimumSecondsBeforeRandomTalking = 100;
        @Comment("How specific the {time} field will be filled out in for the Random Talk Message.")
        public double talkIntervalSpecificity = 0.2;
        @Comment("Prevents the AI from talking at all.")
        public boolean disabled = false;

        // required because Cloth needs an empty constructor for serialization
        public CharacterTuple() {
            this("", "", "", new String[0]);
        }

        public CharacterTuple(String name, String id, String historyId, String[] aliases) {
            this.name = name;
            this.id = id;
            this.historyId = historyId;
            this.aliases = aliases;
        }
    }
}
