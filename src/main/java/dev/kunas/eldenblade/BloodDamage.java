package dev.kunas.eldenblade;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Separate server-authoritative health debit. Does not alter global boss damage rules. */
public final class BloodDamage {
    private BloodDamage() {}
    public static float apply(ServerPlayer player, LivingEntity target, float amount) {
        if (!target.isAlive() || target.level().isClientSide || !Float.isFinite(amount) || amount <= 0) return 0;
        BossCompatibility.onBloodHit(player, target);
        float before = target.getHealth();
        float desired = Math.max(0, before - amount);
        var source = target.level().damageSources().playerAttack(player);
        target.setLastHurtByPlayer(player);
        target.setLastHurtByMob(player);
        target.getCombatTracker().recordDamage(source, Math.min(before, amount));
        // Vanilla synchronized health storage bypasses overridden hurt/setHealth and armor/shields.
        // A mod with a separate health store requires a dedicated adapter; verify instead of claiming success.
        target.getEntityData().set(LivingEntity.DATA_HEALTH_ID, desired);
        target.hurtTime = 10;
        if (target.getHealth() > desired + 0.01f) {
            LogUtils.getLogger().warn("EldenBlade health debit resisted by {}: requested={}, actual={}",
                target.getType(), desired, target.getHealth());
        }
        if (target.getHealth() <= 0) target.die(source);
        return Math.max(0, before - target.getHealth());
    }
}
