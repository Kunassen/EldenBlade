package dev.kunas.eldenblade;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Optional compatibility without loading optional mod classes on an ordinary server. */
final class BossCompatibility {
    private static final String WROUGHTNAUT = "com.bobmowzie.mowziesmobs.server.entity.wroughtnaut.EntityWroughtnaut";
    private BossCompatibility() {}
    static void onBloodHit(ServerPlayer player, LivingEntity target) {
        if (target instanceof Mob mob && mob.getTarget() == null) mob.setTarget(player);
        if (target.getClass().getName().equals(WROUGHTNAUT)) {
            try {
                // Both the synchronized activation flag and its server-side cache are used by 1.7.x.
                // Leaving either inactive invokes out-of-combat regeneration after an otherwise valid debit.
                target.getClass().getMethod("setActive", boolean.class).invoke(target, true);
                target.getClass().getField("active").setBoolean(target, true);
            } catch (ReflectiveOperationException ex) {
                LogUtils.getLogger().warn("EldenBlade could not activate Wroughtnaut compatibility", ex);
            }
        }
    }
}
