package me.cortex.nvidium.renderers;

import com.mojang.blaze3d.platform.GlStateManager;
import me.cortex.nvidium.gl.shader.Shader;
import me.cortex.nvidium.sodiumCompat.ShaderLoader;
import me.cortex.nvidium.mixin.minecraft.LightMapAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GL45C;

import static me.cortex.nvidium.RenderPipeline.GL_DRAW_INDIRECT_ADDRESS_NV;
import static me.cortex.nvidium.gl.shader.ShaderType.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL33.glGenSamplers;
import static org.lwjgl.opengl.NVMeshShader.glMultiDrawMeshTasksIndirectNV;
import static org.lwjgl.opengl.NVVertexBufferUnifiedMemory.glBufferAddressRangeNV;

public class PrimaryTerrainRasterizer extends Phase {
    private final int blockSampler = glGenSamplers();
    private final int lightSampler = glGenSamplers();
    private final Shader shader = Shader.make()
            .addSource(TASK, ShaderLoader.parse(Identifier.of("nvidium", "terrain/task.glsl")))
            .addSource(MESH, ShaderLoader.parse(Identifier.of("nvidium", "terrain/mesh.glsl")))
            .addSource(FRAGMENT, ShaderLoader.parse(Identifier.of("nvidium", "terrain/frag.frag"))).compile();

    public PrimaryTerrainRasterizer() {
        GL45C.glSamplerParameteri(blockSampler,     GL45C.GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MIN_LOD, 0);
        GL45C.glSamplerParameteri(blockSampler, GL45C.GL_TEXTURE_MAX_LOD, 4);
        GL45C.glSamplerParameteri(lightSampler, GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
        GL45C.glSamplerParameteri(lightSampler, GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
        GL45C.glSamplerParameteri(lightSampler, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GL45C.glSamplerParameteri(lightSampler, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    private static void setTexture(int textureId, int bindingPoint) {
        GlStateManager._activeTexture(GL_TEXTURE0 + bindingPoint);
        GlStateManager._bindTexture(textureId);
    }

    public void raster(int regionCount, long commandAddr) {
        shader.bind();

        int blockId = MinecraftClient.getInstance().getTextureManager().getTexture(Identifier.of("minecraft", "textures/atlas/blocks.png")).getGlId();
        int lightId = ((LightMapAccessor)MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager()).getLightmapFramebuffer().getColorAttachment();

        GL45C.glBindSampler(0, blockSampler);
        GL45C.glBindSampler(1, lightSampler);
        setTexture(blockId, 0);
        setTexture(lightId, 1);

        glBufferAddressRangeNV(GL_DRAW_INDIRECT_ADDRESS_NV, 0, commandAddr, regionCount*8L);//Bind the command buffer
        glMultiDrawMeshTasksIndirectNV( 0, regionCount, 0);
        GL45C.glBindSampler(0, 0);
        GL45C.glBindSampler(1, 0);
    }

    public void delete() {
        GL45.glDeleteSamplers(blockSampler);
        GL45.glDeleteSamplers(lightSampler);
        shader.delete();
    }
}
