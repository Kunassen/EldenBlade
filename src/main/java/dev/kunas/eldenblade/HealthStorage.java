package dev.kunas.eldenblade;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.lang.ref.WeakReference;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;

/** Observe the active getter, rather than guessing fields by name or numerical value. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = EldenBlade.ID)
public final class HealthStorage {
    private static final ThreadLocal<ReadTrace> TRACE = new ThreadLocal<>();
    private static final Map<Class<?>, Long> WARNINGS = new WeakHashMap<>();
    private record FollowUp(WeakReference<LivingEntity> entity, long due, float after) {}
    private static final Map<UUID, FollowUp> FOLLOW_UP = new LinkedHashMap<>();

    private static final class ReadTrace {
        final SynchedEntityData owner;
        EntityDataAccessor<?> key;
        float value;
        boolean ambiguous;

        ReadTrace(SynchedEntityData owner) { this.owner = owner; }

        void read(SynchedEntityData data, EntityDataAccessor<?> accessor, Object result) {
            if (!(result instanceof Float number)) return;
            if (data != owner || accessor.getSerializer() != EntityDataSerializers.FLOAT) {
                ambiguous = true;
                return;
            }
            if (key != null && (key != accessor || Float.compare(value, number) != 0)) ambiguous = true;
            key = accessor;
            value = number;
        }
    }

    public record Snapshot(float health, EntityDataAccessor<Float> key) {
        public boolean supported() { return key != null; }
    }

    /** Called by the small SynchedEntityData#get hook. Outside a trace it does no recording. */
    public static void observe(SynchedEntityData data, EntityDataAccessor<?> key, Object value) {
        ReadTrace trace = TRACE.get();
        if (trace != null) trace.read(data, key, value);
    }

    @SuppressWarnings("unchecked")
    public static Snapshot inspect(LivingEntity entity) {
        ReadTrace previous = TRACE.get();
        ReadTrace trace = new ReadTrace(entity.getEntityData());
        TRACE.set(trace);
        float health;
        try { health = entity.getHealth(); }
        finally {
            if (previous == null) TRACE.remove(); else TRACE.set(previous);
        }
        // No candidate scanning, temporary writes or cached per-class branch assumptions.
        boolean supported = !trace.ambiguous && trace.key != null && Float.isFinite(health)
            && Float.compare(trace.value, health) == 0;
        return new Snapshot(health, supported ? (EntityDataAccessor<Float>) trace.key : null);
    }

    public static float debit(LivingEntity entity, float amount) {
        if (entity.level().isClientSide || !Float.isFinite(amount) || amount <= 0) return 0;
        Snapshot before = inspect(entity);
        if (!before.supported()) {
            warn(entity, "unsupported/ambiguous getHealth storage", before.health(), before.health());
            return 0;
        }
        float desired = Math.max(0, before.health() - amount);
        // Use the normal sync setter so dirty tracking, client updates and callbacks remain intact.
        entity.getEntityData().set(before.key(), desired);
        Snapshot after = inspect(entity);
        float actual = after.health();
        if (!after.supported() || after.key() != before.key() || Math.abs(actual - desired) > .01f)
            warn(entity, "health storage write resisted or getter changed", desired, actual);
        if (!Float.isFinite(actual)) return 0;
        if (FOLLOW_UP.size() >= 256) FOLLOW_UP.remove(FOLLOW_UP.keySet().iterator().next());
        FOLLOW_UP.put(entity.getUUID(), new FollowUp(new WeakReference<>(entity), entity.level().getGameTime()+1, actual));
        return Math.max(0, before.health() - actual);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        for (var iterator = FOLLOW_UP.values().iterator(); iterator.hasNext();) {
            FollowUp follow = iterator.next();
            LivingEntity entity = follow.entity().get();
            if (entity == null || entity.isRemoved()) { iterator.remove(); continue; }
            if (entity.level().getGameTime() < follow.due()) continue;
            iterator.remove();
            float current = entity.getHealth();
            if (current > follow.after() + .01f)
                LogUtils.getLogger().debug("EldenBlade health increased after debit: entity={}, after={}, nextTick={}; healing/phase changes may explain this; no repeat write",
                    entity.getType(), follow.after(), current);
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        FOLLOW_UP.clear(); WARNINGS.clear(); TRACE.remove();
    }

    private static synchronized void warn(LivingEntity entity, String reason, float desired, float actual) {
        long now = System.nanoTime();
        Long last = WARNINGS.get(entity.getClass());
        if (last != null && now - last < 30_000_000_000L) return;
        WARNINGS.put(entity.getClass(), now);
        LogUtils.getLogger().warn("EldenBlade {}: entity={}, class={}, requested={}, actual={}",
            reason, entity.getType(), entity.getClass().getName(), desired, actual);
    }

    private HealthStorage() {}
}
