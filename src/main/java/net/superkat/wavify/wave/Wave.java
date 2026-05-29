package net.superkat.wavify.wave;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.superkat.wavify.WavifyParticles;
import net.superkat.wavify.particles.SprayParticleEffect;
import net.superkat.wavify.wave.WavifyWaveHandler;
import org.jetbrains.annotations.Range;

public class Wave {
    private static final double MAX_SQUARED_COLLISION_CHECK_DISTANCE = Mth.square((double)100.0);
    public ClientLevel level;
    public BlockPos spawnPos;
    public float yaw;
    public boolean bigWave;
    public AABB box;
    public float scale;
    public float width;
    public float length;
    public float pitch = 0.0f;
    public float x;
    public float y;
    public float z;
    public float prevX;
    public float prevY;
    public float prevZ;
    public float velX;
    public float velY;
    public float velZ;
    public int age;
    public int maxAge;
    public boolean dead = false;
    public int maxWashingAge = 60;
    public int maxWaterAge = 100;
    public boolean drowningAway = false;
    public int ageUponWhichThisWaveHasOfficiallyJoinedEthoInBecomingWashedUp;
    public BlockState beneathBlock = Blocks.WATER.defaultBlockState();
    public boolean aboveWater = true;
    public boolean washingUp = false;
    public boolean hitBlock = false;
    public int hitBlockAge;
    public boolean ending = false;
    public boolean waterfallMode = false;
    public boolean waterfallSplashed = false;
    public float red = 1.0f;
    public float green = 1.0f;
    public float blue = 1.0f;
    public float alpha = 1.0f;

    public Wave(ClientLevel level, BlockPos spawnPos, float yaw, float yOffset, boolean bigWave) {
        this.level = level;
        this.spawnPos = spawnPos;
        this.yaw = yaw;
        this.bigWave = bigWave;
        if (this.bigWave) {
            this.scale = 3.0f;
            this.length = 1.5f;
            this.width = 1.0f;
            this.maxAge = 300;
        } else {
            this.scale = 2.0f;
            this.length = 1.0f;
            this.width = 2.0f;
            this.maxAge = 250;
        }
        this.x = (float)spawnPos.getX() + 0.5f;
        this.y = (float)spawnPos.getY() + Math.abs(yOffset) - 0.05f;
        this.z = (float)spawnPos.getZ() + 0.5f;
        float f = 0.1f;
        float g = 0.2f;
        this.box = new AABB((double)this.x - (double)f, (double)this.y, (double)this.z - (double)f, (double)this.x + (double)f, (double)this.y + (double)g, (double)this.z + (double)f).inflate((double)(this.scale / 4.0f), 0.0, (double)(this.scale / 4.0f));
        float speed = 0.115f;
        this.velX = (float)(Math.cos(Math.toRadians(yaw)) * (double)speed);
        this.velZ = (float)(Math.sin(Math.toRadians(yaw)) * (double)speed);
        this.alpha = 0.0f;
    }

    public int getWashingAge() {
        return this.age - this.ageUponWhichThisWaveHasOfficiallyJoinedEthoInBecomingWashedUp;
    }

    public BlockPos getBlockPos() {
        return BlockPos.containing((double)this.x, (double)this.y, (double)this.z);
    }

    public Set<BlockPos> getCoveredBlocks() {
        HashSet set = Sets.newHashSet();
        BlockPos currentPos = this.getBlockPos();
        int extra = 0;
        if (this.bigWave && this.getWashingAge() >= 13) {
            extra = this.getWashingAge() <= 40 ? 3 : 1;
        }
        int usedWidth = (int)(this.width - (float)(!this.bigWave ? 1 : 0)) + extra;
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)currentPos.offset(-usedWidth, -1, -usedWidth), (BlockPos)currentPos.offset(usedWidth, -1, usedWidth))) {
            if (WavifyWaveHandler.posIsWater(this.level, pos) || this.level.isEmptyBlock(pos)) continue;
            set.add(new BlockPos((Vec3i)pos));
        }
        return set;
    }

    public void tick() {
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        if (this.updateWashingUp()) {
            if (this.getWashingAge() <= 10) {
                this.velX *= 0.875f;
                this.velY = -5.0E-4f;
                this.velZ *= 0.875f;
            } else if (this.washBounce()) {
                this.velX *= 1.2f;
                this.velZ *= 1.2f;
            } else {
                this.velX *= 0.9f;
                this.velZ *= 0.9f;
            }
            this.ending = Math.abs(this.velX) <= 0.03f && Math.abs(this.velZ) <= 0.3f;
            float addedLength = Math.abs(this.velX) * (this.bigWave ? 1.0f : 0.75f);
            this.length += addedLength;
            if (this.getWashingAge() >= this.maxWashingAge) {
                this.markDead();
            }
        } else {
            this.updateWaterColor();
            if (this.drowningAway) {
                this.length -= 0.1f;
                this.velY -= 0.005f;
                if (this.length <= 0.0f) {
                    this.markDead();
                }
            }
            if (this.alpha < 1.0f) {
                this.alpha += 0.05f;
            }
        }
        if (this.hitBlock && this.age - this.hitBlockAge >= 2) {
            this.markDead();
        }
        this.move(this.velX, this.velY, this.velZ);
        this.updateBeneathBlock();
    }

    public void move(float velX, float velY, float velZ) {
        float initVelX = velX;
        float initVelZ = velZ;
        if (((double)velX != 0.0 || (double)velY != 0.0 || (double)velZ != 0.0) && (double)(velX * velX + velY * velY + velZ * velZ) < MAX_SQUARED_COLLISION_CHECK_DISTANCE) {
            Vec3 vec3d = Entity.collideBoundingBox(null, (Vec3)new Vec3((double)velX, (double)velY, (double)velZ), (AABB)this.getHitBox(), (Level)this.level, List.of());
            velX = (float)vec3d.x;
            velY = (float)vec3d.y;
            velZ = (float)vec3d.z;
        }
        if (initVelX != velX || initVelZ != velZ) {
            this.spray();
        }
        if ((double)velX != 0.0 || (double)velY != 0.0 || (double)velZ != 0.0) {
            this.box = this.box.move((double)velX, (double)velY, (double)velZ);
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            this.x = (float)(this.box.minX + this.box.maxX) / 2.0f;
            this.y = (float)this.box.minY;
            this.z = (float)(this.box.minZ + this.box.maxZ) / 2.0f;
        }
    }

    public void spray() {
        if (this.hitBlock) {
            return;
        }
        if (!this.drowningAway) {
            float sprayIntensity;
            int sprayAmount;
            int n = sprayAmount = this.bigWave ? 3 : 1;
            if (this.isWashingUp()) {
                sprayIntensity = (float)this.getWashingAge() / 128.0f;
                if (this.washBounce()) {
                    sprayIntensity *= 2.0f;
                }
            } else {
                sprayIntensity = (float)this.age / (float)this.maxAge * 2.5f / ((float)this.age / 16.0f);
            }
            double splashX = this.x + this.velX * 10.0f;
            double splashZ = this.z + this.velZ * 10.0f;
            for (int i = 0; i < sprayAmount; ++i) {
                this.level.addParticle((ParticleOptions)WavifyParticles.SPLASH_PARTICLE, splashX, (double)this.y, splashZ, this.level.getRandom().nextGaussian() * (double)0.1f, Math.abs(this.level.getRandom().nextGaussian()) * (double)0.1f + (double)0.1f, this.level.getRandom().nextGaussian() * (double)0.1f);
                if (!this.bigWave) continue;
                this.level.addParticle((ParticleOptions)WavifyParticles.BIG_SPLASH_PARTICLE, splashX + this.level.getRandom().nextGaussian() / 2.0, (double)this.y, splashZ + this.level.getRandom().nextGaussian() / 2.0, 0.0, 0.01, 0.0);
            }
            this.level.addParticle((ParticleOptions)new SprayParticleEffect(this.yaw - 180.0f, sprayIntensity, this.scale), splashX, (double)(this.y - 0.05f), splashZ, (double)(-this.velX), 0.0, (double)(-this.velZ));
            this.velX = 0.0f;
            this.velY = 0.0f;
            this.velZ = 0.0f;
        }
        this.hitBlockAge = this.age;
        this.hitBlock = true;
    }

    public boolean updateWashingUp() {
        if (!(this.washingUp || this.aboveWater || this.drowningAway)) {
            if (this.beneathBlock.isAir()) {
                this.waterfallMode = true;
                this.velY = Mth.clamp((float)(this.velY - 0.01f), (float)-1.5f, (float)0.0f);
                this.pitch += 1.0f + Math.abs(this.velY) * 5.0f;
            } else {
                this.velY = 0.0f;
                this.pitch = 0.0f;
                this.washingUp = true;
                this.ageUponWhichThisWaveHasOfficiallyJoinedEthoInBecomingWashedUp = this.age;
            }
        }
        if (this.waterfallMode && this.aboveWater && !this.waterfallSplashed && WavifyWaveHandler.posIsWater(this.level, this.getBlockPos())) {
            this.waterfallSplashed = true;
            int splashAmount = this.bigWave ? 7 : 3;
            float splashIntensity = this.bigWave ? 0.2f : 0.1f;
            double splashX = this.x + this.velX * 3.0f;
            double splashZ = this.z + this.velZ * 3.0f;
            int i = 0;
            while ((float)i < this.width) {
                for (int j = 0; j < splashAmount; ++j) {
                    this.level.addParticle((ParticleOptions)WavifyParticles.SPLASH_PARTICLE, splashX + this.level.getRandom().nextGaussian(), (double)this.y, splashZ + this.level.getRandom().nextGaussian(), this.level.getRandom().nextGaussian() * (double)splashIntensity, Math.abs(this.level.getRandom().nextGaussian()) * (double)splashIntensity + (double)splashIntensity, this.level.getRandom().nextGaussian() * (double)splashIntensity);
                }
                ++i;
            }
            for (i = 0; i < splashAmount; ++i) {
            }
        }
        if (!this.drowningAway && !this.washingUp && this.age >= this.maxWaterAge) {
            this.drowningAway = true;
        }
        return this.washingUp;
    }

    public boolean isWashingUp() {
        return this.washingUp;
    }

    private boolean washBounce() {
        return this.getWashingAge() >= 12 && this.getWashingAge() <= 17;
    }

    public void updateBeneathBlock() {
        this.beneathBlock = this.level.getBlockState(this.getBlockPos().offset(0, -1, 0));
        this.aboveWater = WavifyWaveHandler.stateIsWater(this.beneathBlock);
    }

    public AABB getBoundingBox() {
        return this.box.inflate(0.5);
    }

    public AABB getHitBox() {
        if (this.isWashingUp()) {
            float yawRadians = (float)Math.toRadians(this.yaw);
            float usedLength = this.bigWave ? this.length * 1.5f : this.length / 16.0f;
            return this.getBoundingBox().expandTowards((double)usedLength * Math.cos(yawRadians), 0.0, (double)usedLength * Math.sin(yawRadians));
        }
        return this.getBoundingBox();
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void updateWaterColor() {
        int color = BiomeColors.getAverageWaterColor((BlockAndTintGetter)this.level, (BlockPos)this.getBlockPos());
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        this.setColor(r, g, b);
    }

    public void setColor(@Range(from=0L, to=1L) float red, @Range(from=0L, to=1L) float green, @Range(from=0L, to=1L) float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public float getX(float delta) {
        return Mth.lerp((float)delta, (float)this.prevX, (float)this.x);
    }

    public float getY(float delta) {
        return Mth.lerp((float)delta, (float)this.prevY, (float)this.y);
    }

    public float getZ(float delta) {
        return Mth.lerp((float)delta, (float)this.prevZ, (float)this.z);
    }

    public int getAge() {
        return this.isWashingUp() ? this.getWashingAge() : this.age;
    }

    public int getMaxAge() {
        return this.isWashingUp() ? this.maxWashingAge : this.maxAge;
    }

    public int getLight() {
        long dayTime = this.level.getGameTime();
        if ((int)(dayTime / 24000L % 8L) == 0 && dayTime % 24000L >= 12000L) {
            return LightCoordsUtil.pack((int)15, (int)15);
        }
        BlockPos pos = this.getBlockPos().offset(0, 1, 0);
        int blockLight = this.level.getBrightness(LightLayer.BLOCK, pos);
        int skylight = this.level.getBrightness(LightLayer.SKY, pos);
        return LightCoordsUtil.pack((int)blockLight, (int)skylight);
    }

    public void markDead() {
        this.dead = true;
    }

    public boolean isDead() {
        return this.dead;
    }
}

