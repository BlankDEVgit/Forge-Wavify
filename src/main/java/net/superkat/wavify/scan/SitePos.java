package net.superkat.wavify.scan;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;

public class SitePos {
    public BlockPos pos;
    public int centerX = 0;
    public int centerZ = 0;
    public float yaw = 0.0f;
    public boolean yawCalculated = false;
    public IntArrayList xList = new IntArrayList();
    public IntArrayList zList = new IntArrayList();

    public SitePos(BlockPos pos) {
        this.pos = pos;
    }

    public void addPos(BlockPos pos) {
        this.xList.add(pos.getX());
        this.zList.add(pos.getZ());
    }

    public void removePos(BlockPos pos) {
        int xIndex = this.xList.indexOf(pos.getX());
        this.xList.removeInt(xIndex);
        int zIndex = this.zList.indexOf(pos.getZ());
        this.zList.removeInt(zIndex);
    }

    public void clearPositions() {
        this.xList.clear();
        this.zList.clear();
        this.yawCalculated = false;
    }

    public void updateCenter() {
        if (this.xList.isEmpty() || this.zList.isEmpty()) {
            return;
        }
        int xSize = this.xList.size();
        this.centerX = this.xList.intStream().sum() / xSize;
        int zSize = this.zList.size();
        this.centerZ = this.zList.intStream().sum() / zSize;
        this.updateYaw();
    }

    public void updateYaw() {
        this.yawCalculated = true;
        this.yaw = (float)Math.toDegrees(Math.atan2(this.pos.getZ() - this.centerZ, this.pos.getX() - this.centerX));
        this.yaw = (float)Math.round(this.yaw / 15.0f) * 15.0f;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getYawAsF3Angle() {
        float angle = this.getYaw() - 90.0f;
        if (angle < 0.0f) {
            angle += 360.0f;
        }
        if (angle > 180.0f) {
            angle -= 360.0f;
        }
        return angle;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getX() {
        return this.pos.getX();
    }

    public int getY() {
        return this.pos.getY();
    }

    public int getZ() {
        return this.pos.getZ();
    }
}

