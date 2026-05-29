package net.superkat.wavify.sprite;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Builds and uploads the wave texture atlas on resource reload.
 *
 * <p>The Fabric original implemented {@code SimpleResourceReloadListener<SpriteLoader.Preparations>}
 * (a Fabric interface that splits load/apply across executors). Here we implement vanilla
 * {@link PreparableReloadListener} directly and replicate that split in {@link #reload}. The async
 * sprite-stitching logic ({@link #load}/{@link #apply}) is unchanged.
 */
public class WavifySpriteHandler implements PreparableReloadListener {
    public static final String MOD_ID = "wavify";
    public static final Identifier WAVE_ATLAS_ID = Identifier.fromNamespaceAndPath("wavify", "textures/atlas/waves.png");
    private static final Identifier TEXTURE_SOURCE_PATH = Identifier.fromNamespaceAndPath("wavify", "wave");
    public static final Set<MetadataSectionType<?>> METADATA_READERS = Set.of(WaveResourceMetadata.SERIALIZER);
    public TextureAtlas atlas;

    public TextureAtlasSprite getSprite(Identifier id) {
        return this.atlas.getSprite(id);
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.SharedState state, Executor backgroundExecutor,
                                          PreparableReloadListener.PreparationBarrier barrier, Executor gameExecutor) {
        // Forge 26.1 reload signature: the ResourceManager is reached via SharedState, and the
        // background executor now precedes the barrier.
        ResourceManager manager = state.resourceManager();
        return this.load(manager, backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenCompose(preparations -> this.apply(preparations, manager, gameExecutor));
    }

    public CompletableFuture<SpriteLoader.Preparations> load(ResourceManager manager, Executor executor) {
        if (this.atlas == null) {
            this.atlas = new TextureAtlas(WAVE_ATLAS_ID);
            Minecraft.getInstance().getTextureManager().register(this.atlas.location(), this.atlas);
        }
        return SpriteLoader.create(this.atlas).loadAndStitch(manager, TEXTURE_SOURCE_PATH, 0, executor, METADATA_READERS);
    }

    public CompletableFuture<Void> apply(SpriteLoader.Preparations stitchResult, ResourceManager manager, Executor executor) {
        return CompletableFuture.runAsync(() -> this.atlas.upload(stitchResult), executor);
    }

    public void clearAtlas() {
        this.atlas.clearTextureData();
    }
}
