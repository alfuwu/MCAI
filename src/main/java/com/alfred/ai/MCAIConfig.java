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
        public String format = "{user}:{message}";
        public String replyFormat = "<{char}> {message}";
        public String authorization = "";
        public int adminPermissionLevel = 3;
        public boolean disableEveryonePing = true;
        public boolean disableAdvancementResponses = false;
        public boolean disableDeathMessageResponses = false;
        public boolean disableRandomResponses = false;
        public boolean disableRandomTalking = false;
        public boolean disableJoinResponses = false;
        public boolean disableLeaveResponses = false;
        public boolean disableRecipeResponses = true;
        public String randomTalkMessage = "Nobody has talked for {time}.";
        public String advancementMessage = "{player} has gained the advancement **[{advancement}]**!\n*{advancement_desc}*";
        public String joinMessage = "{player} joined!";
        public String leaveMessage = "{player} left.";
        public String systemName = "SYSTEM";

        public General() { }
    }

    public static class CharacterTuple implements ConfigData {
        public String name;
        public String id;
        public String historyId;
        public String[] aliases;
        public float advancementResponseChance = 0.5f;
        @ConfigEntry.Gui.Excluded // no GUI provider for Map class
        public Map<String, Float> advancementResponseOverrideChances = new HashMap<>();
        public float deathMessageResponseChance = 0.5f;
        public float randomResponseChance = 0.069f;
        public float randomTalkChance = 0.00004f;
        public float joinResponseChance = 0.5f;
        public float leaveResponseChance = 0.5f;
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
