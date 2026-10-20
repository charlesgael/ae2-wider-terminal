package com.cdjedai.wide_ae2_terminals;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@Mod(value = WideAE2Terminals.MODID, dist = Dist.CLIENT)
public class WideAE2Terminals {
    public static final String MODID = "wide_ae2_terminals";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WideAE2Terminals(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
