package com.alfred.ai;

import com.google.common.collect.Lists;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        public General(String format, String replyFormat, String authorization, int adminPermissionLevel) {
            this.format = format;
            this.replyFormat = replyFormat;
            this.authorization = authorization;
            this.adminPermissionLevel = adminPermissionLevel;
        }
    }

    public static class CharacterTuple {
        public String name;
        public String ID;
        public String historyID;
        public String[] aliases;
        public boolean disabled;
        public float advancementChance;
        public Map<String, Float> advancementOverrideChances;
        public float deathChance;
        public float talkIntervalSpecificity;

        public CharacterTuple(String name, String ID, String historyID, String[] aliases) {
            this.name = name;
            this.ID = ID;
            this.historyID = historyID;
            this.aliases = aliases;
            this.disabled = false;
            this.advancementChance = 0.5f; // default chance for advancements
            this.advancementOverrideChances = new HashMap<>();
            this.deathChance = 0.2f; // default chance for death responses
            this.talkIntervalSpecificity = 1.0f; // default specificity for talk interval
        }
    }
}
