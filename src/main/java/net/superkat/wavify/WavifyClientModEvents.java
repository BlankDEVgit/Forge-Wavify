package net.superkat.wavify;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.superkat.wavify.duck.WavifyWorld;
import net.superkat.wavify.event.ClientBlockUpdateEvent;
import net.superkat.wavify.particles.BigSplashParticle;
import net.superkat.wavify.particles.SprayParticle;
import net.superkat.wavify.particles.WavifySplashParticle;
import net.superkat.wavify.particles.WhiteSprayParticle;
import net.superkat.wavify.particles.debug.DebugShoreParticle;
import net.superkat.wavify.particles.debug.DebugWaterParticle;
import net.superkat.wavify.particles.debug.DebugWaveMovementParticle;

/**
 * Client-side <b>mod event bus</b> handlers (the registration half of the old onInitializeClient).
 * Gated to {@code Dist.CLIENT} so nothing here is class-loaded on a dedicated server.
 */
// No `bus` param: EventBus 7 auto-routes each @SubscribeEvent to its correct BusGroup by event type
// (IModBusEvent → mod bus; everything else → default/game bus). RegisterParticleProvidersEvent and
// FMLClientSetupEvent are mod-bus; RegisterClientReloadListenersEvent is on the default bus — so a
// fixed `bus = Bus.MOD` would (and did) crash under strictRuntimeChecks.
@Mod.EventBusSubscriber(modid = Wavify.MOD_ID, value = Dist.CLIENT)
public class WavifyClientModEvents {

    /** Fabric: ParticleProviderRegistry.getInstance().register(...). */
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WavifyParticles.SPRAY_PARTICLE, SprayParticle.Factory::new);
        event.registerSpriteSet(WavifyParticles.WHITE_SPRAY_PARTICLE, WhiteSprayParticle.Factory::new);
        event.registerSpriteSet(WavifyParticles.SPLASH_PARTICLE, WavifySplashParticle.Factory::new);
        event.registerSpriteSet(WavifyParticles.BIG_SPLASH_PARTICLE, BigSplashParticle.Factory::new);
        event.registerSpriteSet(WavifyParticles.DEBUG_WATERBODY_PARTICLE, DebugWaterParticle.Factory::new);
        event.registerSpriteSet(WavifyParticles.DEBUG_SHORELINE_PARTICLE, DebugShoreParticle.Factory::new);
        event.registerSpriteSet(WavifyParticles.DEBUG_WAVEMOVEMENT_PARTICLE, DebugWaveMovementParticle.Factory::new);
    }

    /** Fabric: ResourceManagerHelper.get(CLIENT_RESOURCES).registerReloadListener(...). */
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(WavifyClient.WAVIFY_SPRITE_HANDLER);
    }

    /** Registers the block-update callback once, replacing the inline Fabric registration. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ClientBlockUpdateEvent.register((pos, state) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || client.player == null) {
                return;
            }
            WavifyWorld wavifyWorld = (WavifyWorld) client.level;
            wavifyWorld.wavify$wavifyWaveHandler().waterHandler.onBlockUpdate(pos, state);
        }));
    }
}
