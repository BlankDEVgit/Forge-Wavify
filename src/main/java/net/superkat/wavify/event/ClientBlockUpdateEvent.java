package net.superkat.wavify.event;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lightweight client-side block-update callback.
 *
 * <p>The Fabric original used {@code net.fabricmc.fabric.api.event.Event} /
 * {@code EventFactory.createArrayBacked}. This is an equivalent self-contained array-backed callback
 * so the mod no longer depends on the Fabric event API. It is fired from
 * {@link net.superkat.wavify.mixin.ClientWorldMixin} on every {@code ClientLevel#setBlock}.
 */
public class ClientBlockUpdateEvent {
    private static final List<BlockUpdate> LISTENERS = new ArrayList<>();

    public static void register(BlockUpdate listener) {
        LISTENERS.add(listener);
    }

    public static void invoke(BlockPos pos, BlockState state) {
        for (BlockUpdate listener : LISTENERS) {
            listener.onUpdate(pos, state);
        }
    }

    @FunctionalInterface
    public interface BlockUpdate {
        void onUpdate(BlockPos pos, BlockState state);
    }
}
