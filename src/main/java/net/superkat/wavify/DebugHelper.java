package net.superkat.wavify;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.wave.WavifyWaveHandler;
import org.joml.Vector3f;

public class DebugHelper {
    public static boolean debug() {
        return WavifyConfig.debug;
    }

    public static boolean usingSpyglass() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player.getUseItem().is(Items.SPYGLASS) && player.getUseItemRemainingTicks() >= 10) {
            if (player.getUseItemRemainingTicks() == 10) {
                player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
            }
            return true;
        }
        return false;
    }

    public static boolean spyglassInHotbar() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        Inventory playerInventory = player.getInventory();
        return Inventory.isHotbarSlot((int)playerInventory.findSlotMatchingItem(Items.SPYGLASS.getDefaultInstance()));
    }

    public static boolean holdingSpyglass() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        return player.getMainHandItem().is(Items.SPYGLASS);
    }

    public static boolean offhandSpyglass() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        return player.getOffhandItem().is(Items.SPYGLASS);
    }

    public static boolean clockInHotbar() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        Inventory playerInventory = player.getInventory();
        return Inventory.isHotbarSlot((int)playerInventory.findSlotMatchingItem(Items.CLOCK.getDefaultInstance()));
    }

    public static boolean offhandClock() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        return player.getOffhandItem().is(Items.CLOCK);
    }

    public static boolean usingShield() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player.getUseItem().is(Items.SHIELD) && (player.getUseItemRemainingTicks() == 1 || player.isShiftKeyDown())) {
            player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
            return true;
        }
        return false;
    }

    public static boolean holdingCompass() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        return player.getMainHandItem().is(Items.COMPASS);
    }

    public static boolean offhandCompass() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        return player.getOffhandItem().is(Items.COMPASS);
    }

    public static Vector3f debugColor(int i, int size) {
        if (i == 0) {
            return new Vector3f(1.0f, 1.0f, 1.0f);
        }
        if (i == 1) {
            return new Vector3f(1.0f, 0.0f, 0.0f);
        }
        if (i == 2) {
            return new Vector3f(0.0f, 1.0f, 0.0f);
        }
        if (i == 3) {
            return new Vector3f(0.0f, 0.0f, 1.0f);
        }
        int i1 = 255 - ((i -= 3) / 3 + 1) * 30 % 255;
        int i2 = 255 - (i / 3 + 30) * 30 % 255;
        int i3 = 255 - (i / 3 - 90) * 30 % 255;
        int red = i % 3 == 0 ? i1 : (i % 3 == 1 ? i2 : i3);
        int green = i % 3 == 1 ? i1 : (i % 3 == 2 ? i2 : i3);
        int blue = i % 3 == 2 ? i1 : (i % 3 == 0 ? i2 : i3);
        return new Vector3f(DebugHelper.checkColor((float)red / 255.0f), DebugHelper.checkColor((float)green / 255.0f), DebugHelper.checkColor((float)blue / 255.0f));
    }

    public static Vector3f debugTransitionColor(int i, int size) {
        return DebugHelper.debugTransitionColor(i, size, new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(0.0f, 0.0f, 0.0f));
    }

    public static Vector3f debugTransitionColor(int i, int size, Vector3f start, Vector3f end) {
        float delta = (float)i / (float)size;
        float red = Mth.lerp((float)delta, (float)start.x, (float)end.x);
        float green = Mth.lerp((float)delta, (float)start.y, (float)end.y);
        float blue = Mth.lerp((float)delta, (float)start.z, (float)end.z);
        return new Vector3f(DebugHelper.checkColor(red), DebugHelper.checkColor(green), DebugHelper.checkColor(blue));
    }

    public static Vector3f randomDebugColor() {
        RandomSource random = WavifyWaveHandler.getRandom();
        int rgbIncrease = random.nextIntBetweenInclusive(1, 3);
        int red = rgbIncrease == 1 ? random.nextIntBetweenInclusive(150, 255) : 255;
        int green = rgbIncrease == 2 ? random.nextIntBetweenInclusive(150, 255) : 255;
        int blue = rgbIncrease == 3 ? random.nextIntBetweenInclusive(150, 255) : 255;
        return new Vector3f((float)red / 255.0f, (float)green / 255.0f, (float)blue / 255.0f);
    }

    private static float checkColor(float color) {
        if (color > 1.0f) {
            return 1.0f;
        }
        return Math.max(color, 0.0f);
    }
}

