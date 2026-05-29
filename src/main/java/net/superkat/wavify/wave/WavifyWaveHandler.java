package net.superkat.wavify.wave;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.superkat.wavify.DebugHelper;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.particles.debug.DebugWaveMovementParticle;
import net.superkat.wavify.renderer.WaveRenderer;
import net.superkat.wavify.scan.SitePos;
import net.superkat.wavify.scan.WaterHandler;
import net.superkat.wavify.wave.Wave;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class WavifyWaveHandler {
    public final ClientLevel level;
    public WaterHandler waterHandler;
    public WaveRenderer renderer;
    public List<Wave> waves = new ObjectArrayList();
    public Set<BlockPos> coveredBlocks = new ObjectArraySet();
    public boolean nearbyChunksLoaded = false;

    public WavifyWaveHandler(ClientLevel level) {
        this.level = level;
        this.waterHandler = new WaterHandler(this, level);
        this.renderer = new WaveRenderer(this, level);
    }

    public void reloadNearbyChunks() {
        this.nearbyChunksLoaded = false;
    }

    public void tick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        assert (player != null);
        if (!this.nearbyChunksLoaded) {
            this.nearbyChunksLoaded = this.nearbyChunksLoaded(player);
        }
        this.waterHandler.tick();
        this.wavifyTick();
        if (DebugHelper.debug()) {
            this.debugTick(client, player);
        }
    }

    public void render(BufferBuilder buffer) {
        this.renderer.render(buffer);
    }

    public void wavifyTick() {
        if (!this.level.tickRateManager().runsNormally()) {
            return;
        }
        double time = this.level.getGameTime();
        if (time % 80.0 == 0.0) {
            this.spawnAllWaves();
        }
        boolean updateCoveredBlocks = time % 10.0 == 0.0;
        ObjectArraySet updatedCovered = new ObjectArraySet();
        Iterator<Wave> iterator = this.waves.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            wave.tick();
            if (wave.isDead()) {
                iterator.remove();
                continue;
            }
            if (!updateCoveredBlocks) continue;
            updatedCovered.addAll(wave.getCoveredBlocks());
        }
        if (updateCoveredBlocks) {
            this.coveredBlocks = updatedCovered;
        }
    }

    public void spawnAllWaves() {
        ChunkPos end;
        int distFromShore = WavifyConfig.waveDistFromShore;
        int chunkRadius = WavifyConfig.chunkRadius - 2;
        ChunkPos playerChunk = Minecraft.getInstance().player.chunkPosition();
        ChunkPos start = new ChunkPos(playerChunk.x() + chunkRadius, playerChunk.z() + chunkRadius);
        Set<BlockPos> waterBlocks = ChunkPos.rangeClosed((ChunkPos)start, (ChunkPos)(end = new ChunkPos(playerChunk.x() - chunkRadius, playerChunk.z() - chunkRadius))).map(chunkPos -> this.waterHandler.getWaterCacheAtDistance((ChunkPos)chunkPos, distFromShore)).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toSet());
        if (waterBlocks.isEmpty()) {
            return;
        }
        this.spawnWaves(waterBlocks);
        if (DebugHelper.debug()) {
            if (DebugHelper.holdingSpyglass()) {
                this.debugWaveParticles(waterBlocks);
            }
            if (DebugHelper.offhandClock()) {
                for (BlockPos water : waterBlocks) {
                    Vec3 pos = water.getCenter();
                    this.level.addParticle((ParticleOptions)ParticleTypes.END_ROD, pos.x(), pos.y() + 2.5, pos.z(), 0.0, 0.0, 0.0);
                }
            }
        }
    }

    public void spawnWaves(Set<BlockPos> waterBlocks) {
        HashSet visited = Sets.newHashSet();
        int spawned = 0;
        for (BlockPos water : waterBlocks) {
            SitePos site;
            if (visited.contains(water) || (site = this.waterHandler.getSiteForPos(water)) == null || !site.yawCalculated || site.xList.size() < 50) continue;
            float yaw = site.getYaw();
            Set<BlockPos> connected = this.findConnected(water, yaw, waterBlocks, visited);
            visited.addAll(connected);
            boolean bigWave = site.xList.size() >= 100;
            float yOffset = Mth.sin((double)(++spawned)) / 16.0f + 0.65f;
            BlockPos spawnPos = connected.stream().sorted(Comparator.comparingInt(Vec3i::getZ)).toList().get(connected.size() / 2).offset(0, 1, 0);
            BlockPos beneath = spawnPos.offset(0, -1, 0);
            if (this.level.isEmptyBlock(beneath) || !this.level.getBlockState(beneath).getFluidState().isSource()) continue;
            if (this.level.getBiome(spawnPos).is(BiomeTags.IS_RIVER)) {
                bigWave = false;
            }
            Wave wave = new Wave(this.level, spawnPos, yaw, yOffset, bigWave);
            int width = (int)Mth.clamp((double)((double)connected.size() * 1.5), (double)1.0, (double)3.0);
            wave.setWidth(width);
            this.waves.add(wave);
        }
    }

    public Set<BlockPos> findConnected(BlockPos start, float yaw, Set<BlockPos> waterBlocks, Set<BlockPos> ignoreSet) {
        int maxLength = 3;
        HashSet connected = Sets.newHashSet();
        ArrayDeque stack = Queues.newArrayDeque();
        stack.add(start);
        for (int i = 0; i < maxLength; ++i) {
            BlockPos water = (BlockPos)stack.poll();
            connected.add(water);
            for (BlockPos check : BlockPos.betweenClosed((BlockPos)water.offset(-1, 0, -1), (BlockPos)water.offset(1, 0, 1))) {
                SitePos site;
                if (water == check || ignoreSet.contains(check) || !waterBlocks.contains(check) || (site = this.waterHandler.getSiteForPos(check)) == null || !site.yawCalculated || site.xList.size() < 50 || Math.abs(site.yaw - yaw) > 15.0f) continue;
                stack.add(new BlockPos((Vec3i)check));
            }
            if (stack.isEmpty()) break;
        }
        return connected;
    }

    public void debugWaveParticles(Set<BlockPos> waterBlocks) {
        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
        boolean farParticles = false;
        for (BlockPos water : waterBlocks) {
            SitePos site = this.waterHandler.getSiteForPos(water);
            if (site == null || !site.yawCalculated) continue;
            DebugWaveMovementParticle.DebugWaveMovementParticleEffect particleEffect = new DebugWaveMovementParticle.DebugWaveMovementParticleEffect((Vector3fc)color, 1.0f, site.getYaw(), 0.3f, 20);
            this.level.addParticle((ParticleOptions)particleEffect, farParticles, false, (double)water.getX(), (double)(water.getY() + 2), (double)water.getZ(), 0.0, 0.0, 0.0);
        }
    }

    public List<Wave> getWaves() {
        return this.waves;
    }

    public boolean nearbyChunksLoaded(LocalPlayer player) {
        if (this.nearbyChunksLoaded) {
            return true;
        }
        int chunkRadius = this.getChunkRadius();
        int chunkX = player.chunkPosition().x();
        int chunkZ = player.chunkPosition().z();
        int chunkRadiusReduced = chunkRadius - chunkRadius / 3;
        List<LevelChunk> checkChunks = List.of(this.level.getChunk(chunkX + chunkRadius, chunkZ), this.level.getChunk(chunkX - chunkRadius, chunkZ), this.level.getChunk(chunkX, chunkZ + chunkRadius), this.level.getChunk(chunkX, chunkZ - chunkRadius), this.level.getChunk(chunkX + chunkRadiusReduced, chunkZ + chunkRadiusReduced), this.level.getChunk(chunkX - chunkRadiusReduced, chunkZ + chunkRadiusReduced), this.level.getChunk(chunkX - chunkRadiusReduced, chunkZ - chunkRadiusReduced), this.level.getChunk(chunkX + chunkRadiusReduced, chunkZ - chunkRadiusReduced));
        return checkChunks.stream().noneMatch(LevelChunk::isEmpty);
    }

    public Set<ChunkPos> getNearbyChunkPos() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ChunkPos playerPos = player.chunkPosition();
        int playerX = playerPos.x();
        int playerZ = playerPos.z();
        int radius = this.getLoadedChunkRadius();
        ChunkPos start = new ChunkPos(playerX + radius, playerZ + radius);
        ChunkPos end = new ChunkPos(playerX - radius, playerZ - radius);
        HashSet loadedChunks = Sets.newHashSet();
        for (ChunkPos chunkPos : ChunkPos.rangeClosed((ChunkPos)start, (ChunkPos)end).toList()) {
            LevelChunk chunk = this.level.getChunk(chunkPos.x(), chunkPos.z());
            if (chunk.isEmpty()) continue;
            loadedChunks.add(chunkPos);
        }
        return loadedChunks;
    }

    public int getChunkRadius() {
        Minecraft client = Minecraft.getInstance();
        int configRadius = WavifyConfig.chunkRadius;
        int serverRadius = client.options.serverRenderDistance;
        return Math.min(configRadius, serverRadius);
    }

    public int getLoadedChunkRadius() {
        Minecraft client = Minecraft.getInstance();
        int loadRadius = client.options.serverRenderDistance;
        return Math.max(2, loadRadius) + 3;
    }

    public void debugTick(Minecraft client, LocalPlayer player) {
        if (DebugHelper.holdingCompass() || DebugHelper.offhandCompass()) {
            if (!this.waterHandler.built) {
                return;
            }
            ChunkPos playerChunk = player.chunkPosition();
            if (DebugHelper.offhandCompass()) {
                if (client.level.getGameTime() % 5L != 0L) {
                    return;
                }
                int radius = 2;
                ChunkPos start = new ChunkPos(playerChunk.x() + radius, playerChunk.z() + radius);
                ChunkPos end = new ChunkPos(playerChunk.x() - radius, playerChunk.z() - radius);
                for (ChunkPos chunkPos : ChunkPos.rangeClosed((ChunkPos)start, (ChunkPos)end).toList()) {
                    this.debugChunkDirectionParticles(chunkPos.pack(), true);
                }
            } else {
                this.debugChunkDirectionParticles(playerChunk.pack(), false);
            }
        }
        if (DebugHelper.usingSpyglass()) {
            if (client.level.getGameTime() % 20L != 0L) {
                return;
            }
            BlockPos playerPos = player.blockPosition();
            List scannedBlocks = this.waterHandler.waterCache.values().stream().flatMap(map -> map.keySet().stream()).toList();
            if (scannedBlocks.contains(playerPos)) {
                long chunkPosL = ChunkPos.pack((BlockPos)playerPos);
                SitePos site = this.waterHandler.waterCache.get(chunkPosL).get(playerPos);
                System.out.println(site.xList.size());
            }
        }
    }

    public void debugChunkDirectionParticles(long chunkPosL, boolean farParticles) {
        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);
        Map<BlockPos, SitePos> map = this.waterHandler.waterCache.get(chunkPosL);
        if (map == null) {
            return;
        }
        for (Map.Entry<BlockPos, SitePos> entry : map.entrySet()) {
            BlockPos pos = entry.getKey();
            SitePos sitePos = entry.getValue();
            if (sitePos == null || !sitePos.yawCalculated) continue;
            DebugWaveMovementParticle.DebugWaveMovementParticleEffect particleEffect = new DebugWaveMovementParticle.DebugWaveMovementParticleEffect((Vector3fc)color, 1.0f, sitePos.getYaw(), 0.3f, 20);
            this.level.addParticle((ParticleOptions)particleEffect, farParticles, false, (double)pos.getX(), (double)(pos.getY() + 2), (double)pos.getZ(), 0.0, 0.0, 0.0);
        }
    }

    public static RandomSource getRandom() {
        return RandomSource.create();
    }

    public static RandomSource getSyncedRandom() {
        long time = Minecraft.getInstance().level.getGameTime();
        long random = 5L * (long)Math.round((float)time / 5.0f);
        return RandomSource.create((long)random);
    }

    public static boolean posIsWater(ClientLevel level, BlockPos pos) {
        FluidState state = level.getFluidState(pos);
        return state.is(FluidTags.WATER);
    }

    public static boolean stateIsWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }
}

