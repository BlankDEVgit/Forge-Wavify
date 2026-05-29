package net.superkat.wavify.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Wavify configuration.
 *
 * <p>The Fabric original used midnightlib ({@code eu.midnightdust.lib.config.MidnightConfig}), which
 * has no classic-Forge build for 26.1, so this is reimplemented with Forge's native
 * {@link ForgeConfigSpec}. The public static fields and their names/defaults are kept identical to the
 * original so every read-site in the mod (WaterHandler, WavifyWaveHandler, DebugHelper, ...) is
 * unchanged. The three user-tunable options (chunkRadius, chunkUpdatesRescanAmount, debug) are backed
 * by the config spec and written to the static fields on load/reload; the rest stay as plain fields
 * exactly as in the original (they were never config-screen entries).
 *
 * <p>The config is stored at {@code config/wavify-client.toml}. For an in-game editing screen with
 * sliders (the feature midnightlib provided), install the optional "Configured" mod.
 */
public class WavifyConfig {
    public static final String WAVES = "waves";

    // --- User-configurable values (had @MidnightConfig.Entry in the Fabric original) ---
    public static int chunkRadius = 5;
    public static int chunkUpdatesRescanAmount = 50;
    public static boolean debug = false;

    // --- Internal tuning values (plain static fields in the original, not config-screen entries) ---
    public static int waveTicks = 80;
    public static int waveDistFromShore = 8;
    public static boolean modEnabled = true;

    // --- ForgeConfigSpec backing ---
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.IntValue CHUNK_RADIUS;
    private static final ForgeConfigSpec.IntValue CHUNK_UPDATES_RESCAN_AMOUNT;
    private static final ForgeConfigSpec.BooleanValue DEBUG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push(WAVES);

        CHUNK_RADIUS = builder
                .comment("Render Radius - the chunk radius of chunks that should spawn waves. Reduce this number if you're struggling for performance!")
                .translation("wavify.midnightconfig.chunkRadius")
                .defineInRange("chunkRadius", 5, 3, 16);

        CHUNK_UPDATES_RESCAN_AMOUNT = builder
                .comment("Chunk Updates Rescan Amount - the amount of block updates that should happen in a chunk before it is rescanned for wave spawning.")
                .translation("wavify.midnightconfig.chunkUpdatesRescanAmount")
                .defineInRange("chunkUpdatesRescanAmount", 50, 1, 1024);

        DEBUG = builder
                .comment("Debug Mode - helpful for figuring out where waves are spawning, and how.")
                .translation("wavify.midnightconfig.debug")
                .define("debug", false);

        builder.pop();
        SPEC = builder.build();
    }

    /** Registers the client config and the load/reload sync listeners on the mod event bus (EventBus 7). */
    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, SPEC);
        BusGroup modBus = context.getModBusGroup();
        ModConfigEvent.Loading.getBus(modBus).addListener(WavifyConfig::onLoad);
        ModConfigEvent.Reloading.getBus(modBus).addListener(WavifyConfig::onReload);
    }

    private static void onLoad(ModConfigEvent.Loading event) {
        sync(event.getConfig());
    }

    private static void onReload(ModConfigEvent.Reloading event) {
        sync(event.getConfig());
    }

    private static void sync(ModConfig config) {
        if (config.getSpec() != SPEC) {
            return;
        }
        chunkRadius = CHUNK_RADIUS.get();
        chunkUpdatesRescanAmount = CHUNK_UPDATES_RESCAN_AMOUNT.get();
        debug = DEBUG.get();
    }
}
