package com.alfred.ai;

import com.google.common.collect.Lists;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.List;

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

        public CharacterTuple(String name, String ID, String historyID, String[] aliases) {
            this.name = name;
            this.ID = ID;
            this.historyID = historyID;
            this.aliases = aliases;
            this.disabled = false;
        }
    }
}
