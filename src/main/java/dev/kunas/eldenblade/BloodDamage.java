package dev.kunas.eldenblade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Separate server-authoritative health debit. Does not alter global boss damage rules. */
public final class BloodDamage {
    private BloodDamage() {}
    public static float apply(ServerPlayer player, LivingEntity target, float amount) {
        if (!target.isAlive() || target.level().isClientSide || !Float.isFinite(amount) || amount <= 0) return 0;
        BossCompatibility.onBloodHit(player, target);
        var source = target.level().damageSources().playerAttack(player);
        float removed = HealthStorage.debit(target, amount);
        if (removed <= 0) return 0;
        target.setLastHurtByPlayer(player);
        target.setLastHurtByMob(player);
        target.getCombatTracker().recordDamage(source, removed);
        target.hurtTime = 10;
        if (target.getHealth() <= 0) target.die(source);
        return removed;
    }
}
