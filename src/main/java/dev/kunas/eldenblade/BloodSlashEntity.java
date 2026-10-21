package dev.kunas.eldenblade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

/** A stationary, short-lived visual. It has no hit detection or damage path. */
public final class BloodSlashEntity extends Entity {
    public static final int LIFETIME = 12;
    private static final EntityDataAccessor<Boolean> SECOND =
        SynchedEntityData.defineId(BloodSlashEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> START =
        SynchedEntityData.defineId(BloodSlashEntity.class, EntityDataSerializers.LONG);

    public BloodSlashEntity(EntityType<? extends BloodSlashEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        entityData.define(SECOND, false);
        entityData.define(START, 0L);
    }

    public void start(boolean second) {
        entityData.set(SECOND, second);
        entityData.set(START, level().getGameTime());
    }

    public boolean isSecond() { return entityData.get(SECOND); }
    public float visualAge(float partialTick) {
        return Math.max(0, level().getGameTime() - entityData.get(START) + partialTick);
    }

    @Override public void tick() {
        super.tick();
        if (visualAge(0) >= LIFETIME) discard();
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 64 * 64; }
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(5); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(SECOND, tag.getBoolean("Second"));
        entityData.set(START, tag.getLong("Start"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Second", isSecond());
        tag.putLong("Start", entityData.get(START));
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
