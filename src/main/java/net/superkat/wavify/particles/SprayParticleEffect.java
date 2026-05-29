package net.superkat.wavify.particles;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.superkat.wavify.WavifyParticles;

public class SprayParticleEffect
implements ParticleOptions {
    public static final MapCodec<SprayParticleEffect> CODEC = SprayParticleEffect.createCodec(SprayParticleEffect::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SprayParticleEffect> PACKET_CODEC = SprayParticleEffect.createPacketCodec(SprayParticleEffect::new);
    protected final float yaw;
    protected final float intensity;
    protected final float scale;

    protected static <T extends SprayParticleEffect> MapCodec<T> createCodec(Function3<Float, Float, Float, T> particle) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.FLOAT.fieldOf("yaw").forGetter(SprayParticleEffect::getYaw), Codec.FLOAT.fieldOf("intensity").forGetter(SprayParticleEffect::getIntensity), Codec.FLOAT.fieldOf("scale").forGetter(SprayParticleEffect::getScale)).apply(instance, particle));
    }

    protected static <T extends SprayParticleEffect> StreamCodec<RegistryFriendlyByteBuf, T> createPacketCodec(Function3<Float, Float, Float, T> particle) {
        return StreamCodec.composite((StreamCodec)ByteBufCodecs.FLOAT, SprayParticleEffect::getYaw, (StreamCodec)ByteBufCodecs.FLOAT, SprayParticleEffect::getIntensity, (StreamCodec)ByteBufCodecs.FLOAT, SprayParticleEffect::getScale, particle);
    }

    public SprayParticleEffect(float yaw, float intensity, float scale) {
        this.yaw = yaw;
        this.intensity = intensity;
        this.scale = scale;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getIntensity() {
        return this.intensity;
    }

    public float getScale() {
        return this.scale;
    }

    public ParticleType<?> getType() {
        return WavifyParticles.SPRAY_PARTICLE;
    }
}

