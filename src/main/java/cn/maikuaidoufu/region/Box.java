package cn.maikuaidoufu.region;

import net.minecraft.core.BlockPos;

/**
 * An inclusive, normalized block box.
 */
public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
{
    public Box
    {
        int x1 = Math.min(minX, maxX);
        int y1 = Math.min(minY, maxY);
        int z1 = Math.min(minZ, maxZ);
        int x2 = Math.max(minX, maxX);
        int y2 = Math.max(minY, maxY);
        int z2 = Math.max(minZ, maxZ);
        minX = x1;
        minY = y1;
        minZ = z1;
        maxX = x2;
        maxY = y2;
        maxZ = z2;
    }

    public static Box of(BlockPos a, BlockPos b)
    {
        return new Box(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    public boolean contains(BlockPos pos)
    {
        return pos.getX() >= this.minX && pos.getX() <= this.maxX
            && pos.getY() >= this.minY && pos.getY() <= this.maxY
            && pos.getZ() >= this.minZ && pos.getZ() <= this.maxZ;
    }

    public int sizeX()
    {
        return this.maxX - this.minX + 1;
    }

    public int sizeY()
    {
        return this.maxY - this.minY + 1;
    }

    public int sizeZ()
    {
        return this.maxZ - this.minZ + 1;
    }

    public boolean isValid()
    {
        return Math.abs(this.minX) <= RegionLimits.MAX_COORDINATE
            && Math.abs(this.maxX) <= RegionLimits.MAX_COORDINATE
            && Math.abs(this.minY) <= RegionLimits.MAX_COORDINATE
            && Math.abs(this.maxY) <= RegionLimits.MAX_COORDINATE
            && Math.abs(this.minZ) <= RegionLimits.MAX_COORDINATE
            && Math.abs(this.maxZ) <= RegionLimits.MAX_COORDINATE;
    }
}
