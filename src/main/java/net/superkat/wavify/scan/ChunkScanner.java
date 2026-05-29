package net.superkat.wavify.scan;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.superkat.wavify.scan.ScannedChunk;
import net.superkat.wavify.scan.SitePos;
import net.superkat.wavify.scan.WaterHandler;
import net.superkat.wavify.wave.WavifyWaveHandler;
import com.google.common.collect.Lists;

public class ChunkScanner {
    public final WaterHandler handler;
    public final ClientLevel level;
    public Map<BlockPos, Boolean> cachedBlocks = new Object2ObjectOpenHashMap();
    public Set<BlockPos> visitedBlocks = new ObjectOpenHashSet();
    public Iterator<BlockPos> cachedIterator = null;
    public int shorelinesSinceSite = 0;
    public ChunkPos chunkPos;
    public ObjectOpenHashSet<BlockPos> waters = new ObjectOpenHashSet();
    public ObjectOpenHashSet<BlockPos> shorelines = new ObjectOpenHashSet();
    public ObjectOpenHashSet<SitePos> sites = new ObjectOpenHashSet();

    public ChunkScanner(WaterHandler handler, ClientLevel level, ChunkPos chunkPos) {
        this.handler = handler;
        this.level = level;
        this.chunkPos = chunkPos;
        BlockPos startPos = chunkPos.getWorldPosition();
        BlockPos endPos = startPos.offset(15, 0, 15);
        this.cachedIterator = this.stack(startPos, endPos);
    }

    public ScannedChunk scan() {
        BlockPos startPos = this.chunkPos.getWorldPosition();
        BlockPos endPos = startPos.offset(15, 0, 15);
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)startPos, (BlockPos)endPos)) {
            int y = this.sampleHeightmap(pos) - 1;
            this.scanPos(new BlockPos(pos.getX(), y, pos.getZ()));
        }
        return new ScannedChunk(this.chunkPos, (Set<BlockPos>)this.waters, (Set<BlockPos>)this.shorelines, (Set<SitePos>)this.sites);
    }

    private int sampleHeightmap(BlockPos pos) {
        return this.level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
    }

    public Iterator<BlockPos> stack(BlockPos startPos, BlockPos endPos) {
        this.cachedIterator = BlockPos.betweenClosed((BlockPos)startPos, (BlockPos)endPos).iterator();
        return this.cachedIterator;
    }

    public void scanPos(BlockPos pos) {
        if (this.visitedBlocks.contains(pos)) {
            return;
        }
        if (this.level.isEmptyBlock(pos)) {
            return;
        }
        boolean posIsWater = this.cacheAndIsWater(pos);
        this.visitedBlocks.add(pos);
        ArrayList nonWaterBlocks = Lists.newArrayList();
        ArrayList waterBlocks = Lists.newArrayList();
        if (posIsWater) {
            waterBlocks.add(pos);
        } else {
            nonWaterBlocks.add(pos);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos checkPos = pos.relative(direction);
            if (this.level.isEmptyBlock(checkPos) || direction == Direction.NORTH && pos.getZ() % 16 == 0 || direction == Direction.WEST && pos.getX() % 16 == 0 || direction == Direction.SOUTH && (pos.getZ() - 1) % 16 == 0 || direction == Direction.EAST && (pos.getX() + 1) % 16 == 0) continue;
            boolean neighborIsWater = this.cacheAndIsWater(checkPos);
            if (neighborIsWater) {
                waterBlocks.add(checkPos);
                continue;
            }
            nonWaterBlocks.add(checkPos);
        }
        if (waterBlocks.isEmpty() || !posIsWater) {
            return;
        }
        if (!nonWaterBlocks.isEmpty()) {
            this.shorelines.addAll((Collection)nonWaterBlocks);
            this.shorelinesSinceSite += nonWaterBlocks.size();
            if (this.shorelinesSinceSite >= 8) {
                this.sites.add(new SitePos(pos));
                this.shorelinesSinceSite = 0;
            }
        }
        this.waters.addAll((Collection)waterBlocks);
    }

    public boolean cacheAndIsWater(BlockPos pos) {
        return this.cachedBlocks.computeIfAbsent(pos, pos1 -> WavifyWaveHandler.posIsWater(this.level, pos1));
    }
}

