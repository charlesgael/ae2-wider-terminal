package com.cdjedai.wide_ae2_terminals.client;

/**
 * Rows of the ExtendedAE pattern access terminal, in a holder class so the ExtendedAE classes are only looked up when
 * its screen is used (ExtendedAE is optional).
 */
public final class ExPatternAccessRows {
    public static final PatternAccessRows INSTANCE = new PatternAccessRows(
            "com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal$SlotsRow");

    private ExPatternAccessRows() {
    }
}
