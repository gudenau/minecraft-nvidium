package me.cortex.nvidium.mixin.sodium;

import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttribute;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(GlVertexFormat.class)
public interface GlVertexFormatAccessor {
    @Accessor Map<VertexFormatAttribute, GlVertexAttribute> getAttributesKeyed();
}
