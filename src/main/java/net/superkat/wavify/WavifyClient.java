package net.superkat.wavify;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.superkat.wavify.duck.WavifyWorld;
import net.superkat.wavify.sprite.WavifySpriteHandler;

/**
 * Client-side holder (replaces the Fabric {@code ClientModInitializer} "WavifyClient").
 *
 * <p>The actual event hookups moved to {@link WavifyClientModEvents} (mod bus) and
 * {@link WavifyClientForgeEvents} (game bus). This class keeps the shared sprite handler and the
 * wave render layer so {@code WaveRenderer} can still reference {@code WavifyClient.WAVIFY_SPRITE_HANDLER}.
 */
public class WavifyClient {
    public static final WavifySpriteHandler WAVIFY_SPRITE_HANDLER = new WavifySpriteHandler();
    private static RenderType waveRenderLayer;

    public static RenderType getWaveRenderLayer() {
        if (waveRenderLayer == null) {
            waveRenderLayer = RenderTypes.entityTranslucent(WavifySpriteHandler.WAVE_ATLAS_ID, false);
        }
        return waveRenderLayer;
    }

    /**
     * Builds and draws the waves for the current client level. Invoked from the FrameGraph pass
     * registered in {@link WavifyClientForgeEvents#onAddFramePass} — the Forge 26.1 replacement for
     * Fabric's {@code LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN}.
     */
    public static void renderWaves() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        WavifyWorld wavifyWorld = (WavifyWorld) mc.level;
        RenderType layer = getWaveRenderLayer();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.begin(layer.mode(), layer.format());
        wavifyWorld.wavify$wavifyWaveHandler().render(buffer);
        MeshData builtBuffer = buffer.build();
        if (builtBuffer == null) {
            return;
        }
        layer.draw(builtBuffer);
    }
}
