package net.superkat.wavify.scan;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.superkat.wavify.scan.SitePos;

public class ScannedChunk {
    public final long chunkPos;
    public Set<BlockPos> waters;
    public Set<BlockPos> shorelines;
    public Set<SitePos> sites;

    public ScannedChunk(ChunkPos chunkPos, Set<BlockPos> waters, Set<BlockPos> shorelines, Set<SitePos> sites) {
        this.chunkPos = chunkPos.pack();
        this.waters = waters;
        this.shorelines = shorelines;
        this.sites = sites;
    }
}

