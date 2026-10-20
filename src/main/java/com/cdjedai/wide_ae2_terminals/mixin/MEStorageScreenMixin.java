package com.cdjedai.wide_ae2_terminals.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.menu.me.common.MEStorageMenu;
import com.cdjedai.wide_ae2_terminals.client.WideLayout;
import com.cdjedai.wide_ae2_terminals.client.SupportedScreens;
import com.cdjedai.wide_ae2_terminals.client.WideTerminalScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEStorageScreen.class)
public abstract class MEStorageScreenMixin<C extends MEStorageMenu> extends AEBaseScreen<C>
        implements WideTerminalScreen {
    @Shadow
    @Final
    protected Repo repo;

    @Shadow
    @Final
    private AETextField searchField;

    @Shadow
    private int rows;

    @Unique
    private int wide$columns = WideLayout.BASE_COLUMNS;

    private MEStorageScreenMixin(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    /**
     * init() is called again at every window resize: recompute the column count and move the elements before AE2
     * creates the slots and positions the widgets.
     * <p>
     * {@code style} is the ScreenStyle of AEBaseScreen: MEStorageScreen also has a private {@code style} field (its
     * TerminalStyle), but fields are resolved by name and type.
     */
    @Inject(method = "init", at = @At("HEAD"))
    private void wide$layout(CallbackInfo ci) {
        var terminalStyle = style.getTerminalStyle();
        wide$columns = SupportedScreens.isStorageScreen(this)
                ? WideLayout.computeColumns(width, terminalStyle.getScreenWidth())
                : WideLayout.BASE_COLUMNS;
        int extraColumns = wide$columns - WideLayout.BASE_COLUMNS;

        // Also for the screens that are not widened: the style is shared with the other screens of the same JSON
        WideLayout.layoutStyle(style, extraColumns, true);
        WideLayout.resizeSearchField(style, searchField);
        if (extraColumns > 0) {
            repo.setRowSize(wide$columns);
            imageWidth = terminalStyle.getScreenWidth() + extraColumns * WideLayout.SLOT_SIZE;
        }
    }

    @Override
    public int wide$getBottomOffset() {
        return (wide$columns - WideLayout.BASE_COLUMNS) * WideLayout.SLOT_SIZE / 2;
    }

    /**
     * Replaces the drawing when widened (other injections into this method are then skipped as well).
     */
    @Inject(method = "drawBG", at = @At("HEAD"), cancellable = true)
    private void wide$drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            float partialTicks, CallbackInfo ci) {
        if (wide$columns == WideLayout.BASE_COLUMNS) {
            return;
        }

        WideLayout.drawBackground(guiGraphics, style.getTerminalStyle(), offsetX, offsetY, rows, wide$columns,
                repo.hasPinnedRow());
        if (searchField != null) {
            searchField.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        ci.cancel();
    }
}
