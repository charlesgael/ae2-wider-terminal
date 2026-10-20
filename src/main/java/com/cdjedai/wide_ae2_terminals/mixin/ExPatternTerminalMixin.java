package com.cdjedai.wide_ae2_terminals.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.AEBaseMenu;
import com.cdjedai.wide_ae2_terminals.client.ExPatternAccessRows;
import com.cdjedai.wide_ae2_terminals.client.SupportedScreens;
import com.cdjedai.wide_ae2_terminals.client.WideLayout;
import com.cdjedai.wide_ae2_terminals.client.WideLayout.PatternAccessTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * ExtendedAE's pattern access terminal: a copy of AE2's one (9 columns and 195px hardcoded), with search by
 * ingredients, pattern highlighting and a button per provider to highlight it in the world.
 * <p>
 * ExtendedAE is optional ({@link Pseudo}), and {@code WideMixinPlugin} only applies this mixin when the ExtendedAE
 * class still has everything it targets.
 */
@Pseudo
@Mixin(targets = "com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal")
public abstract class ExPatternTerminalMixin extends AEBaseScreen<AEBaseMenu> {
    @Shadow
    @Final
    private ArrayList<Object> rows;

    /** Highlight buttons, by index of the first row of their pattern provider. */
    @Shadow
    @Final
    private HashMap<Integer, Object> highlightBtns;

    @Shadow
    @Final
    private Scrollbar scrollbar;

    @Shadow
    protected int visibleRows;

    @Unique
    private int wide$columns = WideLayout.BASE_COLUMNS;

    private ExPatternTerminalMixin(AEBaseMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Shadow
    private void refreshList() {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void wide$layout(CallbackInfo ci) {
        wide$columns = SupportedScreens.isExPatternAccessScreen(this)
                ? WideLayout.computeColumns(width, PatternAccessTexture.WIDTH)
                : WideLayout.BASE_COLUMNS;
        int extraColumns = wide$columns - WideLayout.BASE_COLUMNS;

        // The search box background is part of the header texture: keep the field size. Also for the screens that
        // are not widened: the style is shared with the other screens of the same JSON
        WideLayout.layoutStyle(style, extraColumns, false);
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

    /**
     * Slots of each pattern container are wrapped every 9 slots: re-wrap them to the column count, and move the
     * highlight buttons to the new index of the first row of their provider.
     */
    @Inject(method = "refreshList", at = @At(value = "INVOKE",
            target = "Lcom/glodblock/github/extendedae/client/gui/GuiExPatternTerminal;resetScrollbar()V"))
    private void wide$rewrapRows(CallbackInfo ci) {
        if (wide$columns == WideLayout.BASE_COLUMNS) {
            return;
        }

        var rewrapped = ExPatternAccessRows.INSTANCE.rewrap(rows, wide$columns);
        rows.clear();
        rows.addAll(rewrapped.rows());

        var buttons = new HashMap<>(highlightBtns);
        highlightBtns.clear();
        buttons.forEach((index, button) -> highlightBtns.put(
                rewrapped.firstRowIndices().getOrDefault(index, index), button));
    }

    /**
     * Width of the scroll area (COLUMNS * 18): highlight buttons position, disconnected overlay and hovered row.
     */
    @ModifyConstant(method = { "drawFG", "renderLinkStatus", "getHoveredLineIndex" },
            constant = @Constant(intValue = 162))
    private int wide$gridWidth(int width) {
        return width + (wide$columns - WideLayout.BASE_COLUMNS) * WideLayout.SLOT_SIZE;
    }

    /** Max width of a group name (TEXT_MAX_WIDTH - 10). */
    @ModifyConstant(method = "drawFG", constant = @Constant(intValue = 145))
    private int wide$textWidth(int width) {
        return width + (wide$columns - WideLayout.BASE_COLUMNS) * WideLayout.SLOT_SIZE;
    }

    /**
     * Same drawing as GuiExPatternTerminal.drawBG, with the rows widened. Replaces it when widened (other injections
     * into this method are then skipped as well).
     */
    @Inject(method = "drawBG", at = @At("HEAD"), cancellable = true)
    private void wide$drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            float partialTicks, CallbackInfo ci) {
        if (wide$columns == WideLayout.BASE_COLUMNS) {
            return;
        }

        WideLayout.drawPatternAccessBackground(guiGraphics, PatternAccessTexture.EXTENDED_AE,
                ExPatternAccessRows.INSTANCE, rows, scrollbar.getCurrentScroll(), visibleRows, offsetX, offsetY,
                wide$columns);
        ci.cancel();
    }
}
