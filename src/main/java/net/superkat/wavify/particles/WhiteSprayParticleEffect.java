package net.superkat.wavify.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.superkat.wavify.WavifyParticles;
import net.superkat.wavify.particles.SprayParticleEffect;

public class WhiteSprayParticleEffect
extends SprayParticleEffect {
    public static final MapCodec<WhiteSprayParticleEffect> CODEC = WhiteSprayParticleEffect.createCodec(WhiteSprayParticleEffect::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, WhiteSprayParticleEffect> PACKET_CODEC = WhiteSprayParticleEffect.createPacketCodec(WhiteSprayParticleEffect::new);

    public WhiteSprayParticleEffect(float yaw, float intensity, float scale) {
        super(yaw, intensity, scale);
    }

    @Override
    public ParticleType<?> getType() {
        return WavifyParticles.WHITE_SPRAY_PARTICLE;
    }
}

