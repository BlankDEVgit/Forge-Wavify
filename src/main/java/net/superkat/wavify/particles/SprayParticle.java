package net.superkat.wavify.particles;

import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.superkat.wavify.WavifyParticles;
import net.superkat.wavify.particles.SprayParticleEffect;
import net.superkat.wavify.particles.WhiteSprayParticleEffect;
import net.superkat.wavify.wave.WavifyWaveHandler;
import org.joml.Quaternionf;

public class SprayParticle
extends SingleQuadParticle {
    private static final double MAX_SQUARED_COLLISION_CHECK_DISTANCE = Mth.square((double)100.0);
    protected final SpriteSet spriteProvider;
    public float yaw;
    public float intensity;
    private boolean stopped;

    public SprayParticle(ClientLevel level, double x, double y, double z, double velX, double velY, double velZ, SprayParticleEffect params, SpriteSet spriteProvider) {
        super(level, x, y, z, velX, velY, velZ, spriteProvider.first());
        this.spriteProvider = spriteProvider;
        this.yaw = params.getYaw();
        this.intensity = params.getIntensity();
        float speed = (float)(Math.abs(velX) + Math.abs(velZ)) / 1.25f;
        this.xd = (float)(Math.cos(Math.toRadians(this.yaw)) * (double)speed);
        this.yd = 0.15f * this.intensity;
        this.zd = (float)(Math.sin(Math.toRadians(this.yaw)) * (double)speed);
        this.lifetime = (int)(50.0f + this.intensity * 5.0f);
        this.quadSize = Mth.clamp((float)(this.intensity * 4.0f), (float)1.0f, (float)(params.getScale() * 2.0f));
        this.hasPhysics = true;
        this.gravity = 0.5f;
        if (this.spawnWhite()) {
            this.level.addParticle((ParticleOptions)new WhiteSprayParticleEffect(this.yaw, this.intensity, this.quadSize), x, y, z, velX, velY, velZ);
            this.updateWaterColor();
        }
        this.roll = 15.0f * this.intensity * 5.0f;
        this.setSpriteFromAge(this.spriteProvider);
    }

    public void tick() {
        super.tick();
        if (this.quadSize <= 0.0f) {
            this.remove();
            return;
        }
        if (WavifyWaveHandler.posIsWater(this.level, this.getBlockPos().offset(0, 1, 0))) {
            this.x -= this.xd * 8.0;
            this.z -= this.zd * 8.0;
            for (int i = 0; i < 5; ++i) {
                this.level.addParticle((ParticleOptions)WavifyParticles.SPLASH_PARTICLE, this.x + this.random.nextGaussian(), this.y + 1.0, this.z + this.random.nextGaussian(), this.random.nextGaussian() * (double)0.05f, Math.abs(this.level.getRandom().nextGaussian()) * (double)0.1f + Mth.clamp((double)this.intensity, (double)0.1, (double)0.3), this.random.nextGaussian() * (double)0.05f);
                this.level.addParticle((ParticleOptions)ParticleTypes.BUBBLE, this.x + this.random.nextGaussian() / 2.0, this.y + 1.0, this.z + this.random.nextGaussian() / 2.0, this.random.nextGaussian() / 8.0, 0.0, this.random.nextGaussian() / 8.0);
            }
            this.remove();
        }
        this.oRoll = this.roll;
        this.roll = this.yd != 0.0 && !this.onGround ? (this.roll += (float)this.yd * 35.0f) : 0.0f;
        this.setSpriteFromAge(this.spriteProvider);
    }

    public void extract(QuadParticleRenderState state, Camera camera, float tickDelta) {
        Quaternionf quaternionf = new Quaternionf();
        quaternionf.rotateX((float)Math.toRadians(-90.0));
        quaternionf.rotateZ((float)Math.toRadians(-90.0f - this.yaw));
        float angle = Mth.lerp((float)tickDelta, (float)this.oRoll, (float)this.roll);
        quaternionf.rotateX((float)Math.toRadians(angle));
        this.extractRotatedQuad(state, camera, quaternionf, tickDelta);
        quaternionf.rotateY((float)Math.toRadians(180.0));
        this.extractRotatedQuad(state, camera, quaternionf, tickDelta);
    }

    public void move(double dx, double dy, double dz) {
        if (!this.stopped) {
            double e = dy;
            if (this.hasPhysics && (dx != 0.0 || dy != 0.0 || dz != 0.0) && dx * dx + dy * dy + dz * dz < MAX_SQUARED_COLLISION_CHECK_DISTANCE) {
                Vec3 vec3d = Entity.collideBoundingBox(null, (Vec3)new Vec3(dx, dy, dz), (AABB)this.getBoundingBox().inflate(0.0, 0.15, 0.0), (Level)this.level, List.of());
                dx = vec3d.x;
                dy = vec3d.y;
                dz = vec3d.z;
            }
            if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
                this.setBoundingBox(this.getBoundingBox().move(dx, dy, dz));
                this.setLocationFromBoundingbox();
            }
            this.onGround = e != dy && e < 0.0;
        }
    }

    public BlockPos getBlockPos() {
        return BlockPos.containing((double)this.x, (double)this.y, (double)this.z);
    }

    public void updateWaterColor() {
        int color = BiomeColors.getAverageWaterColor((BlockAndTintGetter)this.level, (BlockPos)this.getBlockPos());
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        this.setColor(r, g, b);
    }

    protected boolean spawnWhite() {
        return true;
    }

    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Factory
    implements ParticleProvider<SprayParticleEffect> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(SprayParticleEffect params, ClientLevel level, double x, double y, double z, double velX, double velY, double velZ, RandomSource random) {
            return new SprayParticle(level, x, y, z, velX, velY, velZ, params, this.spriteProvider);
        }
    }
}

