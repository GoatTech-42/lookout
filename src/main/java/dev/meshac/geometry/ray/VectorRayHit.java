package dev.meshac.geometry.ray;

import dev.meshac.geometry.direction.Direction;
import dev.meshac.geometry.position.BlockPosition;
import dev.meshac.geometry.vector.Vector3D;
import net.minecraft.world.entity.Entity;

public class VectorRayHit {

    private BlockPosition blockPosition;

    public HitType hitType;

    public Direction direction;

    public Vector3D position;

    public Entity entity;

    public VectorRayHit(Vector3D position, Direction direction, BlockPosition blockPosition) {
        this(HitType.BLOCK, position, direction, blockPosition);
    }

    public VectorRayHit(Vector3D position, Direction direction) {
        this(HitType.BLOCK, position, direction, BlockPosition.ORIGIN);
    }

    public VectorRayHit(Entity entity) {
        this(entity, new Vector3D(entity.getX(), entity.getY(), entity.getZ()));
    }

    public VectorRayHit(HitType hitType, Vector3D position, Direction direction, BlockPosition blockPosition) {
        this.hitType = hitType;
        this.blockPosition = blockPosition;
        this.direction = direction;
        this.position = new Vector3D(position.x, position.y, position.z);
    }

    public VectorRayHit(Entity entity, Vector3D position) {
        this.hitType = HitType.ENTITY;
        this.entity = entity;
        this.position = position;
    }

    public BlockPosition getBlockPosition() {
        return this.blockPosition;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        return builder.append("HitResult{type=").append(this.hitType).append(", blockpos=").append(this.blockPosition).append(", f=").append(this.direction).append(", pos=").append(this.position).append(", entity=").append(this.entity).append('}').toString();
    }
}
