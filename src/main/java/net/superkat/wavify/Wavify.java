package net.superkat.wavify;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import net.superkat.wavify.config.WavifyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod entry point (replaces the Fabric {@code ModInitializer} entry "net.superkat.wavify.Wavify").
 *
 * <p>Only physical-side-agnostic setup happens here. Everything that touches client classes
 * (particle providers, rendering, world ticking, the resource reload listener, ...) lives in the
 * {@code Dist.CLIENT}-gated subscriber classes {@link WavifyClientModEvents} and
 * {@link WavifyClientForgeEvents} so the mod never class-loads client code on a dedicated server.
 */
@Mod(Wavify.MOD_ID)
public class Wavify {
    public static final String MOD_ID = "wavify";
    public static final Logger LOGGER = LoggerFactory.getLogger("wavify");

    public Wavify(FMLJavaModLoadingContext context) {
        // EventBus 7 (Forge 64.x): the mod bus is a BusGroup, and each event exposes its own bus via
        // EventClass.getBus(modBusGroup). The legacy getModEventBus()/addListener model was removed.
        BusGroup modBus = context.getModBusGroup();

        // Replaces midnightlib's MidnightConfig.init(MOD_ID, WavifyConfig.class).
        WavifyConfig.register(context);

        // Register the particle types into the (common) particle-type registry.
        // Replaces WavifyParticles.registerParticles() which used Fabric's FabricParticleTypes.
        RegisterEvent.getBus(modBus).addListener(WavifyParticles::onRegister);
    }
}
