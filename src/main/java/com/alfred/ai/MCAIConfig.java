package com.alfred.ai;

import com.google.common.collect.Lists;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
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
        public boolean disableAdvancementResponses;
        public boolean disableRecipeResponses;

        public General(String format, String replyFormat, String authorization, int adminPermissionLevel) {
            this.format = format;
            this.replyFormat = replyFormat;
            this.authorization = authorization;
            this.adminPermissionLevel = adminPermissionLevel;
            this.disableRandomResponses = false;
            this.disableAdvancementResponses = false;
            this.disableRecipeResponses = true;
        }
    }

    public static class CharacterTuple {
        public String name;
        public String ID;
        public String historyID;
        public String[] aliases;
        public float advancementChance;
        public Map<String, Float> advancementOverrideChances;
        public float deathChance;
        public int minimumTicks;
        public float talkIntervalSpecificity;
        private String lastCommunicatedWith;
        public boolean disabled;

        public LocalDateTime getLastCommunicatedWith() {
            if (lastCommunicatedWith == null)
                return null;
            else
                return LocalDateTime.parse(lastCommunicatedWith, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public Object getLastCommunicatedWith(LocalDateTime time) {
            if (lastCommunicatedWith == null)
                return null;
            else
                return Duration.between(getLastCommunicatedWith().toInstant(ZoneOffset.UTC), time.toInstant(ZoneOffset.UTC)).toMillis() / 1000.0d;
        }

        public void setLastCommunicatedWith() {
            lastCommunicatedWith = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public void setLastCommunicatedWith(LocalDateTime time) {
            lastCommunicatedWith = time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public CharacterTuple(String name, String ID, String historyID, String[] aliases) {
            this.name = name;
            this.ID = ID;
            this.historyID = historyID;
            this.aliases = aliases;
            this.advancementChance = 0.5f; // default chance to respond to advancements
            this.advancementOverrideChances = new HashMap<>();
            this.deathChance = 0.5f; // default chance to respond to deaths
            this.talkIntervalSpecificity = 0.2f; // default specificity for talk interval (how specific the data is)
            this.lastCommunicatedWith = null;
            this.disabled = false;
        }
    }
}
