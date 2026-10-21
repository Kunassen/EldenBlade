package dev.kunas.eldenblade;

import java.util.*;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BloodCombat {
    public static final int SECOND_STRIKE_DELAY = 5;
    public static final double SA_ATTACK_MULTIPLIER = 2.5;
    public static final double SLASH_FORWARD_OFFSET = .8;
    private record Cast(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 forward,
                        long secondTick, double attack, double saPercent, Map<UUID, Float> health) {}
    private static final List<Cast> PENDING = new ArrayList<>();
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();
    private static final Set<UUID> ACTIVE = new HashSet<>();
    private static boolean resolving;
    private BloodCombat() {}

    public static void start(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !ACTIVE.add(player.getUUID())) return;
        Vec3 forward = Vec3.directionFromRotation(0, player.getYRot());
        Cast cast = new Cast(player, player.serverLevel(), player.position().add(0, 1, 0), forward,
            player.serverLevel().getGameTime() + SECOND_STRIKE_DELAY, player.getAttributeValue(Attributes.ATTACK_DAMAGE), BladeConfig.saPercent(), new HashMap<>());
        PENDING.add(cast);
        strike(cast, false);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Iterator<Cast> it = PENDING.iterator(); it.hasNext();) {
            Cast cast = it.next();
            if (!cast.player.isAlive() || cast.player.isRemoved() || cast.player.level() != cast.level) {
                ACTIVE.remove(cast.player.getUUID()); it.remove(); continue;
            }
            if (cast.level.getGameTime() >= cast.secondTick) {
                strike(cast, true); ACTIVE.remove(cast.player.getUUID()); it.remove();
            }
        }
    }

    private static void strike(Cast cast, boolean second) {
        Vec3 center = cast.origin.add(cast.forward.scale(3.5));
        Vec3 right = new Vec3(cast.forward.z, 0, -cast.forward.x);
        Set<UUID> seen = new HashSet<>();
        resolving = true;
        try {
            for (Entity candidate : cast.level.getEntities(cast.player, new AABB(center, center).inflate(5, 3.5, 5))) {
                // Parts and their parent share one health pool; intersect actual hitboxes before deduplication.
                if (!intersects(candidate.getBoundingBox(), center, cast.forward, right)) continue;
                Entity root = candidate instanceof PartEntity<?> part ? part.getParent() : candidate;
                if (!(root instanceof LivingEntity target) || target == cast.player || !target.isAlive()
                    || target.isAlliedTo(cast.player) || !seen.add(target.getUUID())) continue;
                if (target instanceof net.minecraft.world.entity.player.Player other &&
                    (other.isCreative() || other.isSpectator() || !cast.player.canHarmPlayer(other))) continue;
                float max = cast.health.computeIfAbsent(target.getUUID(), key -> target.getMaxHealth());
                // Clear vanilla hurt immunity only around this hit, so both SA segments can land.
                int immunity = target.invulnerableTime;
                target.invulnerableTime = 0;
                try { target.hurt(cast.level.damageSources().playerAttack(cast.player), (float)(cast.attack * SA_ATTACK_MULTIPLIER)); }
                finally { target.invulnerableTime = immunity; }
                BloodDamage.apply(cast.player, target, (float)(max * cast.saPercent / 100.0));
                proc(cast.player, target);
            }
        } finally { resolving = false; }
        var slash = new BloodSlashEntity(EldenBlade.BLOOD_SLASH.get(), cast.level);
        // Shift visuals forward without moving the established damage volume.
        Vec3 visualOrigin = cast.origin.add(cast.forward.scale(SLASH_FORWARD_OFFSET));
        slash.moveTo(visualOrigin.x, visualOrigin.y + .25, visualOrigin.z,
            (float)Math.toDegrees(Math.atan2(-cast.forward.x, cast.forward.z)), 0);
        slash.start(second);
        cast.level.addFreshEntity(slash);
        cast.level.playSound(null, center.x, center.y, center.z,
            (second ? EldenBlade.BLOOD_SLASH_SECOND : EldenBlade.BLOOD_SLASH_FIRST).get(),
            SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    static boolean intersects(AABB box, Vec3 center, Vec3 forward, Vec3 right) {
        Vec3 d = box.getCenter().subtract(center);
        double hx = box.getXsize()/2, hy = box.getYsize()/2, hz = box.getZsize()/2;
        return HitVolume.intersects(d.x, d.y, d.z, hx, hy, hz, forward.x, forward.z);
    }

    @SubscribeEvent
    public static void onBladeHit(SlashBladeEvent.HitEvent event) {
        if (!resolving && event.getUser() instanceof ServerPlayer player &&
            event.getSlashBladeState().hasSpecialEffect(EldenBlade.BLOOD_BLESSING.getId())) proc(player, event.getTarget());
    }

    private static void proc(ServerPlayer player, LivingEntity target) {
        if (!target.isAlive()) return;
        boolean enabled = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
            .map(state -> state.hasSpecialEffect(EldenBlade.BLOOD_BLESSING.getId())).orElse(false);
        if (!enabled) return;
        long tick = player.serverLevel().getGameTime();
        if (tick < COOLDOWN.getOrDefault(player.getUUID(), Long.MIN_VALUE)) return;
        if (player.getRandom().nextFloat() < BladeConfig.seChance() / 100.0) {
            COOLDOWN.put(player.getUUID(), tick + BladeConfig.cooldown());
            BloodDamage.apply(player, target, (float)(target.getMaxHealth() * BladeConfig.sePercent() / 100.0));
        }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        PENDING.clear(); COOLDOWN.clear(); ACTIVE.clear(); resolving = false;
    }
}
