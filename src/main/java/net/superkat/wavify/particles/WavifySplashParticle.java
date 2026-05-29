package net.superkat.wavify.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.WaterDropParticle;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class WavifySplashParticle
extends WaterDropParticle {
    public WavifySplashParticle(ClientLevel clientWorld, double x, double y, double z, double velX, double velY, double velZ, SpriteSet spriteProvider) {
        super(clientWorld, x, y, z, spriteProvider.first());
        this.gravity = 0.04f;
        this.xd = velX;
        this.yd = velY;
        this.zd = velZ;
        if (this.random.nextBoolean()) {
            this.updateWaterColor();
        }
    }

    public void updateWaterColor() {
        int color = BiomeColors.getAverageWaterColor((BlockAndTintGetter)this.level, (BlockPos)this.getBlockPos());
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        this.setColor(r, g, b);
    }

    public BlockPos getBlockPos() {
        return BlockPos.containing((double)this.x, (double)this.y, (double)this.z);
    }

    public static class Factory
    implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteProvider;

        public Factory(SpriteSet spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientWorld, double d, double e, double f, double g, double h, double i, RandomSource random) {
            return new WavifySplashParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
        }
    }
}

