package net.superkat.wavify;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraftforge.registries.RegisterEvent;
import net.superkat.wavify.particles.SprayParticleEffect;
import net.superkat.wavify.particles.WhiteSprayParticleEffect;
import net.superkat.wavify.particles.debug.DebugShoreParticle;
import net.superkat.wavify.particles.debug.DebugWaterParticle;
import net.superkat.wavify.particles.debug.DebugWaveMovementParticle;

/**
 * Particle type registration.
 *
 * <p>The Fabric original used {@code FabricParticleTypes.complex(...)} / {@code .simple()} plus
 * {@code Registry.register(BuiltInRegistries.PARTICLE_TYPE, ...)}. On Forge the registry is frozen
 * outside of the registration phase, so the types are created eagerly here and registered through
 * {@link RegisterEvent}. The field types stay as plain {@link ParticleType}/{@link SimpleParticleType}
 * (rather than DeferredRegister's RegistryObject) so the effect classes' {@code getType()} and the
 * provider registration keep referencing them directly, matching the original.
 */
public class WavifyParticles {
    public static final String MOD_ID = "wavify";

    public static final ParticleType<SprayParticleEffect> SPRAY_PARTICLE =
            complex(SprayParticleEffect.CODEC, SprayParticleEffect.PACKET_CODEC);
    public static final ParticleType<WhiteSprayParticleEffect> WHITE_SPRAY_PARTICLE =
            complex(WhiteSprayParticleEffect.CODEC, WhiteSprayParticleEffect.PACKET_CODEC);
    public static final SimpleParticleType SPLASH_PARTICLE = simple();
    public static final SimpleParticleType BIG_SPLASH_PARTICLE = simple();
    public static final ParticleType<DebugWaterParticle.DebugWaterParticleEffect> DEBUG_WATERBODY_PARTICLE =
            complex(DebugWaterParticle.DebugWaterParticleEffect.CODEC, DebugWaterParticle.DebugWaterParticleEffect.PACKET_CODEC);
    public static final ParticleType<DebugShoreParticle.DebugShoreParticleEffect> DEBUG_SHORELINE_PARTICLE =
            complex(DebugShoreParticle.DebugShoreParticleEffect.CODEC, DebugShoreParticle.DebugShoreParticleEffect.PACKET_CODEC);
    public static final ParticleType<DebugWaveMovementParticle.DebugWaveMovementParticleEffect> DEBUG_WAVEMOVEMENT_PARTICLE =
            complex(DebugWaveMovementParticle.DebugWaveMovementParticleEffect.CODEC, DebugWaveMovementParticle.DebugWaveMovementParticleEffect.PACKET_CODEC);

    /** Mod-event-bus listener registered in {@link Wavify}'s constructor. */
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.PARTICLE_TYPE, helper -> {
            helper.register(id("spray_particle"), SPRAY_PARTICLE);
            helper.register(id("white_spray_particle"), WHITE_SPRAY_PARTICLE);
            helper.register(id("splash"), SPLASH_PARTICLE);
            helper.register(id("bigsplash"), BIG_SPLASH_PARTICLE);
            helper.register(id("debug_waterbody_particle"), DEBUG_WATERBODY_PARTICLE);
            helper.register(id("debug_shoreline_particle"), DEBUG_SHORELINE_PARTICLE);
            helper.register(id("debug_wavemovement_particle"), DEBUG_WAVEMOVEMENT_PARTICLE);
        });
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Equivalent of {@code FabricParticleTypes.complex(codec, streamCodec)}. */
    private static <T extends ParticleOptions> ParticleType<T> complex(MapCodec<T> codec,
                                                                       StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return new ParticleType<T>(false) {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        };
    }

    /** Equivalent of {@code FabricParticleTypes.simple()} (SimpleParticleType's constructor is protected). */
    private static SimpleParticleType simple() {
        return new SimpleParticleType(false) {};
    }
}
