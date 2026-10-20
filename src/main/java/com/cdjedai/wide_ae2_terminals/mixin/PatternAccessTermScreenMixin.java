package com.cdjedai.wide_ae2_terminals.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.implementations.PatternAccessTermMenu;
import com.cdjedai.wide_ae2_terminals.client.PatternAccessRows;
import com.cdjedai.wide_ae2_terminals.client.SupportedScreens;
import com.cdjedai.wide_ae2_terminals.client.WideLayout;
import com.cdjedai.wide_ae2_terminals.client.WideLayout.PatternAccessTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/**
 * The pattern access terminal does not extend MEStorageScreen: its 9 columns and 195px width are hardcoded.
 */
@Mixin(PatternAccessTermScreen.class)
public abstract class PatternAccessTermScreenMixin<C extends PatternAccessTermMenu> extends AEBaseScreen<C> {
    @Shadow
    @Final
    private ArrayList<Object> rows;

    @Shadow
    @Final
    private Scrollbar scrollbar;

    @Shadow
    private int visibleRows;

    @Shadow
    @Final
    private AETextField searchField;

    @Unique
    private int wide$columns = WideLayout.BASE_COLUMNS;

    private PatternAccessTermScreenMixin(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Shadow
    private void refreshList() {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void wide$layout(CallbackInfo ci) {
        wide$columns = SupportedScreens.isPatternAccessScreen(this)
                ? WideLayout.computeColumns(width, PatternAccessTexture.WIDTH)
                : WideLayout.BASE_COLUMNS;
        int extraColumns = wide$columns - WideLayout.BASE_COLUMNS;

        // Also for the screens that are not widened: the style is shared with the other screens of the same JSON
        WideLayout.layoutStyle(style, extraColumns, true);
        WideLayout.resizeSearchField(style, searchField);
        if (extraColumns > 0) {
            imageWidth = PatternAccessTexture.WIDTH + extraColumns * WideLayout.SLOT_SIZE;
        }
    }

    /** The rows depend on the column count: rebuild them, since init() is called again on resize. */
    @Inject(method = "init", at = @At("TAIL"))
    private void wide$refreshRows(CallbackInfo ci) {
        if (wide$columns != WideLayout.BASE_COLUMNS) {
            refreshList();
        }
    }

    /** AE2 wraps the slots of each pattern container every 9 slots: re-wrap them to the column count. */
    @Inject(method = "refreshList", at = @At(value = "INVOKE",
            target = "Lappeng/client/gui/me/patternaccess/PatternAccessTermScreen;resetScrollbar()V"))
    private void wide$rewrapRows(CallbackInfo ci) {
        if (wide$columns != WideLayout.BASE_COLUMNS) {
            var rewrapped = PatternAccessRows.AE2.rewrap(rows, wide$columns);
            rows.clear();
            rows.addAll(rewrapped.rows());
        }
    }

    /** Width of the scroll area (COLUMNS * 18), for the disconnected overlay and the hovered row. */
    @ModifyConstant(method = { "renderLinkStatus", "getHoveredLineIndex" }, constant = @Constant(intValue = 162))
    private int wide$gridWidth(int width) {
        return width + (wide$columns - WideLayout.BASE_COLUMNS) * WideLayout.SLOT_SIZE;
    }

    /** Max width of a group name (TEXT_MAX_WIDTH - 10). */
    @ModifyConstant(method = "drawFG", constant = @Constant(intValue = 145))
    private int wide$textWidth(int width) {
        return width + (wide$columns - WideLayout.BASE_COLUMNS) * WideLayout.SLOT_SIZE;
    }

    /**
     * Same drawing as PatternAccessTermScreen.drawBG, with the rows widened. Replaces it when widened (other
     * injections into this method are then skipped as well).
     */
    @Inject(method = "drawBG", at = @At("HEAD"), cancellable = true)
    private void wide$drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            float partialTicks, CallbackInfo ci) {
        if (wide$columns == WideLayout.BASE_COLUMNS) {
            return;
        }

        WideLayout.drawPatternAccessBackground(guiGraphics, PatternAccessTexture.AE2, PatternAccessRows.AE2, rows,
                scrollbar.getCurrentScroll(), visibleRows, offsetX, offsetY, wide$columns);
        ci.cancel();
    }
}
