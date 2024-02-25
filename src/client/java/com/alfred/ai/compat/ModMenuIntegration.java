package com.alfred.ai.compat;

import com.alfred.ai.MCAIConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigScreenProvider<MCAIConfig> provider = (ConfigScreenProvider<MCAIConfig>) AutoConfig.getConfigScreen(MCAIConfig.class, parent);
            provider.setI13nFunction(manager -> "config.mcai");
            provider.setOptionFunction((baseI13n, field) -> String.format("%s.%s", baseI13n, field.getName()));
            provider.setCategoryFunction((baseI13n, categoryName) -> String.format("%s.%s", baseI13n, categoryName));
            return provider.get();
        };
    }
}