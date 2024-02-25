package com.alfred.ai;

import com.google.common.collect.Lists;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

@Config(name = "mcai")
public class MCAIConfig implements ConfigData {
    @ConfigEntry.Gui.TransitiveObject
    @ConfigEntry.Category("General")
    public General general = new General();
    @ConfigEntry.Category("AIs")
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
        public String format = "{user}:{message}";
        public String replyFormat = "<{char}> {message}";
        public String authorization = "";
        public boolean disableRandomResponses = false;
        public boolean disableRandomTalking = false;
        public boolean disableAdvancementResponses = false;
        public boolean disableDeathMessageResponses = false;
        public boolean disableRecipeResponses = true;
        public boolean ignoreOnServer = false;
        public String randomTalkMessage = "Nobody has talked for {time}.";
        public String systemName = "SYSTEM";

        public General() { }
    }

    public static class CharacterTuple implements ConfigData {
        public String name;
        public String id;
        public String historyId;
        public String[] aliases;
        public float advancementResponseChance = 0.5f;
        public Map<String, Float> advancementResponseOverrideChances = new HashMap<>();
        public float deathMessageResponseChance = 0.5f;
        public float randomResponseChance = 0.069f;
        public float randomTalkChance = 0.002f;
        public double minimumSecondsBeforeRandomTalking = 100;
        public double talkIntervalSpecificity = 0.2;
        public boolean disabled = false;

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