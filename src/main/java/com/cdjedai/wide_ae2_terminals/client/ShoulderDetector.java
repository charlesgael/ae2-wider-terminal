package com.cdjedai.wide_ae2_terminals.client;

import appeng.client.gui.style.Blitter;
import com.cdjedai.wide_ae2_terminals.WideAE2Terminals;
import com.cdjedai.wide_ae2_terminals.mixin.BlitterAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Finds, in the bottom texture of a terminal, the row where it narrows from the storage width (195px) to the player
 * inventory width (176px). It is row 5 in the AE2 textures, but lower when the bottom part has full-width content
 * (e.g. the ae2wtlib wireless crafting terminal, with its armor slots and crafting grid).
 * <p>
 * The notch is closed by a highlight row, 2 shadow rows and an outline row: the right column of the texture becomes
 * transparent just below it.
 * <p>
 * The result is cached per texture area, and the cache is cleared on resource reload (resource packs may change the
 * textures).
 */
public final class ShoulderDetector {
    public static final int DEFAULT_SHOULDER_ROW = 5;
    private static final int NOTCH_HEIGHT = 4;

    private record Key(ResourceLocation texture, int x, int y, int width, int height) {
    }

    private static final Map<Key, Integer> CACHE = new HashMap<>();

    private ShoulderDetector() {
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static int shoulderRow(Blitter bottom) {
        var accessor = (BlitterAccessor) (Object) bottom;
        var key = new Key(accessor.wide$getTexture(), bottom.getSrcX(), bottom.getSrcY(), bottom.getSrcWidth(),
                bottom.getSrcHeight());
        return CACHE.computeIfAbsent(key, k -> detect(k, accessor));
    }

    private static int detect(Key key, BlitterAccessor accessor) {
        var resourceManager = Minecraft.getInstance().getResourceManager();
        try (var stream = resourceManager.open(key.texture());
                var image = NativeImage.read(stream)) {
            // Textures from resource packs may have a higher resolution than the reference size
            double scaleX = image.getWidth() / (double) accessor.wide$getReferenceWidth();
            double scaleY = image.getHeight() / (double) accessor.wide$getReferenceHeight();
            int px = (int) ((key.x() + key.width() - 0.5) * scaleX);
            for (int row = 0; row < key.height(); row++) {
                int py = (int) ((key.y() + row + 0.5) * scaleY);
                if ((image.getPixelRGBA(px, py) >>> 24) == 0) {
                    int shoulderRow = row - NOTCH_HEIGHT;
                    if (shoulderRow >= 0) {
                        return shoulderRow;
                    }
                    break;
                }
            }
            WideAE2Terminals.LOGGER.warn("No notch found in terminal texture {}, using row {}", key.texture(),
                    DEFAULT_SHOULDER_ROW);
        } catch (Exception e) {
            WideAE2Terminals.LOGGER.warn("Could not read terminal texture {}, using notch row {}", key.texture(),
                    DEFAULT_SHOULDER_ROW, e);
        }
        return DEFAULT_SHOULDER_ROW;
    }
}
