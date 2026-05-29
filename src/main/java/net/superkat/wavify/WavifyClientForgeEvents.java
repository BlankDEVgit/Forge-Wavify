package net.superkat.wavify;

import com.mojang.blaze3d.framegraph.FramePass;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.FramePassManager;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.superkat.wavify.duck.WavifyWorld;

/**
 * Client-side <b>game (Forge) event bus</b> handlers — the runtime half of the old onInitializeClient.
 * Gated to {@code Dist.CLIENT}. Each handler is annotated with the Fabric callback it replaces.
 *
 * <p>Forge 26.1 (build 64.x) runs on the rewritten EventBus 7 (record events; {@code @SubscribeEvent}
 * moved to {@code net.minecraftforge.eventbus.api.listener}) and the FrameGraph render pipeline
 * (the old {@code RenderLevelStageEvent} was removed in favour of {@code AddFramePassEvent}).
 *
 * <p>Not ported: Fabric's {@code InvalidateRenderStateCallback} (no Forge equivalent) and
 * {@code ClientLifecycleEvents.CLIENT_STOPPING} (atlas cleanup at shutdown is unnecessary). The Fabric
 * {@code AFTER_CLIENT_LEVEL_CHANGE} reset is also intentionally dropped: every dimension change builds a
 * brand-new {@code ClientLevel} (hence a brand-new {@code WavifyWaveHandler} that already starts with
 * {@code nearbyChunksLoaded == false}), so the reset is implicit in construction. Hooking
 * {@code LevelEvent.Load} instead would NPE — it fires inside the ClientLevel constructor, before the
 * mixin's TAIL injection has assigned the handler.
 */
// No `bus` param: EventBus 7 auto-routes each @SubscribeEvent to its correct BusGroup by event type.
// All handlers here (TickEvent.LevelTickEvent.Post, ChunkEvent, AddFramePassEvent) are default/game-bus.
@Mod.EventBusSubscriber(modid = Wavify.MOD_ID, value = Dist.CLIENT)
public class WavifyClientForgeEvents {

    /** Fabric: ClientTickEvents.END_LEVEL_TICK. The Post record == end of the level tick. */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.level() instanceof ClientLevel clientLevel) {
            ((WavifyWorld) clientLevel).wavify$wavifyWaveHandler().tick();
        }
    }

    /** Fabric: ClientChunkEvents.CHUNK_LOAD. */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel clientLevel) {
            ((WavifyWorld) clientLevel).wavify$wavifyWaveHandler().waterHandler.loadChunk(event.getChunk());
        }
    }

    /** Fabric: ClientChunkEvents.CHUNK_UNLOAD. */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel clientLevel) {
            ((WavifyWorld) clientLevel).wavify$wavifyWaveHandler().waterHandler.unloadChunk(event.getChunk());
        }
    }

    /**
     * Fabric: LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.
     *
     * <p>Forge 26.1 replaced the per-frame {@code RenderLevelStageEvent} with the FrameGraph:
     * {@code AddFramePassEvent} fires once to REGISTER a render pass whose {@code executes()} runs each
     * frame. The pass binds the main colour target so the waves draw after the translucent terrain.
     */
    @SubscribeEvent
    public static void onAddFramePass(AddFramePassEvent event) {
        event.addPass(Identifier.fromNamespaceAndPath(Wavify.MOD_ID, "wave_pass"), new FramePassManager.PassDefinition() {
            @Override
            public void extracts(LevelTargetBundle bundle, FramePass pass) {
                // A pass must bind at least one target; bind the main colour target so it is scheduled
                // after terrain. (Exact target/ordering for "after translucent terrain" may need runtime
                // tuning against the 26.1 FrameGraph — see README "Remaining risks".)
                pass.readsAndWrites(bundle.main);
            }

            @Override
            public void executes(LevelRenderState state) {
                WavifyClient.renderWaves();
            }
        });
    }
}
