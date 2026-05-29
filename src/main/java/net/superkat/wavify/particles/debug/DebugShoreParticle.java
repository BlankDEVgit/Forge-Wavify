package net.superkat.wavify.particles.debug;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ScalableParticleOptionsBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.superkat.wavify.WavifyParticles;
import net.superkat.wavify.particles.debug.DebugAbstractColoredParticle;
import org.joml.Vector3fc;

public class DebugShoreParticle
extends DebugAbstractColoredParticle<DebugShoreParticle.DebugShoreParticleEffect> {
    public DebugShoreParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, DebugShoreParticleEffect parameters, SpriteSet spriteProvider) {
        super(level, x, y, z, xd, yd, zd, parameters, spriteProvider);
        this.rCol = this.randomizeColor(parameters.color.x(), 0.8f);
        this.gCol = this.randomizeColor(parameters.color.y(), 0.8f);
        this.bCol = this.randomizeColor(parameters.color.z(), 0.8f);
    }

    public static class DebugShoreParticleEffect
    extends ScalableParticleOptionsBase {
        public static final MapCodec<DebugShoreParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ExtraCodecs.VECTOR3F.fieldOf("color").forGetter(effect -> effect.color), SCALE.fieldOf("scale").forGetter(ScalableParticleOptionsBase::getScale)).apply(instance, DebugShoreParticleEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, DebugShoreParticleEffect> PACKET_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.VECTOR3F, effect -> effect.color, (StreamCodec)ByteBufCodecs.FLOAT, ScalableParticleOptionsBase::getScale, DebugShoreParticleEffect::new);
        private final Vector3fc color;

        public DebugShoreParticleEffect(Vector3fc color, float scale) {
            super(scale);
            this.color = color;
        }

        public ParticleType<DebugShoreParticleEffect> getType() {
            return WavifyParticles.DEBUG_SHORELINE_PARTICLE;
        }
    }

    public static class Factory
    implements ParticleProvider<DebugShoreParticleEffect> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(DebugShoreParticleEffect dustParticleEffect, ClientLevel clientWorld, double d, double e, double f, double g, double h, double i, RandomSource random) {
            return new DebugShoreParticle(clientWorld, d, e, f, g, h, i, dustParticleEffect, this.spriteProvider);
        }
    }
}

