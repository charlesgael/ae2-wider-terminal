package com.cdjedai.wide_ae2_terminals;

import com.cdjedai.wide_ae2_terminals.client.ShoulderDetector;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@Mod(value = WideAE2Terminals.MODID, dist = Dist.CLIENT)
public class WideAE2Terminals {
    public static final String MODID = "wide_ae2_terminals";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WideAE2Terminals(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // Resource packs may change the terminal textures
        modEventBus.addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(
                (ResourceManagerReloadListener) resourceManager -> ShoulderDetector.clearCache()));
    }
}
