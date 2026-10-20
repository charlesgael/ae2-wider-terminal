package com.cdjedai.wide_ae2_terminals;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Applies the mixin of the ExtendedAE pattern access terminal only when the ExtendedAE class still has everything it
 * targets: with another ExtendedAE version, that terminal is simply not widened, instead of a crash at startup (the
 * injectors are required). The AE2 mixins stay strict: the supported AE2 versions are restricted in neoforge.mods.toml.
 */
public class WideMixinPlugin implements IMixinConfigPlugin {
    // Not the mod logger: this class is loaded by Mixin, before the mod
    private static final Logger LOGGER = LoggerFactory.getLogger("wide_ae2_terminals");

    private static final String EX_PATTERN_TERMINAL_MIXIN = "com.cdjedai.wide_ae2_terminals.mixin.ExPatternTerminalMixin";
    private static final String EX_PATTERN_TERMINAL = "com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal";
    private static final String GUI_GRAPHICS = "Lnet/minecraft/client/gui/GuiGraphics;";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!EX_PATTERN_TERMINAL_MIXIN.equals(mixinClassName)) {
            return true;
        }
        try {
            String missing = checkExPatternTerminal();
            if (missing != null) {
                LOGGER.warn("ExtendedAE pattern access terminal not widened: unsupported ExtendedAE version ({})",
                        missing);
                return false;
            }
            return true;
        } catch (ClassNotFoundException e) {
            return false; // ExtendedAE is not installed
        } catch (Exception e) {
            LOGGER.warn("ExtendedAE pattern access terminal not widened: could not check the ExtendedAE classes", e);
            return false;
        }
    }

    /** Returns what is missing in the ExtendedAE classes, or null if everything targeted by the mixin is there. */
    private static String checkExPatternTerminal() throws Exception {
        ClassNode node = classNode(EX_PATTERN_TERMINAL);

        for (var field : List.of("rows:Ljava/util/ArrayList;", "highlightBtns:Ljava/util/HashMap;",
                "scrollbar:Lappeng/client/gui/widgets/Scrollbar;", "visibleRows:I")) {
            String[] parts = field.split(":");
            if (node.fields.stream().noneMatch(f -> f.name.equals(parts[0]) && f.desc.equals(parts[1]))) {
                return "field " + parts[0];
            }
        }

        Predicate<AbstractInsnNode> push162 = pushes(162);
        Predicate<AbstractInsnNode> push145 = pushes(145);
        Predicate<AbstractInsnNode> callsResetScrollbar = insn -> insn instanceof MethodInsnNode call
                && call.name.equals("resetScrollbar") && call.desc.equals("()V");
        Map<String, List<Predicate<AbstractInsnNode>>> methods = Map.of(
                "init()V", List.of(),
                "refreshList()V", List.of(callsResetScrollbar),
                "drawBG(" + GUI_GRAPHICS + "IIIIF)V", List.of(),
                "drawFG(" + GUI_GRAPHICS + "IIII)V", List.of(push162, push145),
                "renderLinkStatus(" + GUI_GRAPHICS + "Lappeng/api/storage/ILinkStatus;)V", List.of(push162),
                "getHoveredLineIndex(II)I", List.of(push162));
        for (var entry : methods.entrySet()) {
            MethodNode method = node.methods.stream()
                    .filter(m -> (m.name + m.desc).equals(entry.getKey()))
                    .findFirst().orElse(null);
            if (method == null) {
                return "method " + entry.getKey();
            }
            for (var expected : entry.getValue()) {
                boolean found = false;
                for (var insn : method.instructions) {
                    if (expected.test(insn)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return "content of method " + entry.getKey();
                }
            }
        }

        ClassNode slotsRow = classNode(EX_PATTERN_TERMINAL + "$SlotsRow");
        if (slotsRow.methods.stream().noneMatch(m -> m.name.equals("<init>")
                && m.desc.equals("(Lappeng/client/gui/me/patternaccess/PatternContainerRecord;II)V"))) {
            return "SlotsRow constructor";
        }
        return null;
    }

    private static ClassNode classNode(String className) throws Exception {
        var provider = MixinService.getService().getBytecodeProvider();
        try {
            return provider.getClassNode(className);
        } catch (ClassNotFoundException e) {
            return provider.getClassNode(className.replace('.', '/'));
        }
    }

    private static Predicate<AbstractInsnNode> pushes(int value) {
        return insn -> insn instanceof IntInsnNode push
                && (push.getOpcode() == Opcodes.SIPUSH || push.getOpcode() == Opcodes.BIPUSH)
                && push.operand == value;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
