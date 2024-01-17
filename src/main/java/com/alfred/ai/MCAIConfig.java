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
import java.time.ZoneOffset;
import java.time.Duration;

@Config(name = "mcai")
public class MCAIConfig implements ConfigData {
    public General General = new General(
            "{user}:{message}",
            "<{char}> {message}",
            "",
            3);
    public List<CharacterTuple> AIs = Lists.newArrayList();

    public static MCAIConfig getInstance() {
        return AutoConfig.getConfigHolder(MCAIConfig.class).getConfig();
    }
    public static void save() {
        AutoConfig.getConfigHolder(MCAIConfig.class).save();
    }
    public static void load() {
        AutoConfig.getConfigHolder(MCAIConfig.class).load();
    }

    public static class General {
        public String format;
        public String replyFormat;
        @ConfigEntry.Category("serverOnly") // im not even sure this does anythin but idfc so im leavin it here
        public String authorization;
        public int adminPermissionLevel;
        public boolean disableRandomResponses;
        public boolean disableRandomTalking;
        public boolean disableAdvancementResponses;
        public boolean disableRecipeResponses;
        public String randomTalkMessage;
        public String systemName;

        public General(String format, String replyFormat, String authorization, int adminPermissionLevel) {
            this.format = format;
            this.replyFormat = replyFormat;
            this.authorization = authorization;
            this.adminPermissionLevel = adminPermissionLevel;
            this.disableRandomResponses = false;
            this.disableRandomTalking = false;
            this.disableAdvancementResponses = false;
            this.disableRecipeResponses = true;
            this.randomTalkMessage = "Nobody has talked for {time}.";
            this.systemName = "SYSTEM";
        }
    }

    public static class CharacterTuple {
        public String name;
        public String ID;
        public String historyID;
        public String[] aliases;
        public float advancementResponseChance;
        public Map<String, Float> advancementResponseOverrideChances;
        public float deathResponseChance;
        public float randomResponseChance;
        public float randomTalkChance;
        public double minimumSecondsBeforeRandomTalking;
        public double talkIntervalSpecificity;
        private String lastCommunicatedWith;
        public boolean disabled;

        public ZonedDateTime getLastCommunicatedWith() {
            if (lastCommunicatedWith == null)
                return null;
            else
                return ZonedDateTime.parse(lastCommunicatedWith);
        }

        public double getLastCommunicatedWith(ZonedDateTime time) {
            if (lastCommunicatedWith == null)
                return -1; // can't return null because AAAAAAAAA
            else
                return Duration.between(getLastCommunicatedWith().toInstant(), time.toInstant()).toMillis() / 1000.0d;
        }

        public void setLastCommunicatedWith() {
            setLastCommunicatedWith(ZonedDateTime.now());
        }

        public void setLastCommunicatedWith(ZonedDateTime time) {
            lastCommunicatedWith = time.format(DateTimeFormatter.ISO_DATE_TIME);
        }

        public CharacterTuple(String name, String ID, String historyID, String[] aliases) {
            this.name = name;
            this.ID = ID;
            this.historyID = historyID;
            this.aliases = aliases;
            this.advancementResponseChance = 0.5f; // default chance to respond to advancements
            this.advancementResponseOverrideChances = new HashMap<>();
            this.deathResponseChance = 0.5f; // default chance to respond to deaths
            this.randomResponseChance = 0.069f; // default chance to respond to random chat messages
            this.randomTalkChance = 0.002f; // per tick
            this.minimumSecondsBeforeRandomTalking = 100;
            this.talkIntervalSpecificity = 0.2; // default specificity for talk interval (how specific the data is)
            this.lastCommunicatedWith = null;
            this.disabled = false;
        }
    }
}
