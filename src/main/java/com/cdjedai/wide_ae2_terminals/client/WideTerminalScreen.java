package com.cdjedai.wide_ae2_terminals.client;

/**
 * Implemented by the AE2 terminal screens (via mixin) to expose their widened layout.
 */
public interface WideTerminalScreen {
    /** Horizontal offset of the centered bottom part (player inventory, encoding panels...), 0 if not widened. */
    int wide$getBottomOffset();
}
