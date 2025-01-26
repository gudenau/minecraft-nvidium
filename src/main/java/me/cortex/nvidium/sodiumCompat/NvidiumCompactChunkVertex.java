package me.cortex.nvidium.sodiumCompat;


import me.cortex.nvidium.mixin.sodium.GlVertexFormatAccessor;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.system.MemoryUtil;

public class NvidiumCompactChunkVertex implements ChunkVertexType {
    public static final int STRIDE = 16;
    public static final GlVertexFormat VERTEX_FORMAT = new GlVertexFormat(
        ((GlVertexFormatAccessor) CompactChunkVertex.VERTEX_FORMAT).getAttributesKeyed(),
        null,
        STRIDE
    );
    public static final NvidiumCompactChunkVertex INSTANCE = new NvidiumCompactChunkVertex();

    private static final int POSITION_MAX_VALUE = 65536;
    public static final int TEXTURE_MAX_VALUE = 32768;

    private static final float MODEL_ORIGIN = 8.0f;
    private static final float MODEL_RANGE = 32.0f;
    private static final float MODEL_SCALE_INV = POSITION_MAX_VALUE / MODEL_RANGE;

    @Override
    public GlVertexFormat getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder getEncoder() {
        return (ptr, material, vertices, sectionIndex) -> {
            for(int i = 0; i < 4; i++) {
                var vertex = vertices[i];

                int light = compactLight(vertex.light);

                MemoryUtil.memPutInt(ptr + 0, (encodePosition(vertex.x) << 0) | (encodePosition(vertex.y) << 16));
                MemoryUtil.memPutInt(ptr + 4, (encodePosition(vertex.z) << 0) | (encodeDrawParameters(material) << 16) | ((light & 0xFF) << 24));
                MemoryUtil.memPutInt(ptr + 8, (encodeColor(vertex.color) << 0) | (((light >> 8) & 0xFF) << 24));
                MemoryUtil.memPutInt(ptr + 12, encodeTexture(vertex.u, vertex.v));

                ptr += STRIDE;
            }

            return ptr;
        };
    }


    private static int compactLight(int light) {
        int sky = MathHelper.clamp((light >>> 16) & 0xFF, 8, 248);
        int block = MathHelper.clamp((light >>>  0) & 0xFF, 8, 248);

        return (block << 0) | (sky << 8);
    }

    private static int encodePosition(float v) {
        return (int) ((MODEL_ORIGIN + v) * MODEL_SCALE_INV);
    }

    private static int encodeDrawParameters(int material) {
        return ((material & 0xFF) << 0);
    }


    private static int encodeColor(int color) {
        var brightness = ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(color));

        int r = ColorU8.normalizedFloatToByte(ColorU8.byteToNormalizedFloat(ColorABGR.unpackRed(color)) * brightness);
        int g = ColorU8.normalizedFloatToByte(ColorU8.byteToNormalizedFloat(ColorABGR.unpackGreen(color)) * brightness);
        int b = ColorU8.normalizedFloatToByte(ColorU8.byteToNormalizedFloat(ColorABGR.unpackBlue(color)) * brightness);

        return ColorABGR.pack(r, g, b, 0x00);
    }

    private static int encodeTexture(float u, float v) {
        return ((Math.round(u * TEXTURE_MAX_VALUE) & 0xFFFF) << 0) |
                ((Math.round(v * TEXTURE_MAX_VALUE) & 0xFFFF) << 16);
    }
}
