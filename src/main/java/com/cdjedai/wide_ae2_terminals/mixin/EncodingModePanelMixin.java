package com.cdjedai.wide_ae2_terminals.mixin;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.me.items.CraftingEncodingPanel;
import appeng.client.gui.me.items.EncodingModePanel;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.me.items.ProcessingEncodingPanel;
import appeng.client.gui.me.items.SmithingTableEncodingPanel;
import appeng.client.gui.me.items.StonecuttingEncodingPanel;
import com.cdjedai.wide_ae2_terminals.client.WideTerminalScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The encoding mode panels draw their background relative to the left edge of the screen (bounds.getX() + 8) instead
 * of their own position: follow the centering of the bottom part.
 */
@Mixin({ CraftingEncodingPanel.class, ProcessingEncodingPanel.class, SmithingTableEncodingPanel.class,
        StonecuttingEncodingPanel.class })
public abstract class EncodingModePanelMixin extends EncodingModePanel {
    private EncodingModePanelMixin(PatternEncodingTermScreen<?> screen, WidgetContainer widgets) {
        super(screen, widgets);
    }

    @ModifyArg(method = "drawBackgroundLayer", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/style/Blitter;dest(II)Lappeng/client/gui/style/Blitter;"), index = 0)
    private int wide$centerBackground(int x) {
        return x + ((WideTerminalScreen) screen).wide$getBottomOffset();
    }
}
