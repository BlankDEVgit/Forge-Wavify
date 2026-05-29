package net.superkat.wavify.particles.debug;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ScalableParticleOptionsBase;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.superkat.wavify.WavifyParticles;
import net.superkat.wavify.particles.debug.DebugAbstractColoredParticle;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class DebugWaveMovementParticle
extends DebugAbstractColoredParticle<DebugWaveMovementParticle.DebugWaveMovementParticleEffect> {
    public float yaw = 0.0f;
    public float speed = 0.0f;
    public boolean lifetimeColorMode = false;
    private Vector3f startColor;
    private Vector3f midColor;
    private Vector3f endColor;

    public DebugWaveMovementParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, DebugWaveMovementParticleEffect parameters, SpriteSet spriteProvider) {
        super(level, x, y, z, xd, yd, zd, parameters, spriteProvider);
        this.yaw = parameters.getYaw();
        this.speed = parameters.getSpeed();
        this.lifetime = parameters.getLifetime();
        this.xd = Math.cos(Math.toRadians(this.yaw)) * (double)this.speed;
        this.zd = Math.sin(Math.toRadians(this.yaw)) * (double)this.speed;
        float red = parameters.color.x();
        float green = parameters.color.y();
        float blue = parameters.color.z();
        if (red == 1.0f && green == 1.0f && blue == 1.0f) {
            this.lifetimeColorMode = true;
            this.rCol = red;
            this.gCol = green;
            this.bCol = blue;
        } else {
            this.rCol = this.randomizeColor(parameters.color.x(), 1.0f);
            this.gCol = this.randomizeColor(parameters.color.y(), 1.0f);
            this.bCol = this.randomizeColor(parameters.color.z(), 1.0f);
        }
        this.gravity = 0.0f;
        this.yd = -0.01f;
        this.startColor = new Vector3f(0.007843138f, 0.9647059f, 0.25490198f);
        this.midColor = new Vector3f(0.99215686f, 0.7019608f, 0.25882354f);
        this.endColor = new Vector3f(0.6509804f, 0.06666667f, 0.23921569f);
    }

    public void extract(QuadParticleRenderState state, Camera camera, float tickDelta) {
        if (this.lifetimeColorMode) {
            this.updateColor(tickDelta);
        }
        super.extract(state, camera, tickDelta);
    }

    private void updateColor(float tickDelta) {
        Vector3f vector3f;
        float mAge = (float)this.lifetime / 2.0f;
        if ((float)this.age >= mAge) {
            float f = ((float)this.age - mAge + tickDelta) / (mAge + 1.0f);
            vector3f = new Vector3f((Vector3fc)this.midColor).lerp((Vector3fc)this.endColor, f);
        } else {
            float f = ((float)this.age + tickDelta) / (mAge + 1.0f);
            vector3f = new Vector3f((Vector3fc)this.startColor).lerp((Vector3fc)this.midColor, f);
        }
        this.rCol = vector3f.x();
        this.gCol = vector3f.y();
        this.bCol = vector3f.z();
        this.setAlpha(Mth.lerp((float)((float)this.age / (float)this.lifetime), (float)1.0f, (float)0.0f));
    }

    public static class DebugWaveMovementParticleEffect
    extends ScalableParticleOptionsBase {
        public static final MapCodec<DebugWaveMovementParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ExtraCodecs.VECTOR3F.fieldOf("color").forGetter(effect -> effect.color), SCALE.fieldOf("scale").forGetter(ScalableParticleOptionsBase::getScale), Codec.FLOAT.fieldOf("yaw").forGetter(DebugWaveMovementParticleEffect::getYaw), Codec.FLOAT.fieldOf("speed").forGetter(DebugWaveMovementParticleEffect::getSpeed), Codec.INT.fieldOf("lifetime").forGetter(DebugWaveMovementParticleEffect::getLifetime)).apply(instance, DebugWaveMovementParticleEffect::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, DebugWaveMovementParticleEffect> PACKET_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.VECTOR3F, effect -> effect.color, (StreamCodec)ByteBufCodecs.FLOAT, ScalableParticleOptionsBase::getScale, (StreamCodec)ByteBufCodecs.FLOAT, DebugWaveMovementParticleEffect::getYaw, (StreamCodec)ByteBufCodecs.FLOAT, DebugWaveMovementParticleEffect::getSpeed, (StreamCodec)ByteBufCodecs.INT, DebugWaveMovementParticleEffect::getLifetime, DebugWaveMovementParticleEffect::new);
        private final Vector3fc color;
        private final float yaw;
        private final float speed;
        private final int lifetime;

        public DebugWaveMovementParticleEffect(Vector3fc color, float scale, float yaw, float speed, int lifetime) {
            super(scale);
            this.color = color;
            this.yaw = yaw;
            this.speed = speed;
            this.lifetime = lifetime;
        }

        public ParticleType<DebugWaveMovementParticleEffect> getType() {
            return WavifyParticles.DEBUG_WAVEMOVEMENT_PARTICLE;
        }

        public float getYaw() {
            return this.yaw;
        }

        public float getSpeed() {
            return this.speed;
        }

        public int getLifetime() {
            return this.lifetime;
        }
    }

    public static class Factory
    implements ParticleProvider<DebugWaveMovementParticleEffect> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(DebugWaveMovementParticleEffect dustParticleEffect, ClientLevel clientWorld, double d, double e, double f, double g, double h, double i, RandomSource random) {
            return new DebugWaveMovementParticle(clientWorld, d, e, f, g, h, i, dustParticleEffect, this.spriteProvider);
        }
    }
}

