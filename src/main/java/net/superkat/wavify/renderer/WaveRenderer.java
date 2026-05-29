package net.superkat.wavify.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.superkat.wavify.WavifyClient;
import net.superkat.wavify.sprite.WavifySpriteHandler;
import net.superkat.wavify.sprite.WavifySprites;
import net.superkat.wavify.wave.Wave;
import net.superkat.wavify.wave.WavifyWaveHandler;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

public class WaveRenderer {
    public WavifyWaveHandler handler;
    public WavifySpriteHandler spriteHandler;
    public ClientLevel level;

    public WaveRenderer(WavifyWaveHandler handler, ClientLevel level) {
        this.handler = handler;
        this.spriteHandler = WavifyClient.WAVIFY_SPRITE_HANDLER;
        this.level = level;
    }

    public void render(BufferBuilder buffer) {
        List<Wave> waves = this.handler.getWaves();
        if (waves == null || waves.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Camera camera = mc.gameRenderer.getMainCamera();
        for (Wave wave : waves) {
            this.renderWave(buffer, camera, wave, tickDelta);
        }
        this.renderOverlays(buffer, camera, this.handler.coveredBlocks);
    }

    public void renderWave(BufferBuilder buffer, Camera camera, Wave wave, float delta) {
        if (wave == null) {
            return;
        }
        PoseStack matrices = new PoseStack();
        matrices.pushPose();
        AABB box = wave.getBoundingBox();
        Vec3 center = new Vec3((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0);
        Vec3 cameraPos = camera.position();
        Vec3 transPos = center.subtract(cameraPos);
        matrices.pushPose();
        matrices.translate(transPos.x, transPos.y, transPos.z);
        matrices.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-wave.yaw + 90.0f));
        matrices.mulPose((Quaternionfc)Axis.XP.rotationDegrees(wave.pitch));
        float scale = wave.scale;
        matrices.scale(scale, 1.0f, scale);
        matrices.translate(-wave.width / 3.0f, 0.0f, 0.0f);
        Matrix4f posMatrix = matrices.last().pose();
        boolean washingUp = wave.isWashingUp();
        TextureAtlasSprite colorableSprite = washingUp ? this.getTopWashingSprite() : this.getMovingSprite();
        TextureAtlasSprite whiteSprite = washingUp ? this.getTopWashingWhiteSprite() : this.getMovingWhiteSprite();
        int light = wave.getLight();
        float red = wave.red;
        float green = wave.green;
        float blue = wave.blue;
        float alpha = wave.alpha;
        int age = wave.getAge();
        int maxAge = wave.getMaxAge();
        int i = 0;
        while ((float)i < wave.width) {
            this.waveQuad(posMatrix, buffer, colorableSprite, age, maxAge, i, 0.0f, 0.0f, 1.0f, wave.length, red, green, blue, alpha, light);
            this.waveQuad(posMatrix, buffer, whiteSprite, age, maxAge, i, 0.05f, 0.0f, 1.0f, wave.length, 1.0f, 1.0f, 1.0f, alpha, light);
            ++i;
        }
        if (washingUp && wave.bigWave) {
            TextureAtlasSprite washingColorableSprite = this.getBottomWashingSprite();
            TextureAtlasSprite washingWhiteSprite = this.getBottomWashingWhiteSprite();
            float ageDelta = (float)age / (float)maxAge;
            float turnBackDelta = 0.5f;
            float washingLength = ageDelta > turnBackDelta ? Mth.lerp((float)((ageDelta - turnBackDelta) * 2.0f), (float)2.0f, (float)3.0f) : 2.0f;
            float washingZ = ageDelta > turnBackDelta ? Mth.lerp((float)((ageDelta - turnBackDelta) * 2.0f), (float)1.35f, (float)0.0f) : 1.35f;
            matrices.scale(1.25f, 1.0f, 1.0f);
            int i2 = 0;
            while ((float)i2 < wave.width) {
                this.waveQuad(posMatrix, buffer, washingColorableSprite, age, maxAge, (float)i2 - 0.15f, -0.05f, washingZ, 1.0f, washingLength, red, green, blue, alpha, light);
                this.waveQuad(posMatrix, buffer, washingWhiteSprite, age, maxAge, (float)i2 - 0.15f, -0.01f, washingZ, 1.0f, washingLength, 1.0f, 1.0f, 1.0f, alpha, light);
                ++i2;
            }
        }
        matrices.popPose();
    }

    private void waveQuad(Matrix4f matrix4f, BufferBuilder buffer, TextureAtlasSprite sprite, int waveAge, int waveMaxAge, float x, float y, float z, float width, float length, float red, float green, float blue, float alpha, int light) {
        float halfWidth = width / 2.0f;
        float halfLength = length / 2.0f;
        int frame = WavifySprites.getFrameFromAge(sprite, waveAge, waveMaxAge);
        float u0 = WavifySprites.getU0(sprite);
        float u1 = WavifySprites.getU1(sprite);
        float v0 = WavifySprites.getV0(sprite, frame);
        float v1 = WavifySprites.getV1(sprite, frame);
        buffer.addVertex((Matrix4fc)matrix4f, x - halfWidth, y, z - halfLength).setColor(red, green, blue, alpha).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
        buffer.addVertex((Matrix4fc)matrix4f, x - halfWidth, y, z + halfLength).setColor(red, green, blue, alpha).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
        buffer.addVertex((Matrix4fc)matrix4f, x + halfWidth, y, z + halfLength).setColor(red, green, blue, alpha).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
        buffer.addVertex((Matrix4fc)matrix4f, x + halfWidth, y, z - halfLength).setColor(red, green, blue, alpha).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
    }

    public void renderOverlays(BufferBuilder buffer, Camera camera, Set<BlockPos> coveredBlocks) {
        for (BlockPos covered : coveredBlocks) {
            this.renderCoverOverlay(buffer, camera, covered);
        }
    }

    public void renderCoverOverlay(BufferBuilder buffer, Camera camera, BlockPos pos) {
        PoseStack matrices = new PoseStack();
        Vec3 cameraPos = camera.position();
        Vec3 transPos = pos.getBottomCenter().subtract(cameraPos);
        TextureAtlasSprite sprite = this.getWetOverlaySprite();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        int light = LightCoordsUtil.pack((int)0, (int)0);
        matrices.pushPose();
        matrices.translate(transPos.x - 0.5, transPos.y + 1.01, transPos.z - 0.5);
        Matrix4f matrix4f = matrices.last().pose();
        buffer.addVertex((Matrix4fc)matrix4f, 0.0f, 0.0f, 0.0f).setColor(0.1f, 0.1f, 0.25f, 0.25f).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
        buffer.addVertex((Matrix4fc)matrix4f, 0.0f, 0.0f, 1.0f).setColor(0.1f, 0.1f, 0.25f, 0.25f).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
        buffer.addVertex((Matrix4fc)matrix4f, 1.0f, 0.0f, 1.0f).setColor(0.1f, 0.1f, 0.25f, 0.25f).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
        buffer.addVertex((Matrix4fc)matrix4f, 1.0f, 0.0f, 0.0f).setColor(0.1f, 0.1f, 0.25f, 0.25f).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0.0f, 1.0f, 0.0f);
        matrices.popPose();
    }

    public TextureAtlasSprite getMovingSprite() {
        return this.spriteHandler.getSprite(WavifySprites.MOVING_TEXTURE_ID);
    }

    public TextureAtlasSprite getMovingWhiteSprite() {
        return this.spriteHandler.getSprite(WavifySprites.MOVING_WHITE_TEXTURE_ID);
    }

    public TextureAtlasSprite getTopWashingSprite() {
        return this.spriteHandler.getSprite(WavifySprites.TOP_WASHING_ID);
    }

    public TextureAtlasSprite getTopWashingWhiteSprite() {
        return this.spriteHandler.getSprite(WavifySprites.TOP_WASHING_WHITE_ID);
    }

    public TextureAtlasSprite getBottomWashingSprite() {
        return this.spriteHandler.getSprite(WavifySprites.BOTTOM_WASHING_ID);
    }

    public TextureAtlasSprite getBottomWashingWhiteSprite() {
        return this.spriteHandler.getSprite(WavifySprites.BOTTOM_WASHING_WHITE_ID);
    }

    public TextureAtlasSprite getWashedSprite() {
        return this.spriteHandler.getSprite(WavifySprites.WASHING_TEXTURE_ID);
    }

    public TextureAtlasSprite getWashedWhiteSprite() {
        return this.spriteHandler.getSprite(WavifySprites.WASHING_WHITE_TEXTURE_ID);
    }

    public TextureAtlasSprite getWetOverlaySprite() {
        return this.spriteHandler.getSprite(WavifySprites.WET_OVERLAY_TEXTURE_ID);
    }
}

