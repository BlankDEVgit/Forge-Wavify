package net.superkat.wavify.duck;

import net.superkat.wavify.wave.WavifyWaveHandler;

/**
 * Duck interface implemented on {@code ClientLevel} via {@link net.superkat.wavify.mixin.ClientWorldMixin},
 * exposing the per-level wave handler. Unchanged from the Fabric version.
 */
public interface WavifyWorld {
    WavifyWaveHandler wavify$wavifyWaveHandler();
}
