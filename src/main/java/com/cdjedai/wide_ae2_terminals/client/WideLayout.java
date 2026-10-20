package com.cdjedai.wide_ae2_terminals.client;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.Position;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.TerminalStyle;
import appeng.client.gui.widgets.AETextField;
import com.cdjedai.wide_ae2_terminals.Config;
import com.cdjedai.wide_ae2_terminals.mixin.ScreenStyleAccessor;
import net.minecraft.client.gui.GuiGraphics;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Layout of a widened AE2 terminal: the storage area gets extra 18px columns, while the bottom part (player
 * inventory) keeps its size and is centered below it ("T" shape).
 * <p>
 * The textures are widened by inserting copies of the 8th slot column before the 9th one: the first and last slot
 * columns hold the borders of the grid, and the left part of a header may hold other content.
 */
public final class WideLayout {
    public static final int BASE_COLUMNS = 9;
    public static final int SLOT_SIZE = 18;

    /** Where the slot columns start in the AE2 terminal textures. */
    private static final int GRID_LEFT = 7;
    /** End of the 9 slot columns in the terminal texture: the scrollbar part starts here. */
    private static final int GRID_RIGHT = GRID_LEFT + BASE_COLUMNS * SLOT_SIZE;
    /** Width of the player inventory part of the bottom texture. */
    private static final int BOTTOM_WIDTH = 176;
    /** Rows of the bottom edge of the terminal (and of the notch): highlight, 2 rows of shadow, outline. */
    private static final int EDGE_ROWS = 4;
    /** The inventory title is drawn just above the inventory part (2px above it in the ae2wtlib crafting terminal). */
    private static final int INVENTORY_TITLE_MARGIN = 4;
    /** The search field background (AE2 text_field.png) is at most 128px wide. */
    private static final int MAX_SEARCH_WIDTH = 128;

    private WideLayout() {
    }

    public static int computeColumns(int screenWidth, int baseWidth) {
        int columns = Config.COLUMN_COUNT.get();
        if (columns == 0) {
            int available = screenWidth - 2 * Config.SIDE_MARGIN.get();
            columns = BASE_COLUMNS + Math.max(0, (available - baseWidth) / SLOT_SIZE);
            int max = Config.MAX_COLUMNS.get();
            if (max > 0) {
                columns = Math.min(columns, max);
            }
        }
        return Math.max(BASE_COLUMNS, columns);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Layout of the style elements
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Values from the screen JSON, before any change. AE2 caches one ScreenStyle per JSON file and shares it between
     * all the screens opened from it, so they are recorded once per style object (until the next resource reload,
     * which creates new style objects).
     */
    private record Original(Map<Position, Integer[]> positions, int searchWidth, int slotsPerRow) {
    }

    private static final Map<ScreenStyle, Original> ORIGINALS = new WeakHashMap<>();

    /**
     * Moves the elements of the (shared) style for the given number of extra columns, starting from the JSON values.
     * Called with 0 for the screens that are not widened, so they do not inherit the changes made for another screen
     * opened from the same JSON.
     *
     * @param widenSearch false when the search box background is drawn in the screen texture (ExtendedAE pattern
     *                    access terminal), so the field must keep its size
     */
    public static void layoutStyle(ScreenStyle style, int extraColumns, boolean widenSearch) {
        var terminalStyle = style.getTerminalStyle();
        var search = style.getWidget("search");
        Original original = ORIGINALS.computeIfAbsent(style, s -> new Original(snapshotPositions(s),
                search.getWidth(), terminalStyle != null ? terminalStyle.getSlotsPerRow() : BASE_COLUMNS));

        // Inventory part of a bottom block with full-width content: centered inside that block (only when widened,
        // otherwise AE2 draws its original texture)
        int inventoryShift = 0;
        int blockExtraWidth = 0;
        int inventoryHeight = 0;
        if (extraColumns > 0 && terminalStyle != null) {
            var bottom = terminalStyle.getBottom();
            int shoulderRow = ShoulderDetector.shoulderRow(bottom);
            if (hasFullWidthContent(shoulderRow)) {
                blockExtraWidth = bottom.getSrcWidth() - BOTTOM_WIDTH;
                inventoryShift = blockExtraWidth / 2;
                inventoryHeight = bottom.getSrcHeight() - shoulderRow;
            }
        }

        applyOffsets(original.positions(), extraColumns, inventoryShift, blockExtraWidth, inventoryHeight);
        if (terminalStyle != null) {
            terminalStyle.setSlotsPerRow(original.slotsPerRow() + extraColumns);
        }
        // The search field keeps its right end (next to the scrollbar / crafting status tab) and grows to the left,
        // up to the width of its background texture
        int searchWidth = original.searchWidth();
        Integer searchLeft = original.positions().get(search)[0];
        if (widenSearch && extraColumns > 0 && searchLeft != null) {
            int widened = Math.max(searchWidth, Math.min(searchWidth + extraColumns * SLOT_SIZE, MAX_SEARCH_WIDTH));
            search.setLeft(searchLeft + searchWidth + extraColumns * SLOT_SIZE - widened);
            searchWidth = widened;
        }
        search.setWidth(searchWidth);
    }

    /**
     * AE2 only sizes the search field once, when the screen is constructed (WidgetContainer.add), from the style
     * at that time: apply the width computed by {@link #layoutStyle} (init() only moves the widgets).
     */
    public static void resizeSearchField(ScreenStyle style, AETextField searchField) {
        var search = style.getWidget("search");
        searchField.resize(search.getWidth(), search.getHeight());
    }

    private static Map<Position, Integer[]> snapshotPositions(ScreenStyle style) {
        Map<Position, Integer[]> result = new IdentityHashMap<>();
        style.getSlots().values().forEach(pos -> snapshot(result, pos));
        style.getText().values().forEach(text -> snapshot(result, text.getPosition()));
        style.getTooltips().values().forEach(pos -> snapshot(result, pos));
        ((ScreenStyleAccessor) style).wide$getWidgets().values().forEach(pos -> snapshot(result, pos));
        return result;
    }

    private static void snapshot(Map<Position, Integer[]> result, Position pos) {
        if (pos != null) {
            result.put(pos, new Integer[] { pos.getLeft(), pos.getTop(), pos.getRight(), pos.getBottom() });
        }
    }

    /**
     * Moves the elements for the given number of extra columns:
     * <ul>
     * <li>elements anchored to the bottom belong to the bottom part, which is centered. Those of the inventory part
     * (at most {@code inventoryHeight} from the bottom, with a small margin for its title just above) are also moved
     * inside a bottom block with full-width content: by {@code inventoryShift} when anchored to the left (inventory
     * centered in the block), by {@code blockExtraWidth} when anchored to the right (side panels such as the toolbox,
     * against the right edge of the block);</li>
     * <li>elements anchored to the top, left of the grid end (scrollbar, crafting status tab), follow the right
     * edge;</li>
     * <li>elements anchored to the right already follow the right edge.</li>
     * </ul>
     */
    private static void applyOffsets(Map<Position, Integer[]> original, int extraColumns, int inventoryShift,
            int blockExtraWidth, int inventoryHeight) {
        int extraWidth = extraColumns * SLOT_SIZE;
        int halfExtraWidth = extraWidth / 2;
        original.forEach((pos, o) -> {
            Integer left = o[0], right = o[2], bottom = o[3];
            pos.setLeft(left);
            pos.setRight(right);
            if (bottom != null) {
                boolean inInventoryPart = bottom <= inventoryHeight + INVENTORY_TITLE_MARGIN;
                if (left != null) {
                    pos.setLeft(left + halfExtraWidth + (inInventoryPart ? inventoryShift : 0));
                } else if (right != null) {
                    int shift = halfExtraWidth + (inInventoryPart ? blockExtraWidth : 0);
                    pos.setRight(right + extraWidth - shift);
                }
            } else if (left != null && left >= GRID_RIGHT) {
                pos.setLeft(left + extraWidth);
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Storage terminals (MEStorageScreen and subclasses)
    // ---------------------------------------------------------------------------------------------------------------

    /** Where the extra columns are inserted in the AE2 terminal textures: before the 9th slot column. */
    private static final int TERMINAL_INSERT_AT = GRID_LEFT + (BASE_COLUMNS - 1) * SLOT_SIZE;

    /**
     * Same drawing as MEStorageScreen.drawBG, with the rows widened.
     */
    public static void drawBackground(GuiGraphics g, TerminalStyle style, int x, int y, int rows, int columns,
            boolean pinnedRow) {
        Blitter header = style.getHeader();
        drawWideRow(g, header, x, y, columns, TERMINAL_INSERT_AT);
        int cy = y + header.getSrcHeight();

        // To draw the first/last row, we need to at least draw 2
        int rowsToDraw = Math.max(2, rows);
        for (int i = 0; i < rowsToDraw; i++) {
            Blitter row = style.getRow();
            if (i == 0) {
                row = style.getFirstRow();
            } else if (i + 1 == rowsToDraw) {
                row = style.getLastRow();
            }
            drawWideRow(g, row, x, cy, columns, TERMINAL_INSERT_AT);
            cy += style.getRow().getSrcHeight();
        }

        drawBottom(g, style.getBottom(), x, cy, columns);

        if (pinnedRow) {
            // Overlay of the 9 slots of the pinned row, with the same borders as the grid
            var overlay = Blitter.texture("guis/terminal.png").src(0, 204, BASE_COLUMNS * SLOT_SIZE, SLOT_SIZE);
            drawWideRow(g, overlay, x + GRID_LEFT, y + header.getSrcHeight(), columns, TERMINAL_INSERT_AT - GRID_LEFT);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Pattern access terminals (AE2 and ExtendedAE)
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Texture layout of a pattern access terminal. Text rows (provider names) and slot rows each have a top, middle
     * and bottom version; a slot row is drawn over a text row, only as wide as its slots.
     */
    public record PatternAccessTexture(String texture, int headerHeight, int textTopY, int textMiddleY,
            int textBottomY, int slotsTopY, int slotsMiddleY, int slotsBottomY, int footerY, int footerHeight) {
        public static final int WIDTH = 195;
        /** Where the slot columns start: one pixel further than in the other terminal textures. */
        private static final int GRID_LEFT = 8;
        /**
         * Before the last slot column, which also holds the right border of the scroll box (x=168). The left part of
         * the header may hold the search box (ExtendedAE).
         */
        private static final int INSERT_AT = GRID_LEFT + (BASE_COLUMNS - 1) * SLOT_SIZE;

        public static final PatternAccessTexture AE2 = new PatternAccessTexture("guis/patternaccessterminal.png",
                17, 17, 53, 89, 35, 71, 107, 125, 99);
        public static final PatternAccessTexture EXTENDED_AE = new PatternAccessTexture(
                "guis/ex_pattern_access_terminal.png", 30, 30, 66, 102, 48, 84, 120, 138, 99);
    }

    /**
     * Same drawing as PatternAccessTermScreen.drawBG (and its ExtendedAE copy), with the rows widened.
     */
    public static void drawPatternAccessBackground(GuiGraphics g, PatternAccessTexture layout, PatternAccessRows access,
            List<?> rows, int scrollLevel, int visibleRows, int x, int y, int columns) {
        var texture = Blitter.texture(layout.texture());
        int width = PatternAccessTexture.WIDTH;
        drawWideRow(g, texture.copy().src(0, 0, width, layout.headerHeight()), x, y, columns,
                PatternAccessTexture.INSERT_AT);
        int currentY = y + layout.headerHeight();
        drawBottom(g, texture.copy().src(0, layout.footerY(), width, layout.footerHeight()), x,
                currentY + visibleRows * SLOT_SIZE, columns);

        for (int i = 0; i < visibleRows; i++) {
            boolean firstLine = i == 0;
            boolean lastLine = i == visibleRows - 1;

            int textY = firstLine ? layout.textTopY() : lastLine ? layout.textBottomY() : layout.textMiddleY();
            drawWideRow(g, texture.copy().src(0, textY, width, SLOT_SIZE), x, currentY, columns,
                    PatternAccessTexture.INSERT_AT);

            if (scrollLevel + i < rows.size()) {
                var row = rows.get(scrollLevel + i);
                if (access.isSlotsRow(row)) {
                    int slotsY = firstLine ? layout.slotsTopY() : lastLine ? layout.slotsBottomY()
                            : layout.slotsMiddleY();
                    drawSlotsRow(g, texture.copy().src(0, slotsY, width, SLOT_SIZE), x, currentY, access.slots(row),
                            columns);
                }
            }
            currentY += SLOT_SIZE;
        }
    }

    /**
     * Draws the slots of a pattern access row, cut like AE2 does (at gridLeft - 1 + 18 * slots). A row filling all
     * the columns ends with the last column of the texture, which holds the right border of the scroll box.
     */
    private static void drawSlotsRow(GuiGraphics g, Blitter b, int x, int y, int slots, int columns) {
        int gridLeft = PatternAccessTexture.GRID_LEFT;
        int rowEnd = gridLeft - 1 + slots * SLOT_SIZE;
        if (slots == columns) {
            drawInserted(g, b, x, y, columns - BASE_COLUMNS, PatternAccessTexture.INSERT_AT, rowEnd);
            return;
        }

        blitPart(g, b, 0, 0, gridLeft, b.getSrcHeight(), x, y);
        for (int col = 0; col < slots; col++) {
            int w = col + 1 == slots ? SLOT_SIZE - 1 : SLOT_SIZE;
            blitPart(g, b, gridLeft, 0, w, b.getSrcHeight(), x + gridLeft + col * SLOT_SIZE, y);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Drawing helpers
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Draws a row texture widened to the given number of columns, by inserting copies of the column before
     * {@code insertAt}: the texture is kept as is on both sides.
     */
    private static void drawWideRow(GuiGraphics g, Blitter b, int x, int y, int columns, int insertAt) {
        int extraColumns = columns - BASE_COLUMNS;
        drawInserted(g, b, x, y, extraColumns, insertAt, b.getSrcWidth() + extraColumns * SLOT_SIZE);
    }

    /** Same as {@link #drawWideRow}, stopping at {@code endX} (relative to x, in the widened row). */
    private static void drawInserted(GuiGraphics g, Blitter b, int x, int y, int extraColumns, int insertAt, int endX) {
        int height = b.getSrcHeight();
        blitPart(g, b, 0, 0, insertAt, height, x, y);
        for (int col = 0; col < extraColumns; col++) {
            blitPart(g, b, insertAt - SLOT_SIZE, 0, SLOT_SIZE, height, x + insertAt + col * SLOT_SIZE, y);
        }
        int lastX = insertAt + extraColumns * SLOT_SIZE;
        blitPart(g, b, insertAt, 0, endX - lastX, height, x + lastX, y);
    }

    private static boolean hasFullWidthContent(int shoulderRow) {
        return shoulderRow > ShoulderDetector.DEFAULT_SHOULDER_ROW;
    }

    /**
     * Draws the bottom part of a terminal, centered below the widened storage area ("T" shape), with "shoulders"
     * closing the storage area on each side:
     * <ul>
     * <li>usual AE2 textures (notch at row 5): the player inventory part keeps its width (176px);</li>
     * <li>textures with full-width content above the inventory (e.g. the ae2wtlib wireless crafting terminal): one
     * block keeps the texture width (195px) down to the bottom, with the inventory centered in it.</li>
     * </ul>
     * The added edges are stretched from the texture itself (notch and bottom edge), to keep the colors of resource
     * packs.
     */
    private static void drawBottom(GuiGraphics g, Blitter bottom, int x, int y, int columns) {
        int extraWidth = (columns - BASE_COLUMNS) * SLOT_SIZE;
        int halfExtraWidth = extraWidth / 2;
        int srcWidth = bottom.getSrcWidth();
        int srcHeight = bottom.getSrcHeight();
        int width = srcWidth + extraWidth;
        int shoulderRow = ShoulderDetector.shoulderRow(bottom);
        int bandRows = ShoulderDetector.DEFAULT_SHOULDER_ROW;

        // First rows: still part of the storage area. Plain band: 2px borders on each side (outline + highlight),
        // body in between, all stretched from the first row of the texture
        drawPlainBand(g, bottom, x, y, width, bandRows);

        int sy = y + bandRows;
        int blockX = x + halfExtraWidth;
        if (!hasFullWidthContent(shoulderRow)) {
            blitPart(g, bottom, 0, bandRows, BOTTOM_WIDTH, srcHeight - bandRows, blockX, sy);
            drawShoulders(g, bottom, shoulderRow, x, sy, width, halfExtraWidth, BOTTOM_WIDTH);
            return;
        }

        // Full-width content (with its side borders), down to the notch of the original texture
        blitPart(g, bottom, 0, bandRows, srcWidth, shoulderRow - bandRows, blockX, sy);
        drawShoulders(g, bottom, shoulderRow, x, sy, width, halfExtraWidth, srcWidth);

        // Below: the block keeps its width, with the inventory centered in it (without the inventory's own borders)
        int iy = y + shoulderRow;
        int inventoryRows = srcHeight - shoulderRow - EDGE_ROWS;
        int inventoryShift = (srcWidth - BOTTOM_WIDTH) / 2;
        drawPlainBand(g, bottom, blockX, iy, srcWidth, inventoryRows);
        blitPart(g, bottom, 2, shoulderRow, BOTTOM_WIDTH - 4, inventoryRows, blockX + inventoryShift + 2, iy);

        // Bottom edge of the block, from the bottom edge of the inventory part: corners + stretched middle
        int edgeY = srcHeight - EDGE_ROWS;
        int ey = iy + inventoryRows;
        blitPart(g, bottom, 0, edgeY, 1, EDGE_ROWS, blockX, ey);
        blitStretched(g, bottom, BOTTOM_WIDTH / 2, edgeY, 1, EDGE_ROWS, blockX + 1, ey, srcWidth - 2, EDGE_ROWS);
        blitPart(g, bottom, BOTTOM_WIDTH - 1, edgeY, 1, EDGE_ROWS, blockX + srcWidth - 1, ey);
    }

    /** Plain band: 2px borders on each side (outline + highlight) and body, stretched from the first texture row. */
    private static void drawPlainBand(GuiGraphics g, Blitter b, int x, int y, int width, int height) {
        int srcWidth = b.getSrcWidth();
        blitStretched(g, b, 0, 0, 2, 1, x, y, 2, height);
        blitStretched(g, b, 2, 0, 1, 1, x + 2, y, width - 4, height);
        blitStretched(g, b, srcWidth - 2, 0, 2, 1, x + width - 2, y, 2, height);
    }

    /**
     * Closes the bottom edge of the storage area on both sides of the narrower block below it, like the notch of the
     * original texture (highlight, 2 rows of shadow, outline), stretched from that notch. The shoulders also cover
     * the outline column of the block, whose vertical border only starts below them.
     */
    private static void drawShoulders(GuiGraphics g, Blitter bottom, int shoulderRow, int x, int sy, int width,
            int blockX, int blockWidth) {
        int srcWidth = bottom.getSrcWidth();
        // A column inside the notch of the texture, and its outline column (right border)
        int notchX = srcWidth - 10;
        int outlineX = srcWidth - 1;

        // Left (absent from the original texture, where the inventory part is left-aligned): the left border column
        // of the texture is the outline
        if (blockX > 0) {
            blitPart(g, bottom, 0, shoulderRow, 1, EDGE_ROWS, x, sy);
            blitStretched(g, bottom, notchX, shoulderRow, 1, EDGE_ROWS, x + 1, sy, blockX, EDGE_ROWS);
        }

        // Right
        int rx = x + blockX + blockWidth - 1;
        int right = x + width;
        blitStretched(g, bottom, notchX, shoulderRow, 1, EDGE_ROWS, rx, sy, right - 1 - rx, EDGE_ROWS);
        blitPart(g, bottom, outlineX, shoulderRow, 1, EDGE_ROWS, right - 1, sy);
    }

    private static void blitStretched(GuiGraphics g, Blitter b, int srcDx, int srcDy, int srcW, int srcH, int x, int y,
            int w, int h) {
        if (w > 0 && h > 0) {
            b.copy().src(b.getSrcX() + srcDx, b.getSrcY() + srcDy, srcW, srcH).dest(x, y, w, h).blit(g);
        }
    }

    private static void blitPart(GuiGraphics g, Blitter b, int srcDx, int srcDy, int w, int h, int x, int y) {
        if (w > 0 && h > 0) {
            b.copy().src(b.getSrcX() + srcDx, b.getSrcY() + srcDy, w, h).dest(x, y).blit(g);
        }
    }
}
