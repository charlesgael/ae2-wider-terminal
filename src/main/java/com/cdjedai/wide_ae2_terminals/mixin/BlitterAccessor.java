package com.cdjedai.wide_ae2_terminals.mixin;

import appeng.client.gui.style.Blitter;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Blitter.class)
public interface BlitterAccessor {
    @Accessor("texture")
    ResourceLocation wide$getTexture();

    @Accessor("referenceWidth")
    int wide$getReferenceWidth();

    @Accessor("referenceHeight")
    int wide$getReferenceHeight();
}
