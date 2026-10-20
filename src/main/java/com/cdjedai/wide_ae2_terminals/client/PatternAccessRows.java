package com.cdjedai.wide_ae2_terminals.client;

import appeng.client.gui.me.patternaccess.PatternContainerRecord;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Access to a package-private {@code SlotsRow(PatternContainerRecord container, int offset, int slots)} record of a
 * pattern access terminal screen, which describes one row of slots of a pattern container. AE2 and ExtendedAE each
 * have their own copy of it.
 */
public final class PatternAccessRows {
    public static final PatternAccessRows AE2 = new PatternAccessRows(
            "appeng.client.gui.me.patternaccess.PatternAccessTermScreen$SlotsRow");

    private final Class<?> slotsRow;
    private final Constructor<?> constructor;
    private final Method container;
    private final Method offset;
    private final Method slots;

    public PatternAccessRows(String slotsRowClass) {
        try {
            slotsRow = Class.forName(slotsRowClass);
            constructor = slotsRow.getDeclaredConstructor(PatternContainerRecord.class, int.class, int.class);
            container = slotsRow.getDeclaredMethod("container");
            offset = slotsRow.getDeclaredMethod("offset");
            slots = slotsRow.getDeclaredMethod("slots");
            constructor.setAccessible(true);
            container.setAccessible(true);
            offset.setAccessible(true);
            slots.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported version: " + slotsRowClass + " not found", e);
        }
    }

    /**
     * Rows re-wrapped to a column count, and for each container, the index of its first row before (key) and after
     * (value) the re-wrapping.
     */
    public record Rewrapped<R>(List<R> rows, Map<Integer, Integer> firstRowIndices) {
    }

    public boolean isSlotsRow(Object row) {
        return slotsRow.isInstance(row);
    }

    public int slots(Object row) {
        return (int) invoke(slots, row);
    }

    /**
     * Re-wraps the slots of each pattern container (wrapped every 9 slots) to the given number of columns. The rows
     * of one container are consecutive in the list; other rows (group headers) are kept as they are.
     */
    public <R> Rewrapped<R> rewrap(List<R> rows, int columns) {
        List<R> result = new ArrayList<>(rows.size());
        Map<Integer, Integer> firstRowIndices = new HashMap<>();
        int i = 0;
        while (i < rows.size()) {
            R row = rows.get(i);
            if (!isSlotsRow(row)) {
                result.add(row);
                i++;
                continue;
            }

            firstRowIndices.put(i, result.size());
            Object rowContainer = invoke(container, row);
            int start = (int) invoke(offset, row);
            int total = 0;
            while (i < rows.size() && isSlotsRow(rows.get(i)) && invoke(container, rows.get(i)) == rowContainer) {
                total += slots(rows.get(i));
                i++;
            }
            for (int rowOffset = 0; rowOffset < total; rowOffset += columns) {
                result.add(create(rowContainer, start + rowOffset, Math.min(total - rowOffset, columns)));
            }
        }
        return new Rewrapped<>(result, firstRowIndices);
    }

    @SuppressWarnings("unchecked")
    private <R> R create(Object rowContainer, int rowOffset, int rowSlots) {
        try {
            return (R) constructor.newInstance(rowContainer, rowOffset, rowSlots);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object invoke(Method method, Object row) {
        try {
            return method.invoke(row);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
