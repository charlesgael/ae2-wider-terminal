package com.cdjedai.wide_ae2_terminals.client;

import java.util.Set;

/**
 * Screens whose layout has been checked. Subclasses from other addons may draw differently, so they are only widened
 * once listed here (by name: the addons are not compile-time dependencies).
 */
public final class SupportedScreens {
    private static final Set<String> STORAGE_SCREENS = Set.of(
            "appeng.client.gui.me.common.MEStorageScreen",
            "appeng.client.gui.me.items.CraftingTermScreen",
            "appeng.client.gui.me.items.PatternEncodingTermScreen",
            // ae2wtlib: wireless crafting and pattern encoding terminals
            "de.mari_023.ae2wtlib.wct.WCTScreen",
            "de.mari_023.ae2wtlib.wet.WETScreen");

    private static final Set<String> PATTERN_ACCESS_SCREENS = Set.of(
            "appeng.client.gui.me.patternaccess.PatternAccessTermScreen",
            // ae2wtlib: wireless pattern access terminal
            "de.mari_023.ae2wtlib.wat.WATScreen");

    private static final Set<String> EX_PATTERN_ACCESS_SCREENS = Set.of(
            // ExtendedAE: extended pattern access terminal, wired and wireless
            "com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal",
            "com.glodblock.github.extendedae.xmod.wt.GuiWirelessExPAT");

    private SupportedScreens() {
    }

    public static boolean isStorageScreen(Object screen) {
        return STORAGE_SCREENS.contains(screen.getClass().getName());
    }

    public static boolean isPatternAccessScreen(Object screen) {
        return PATTERN_ACCESS_SCREENS.contains(screen.getClass().getName());
    }

    public static boolean isExPatternAccessScreen(Object screen) {
        return EX_PATTERN_ACCESS_SCREENS.contains(screen.getClass().getName());
    }
}
