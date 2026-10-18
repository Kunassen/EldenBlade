package dev.kunas.eldenblade;

import java.util.*;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

public final class BloodCombat {
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
            player.serverLevel().getGameTime() + 3, player.getAttributeValue(Attributes.ATTACK_DAMAGE), BladeConfig.saPercent(), new HashMap<>());
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
                try { target.hurt(cast.level.damageSources().playerAttack(cast.player), (float)(cast.attack * 1.5)); }
                finally { target.invulnerableTime = immunity; }
                BloodDamage.apply(cast.player, target, (float)(max * cast.saPercent / 100.0));
                proc(cast.player, target);
            }
        } finally { resolving = false; }
        // No shooter: native SlashBlade skips its damage path entirely (1.1.23 and 1.8.62).
        var slash = new mods.flammpfeil.slashblade.entity.EntitySlashEffect(
            mods.flammpfeil.slashblade.SlashBlade.RegistryEvents.SlashEffect, cast.level);
        slash.moveTo(center.x, center.y, center.z, (float)Math.toDegrees(Math.atan2(-cast.forward.x, cast.forward.z)), 0);
        slash.setColor(0xE01832);
        slash.setRotationRoll(second ? -45f : 45f);
        slash.setBaseSize(2.3f);
        slash.setDamage(0);
        slash.setIsCritical(false);
        slash.setLifetime(10);
        slash.setSilent(true);
        cast.level.addFreshEntity(slash);
        for (int i = 0; i < 64; i++) {
            double t = i / 63.0 * 2 - 1;
            Vec3 point = center.add(right.scale(t * 3.5)).add(0, (second ? -t : t) * 3.2, 0);
            cast.level.sendParticles(new DustParticleOptions(new Vector3f(0.85f, 0.025f, 0.055f), 1.4f),
                point.x, point.y, point.z, 2, 0.04, 0.04, 0.04, 0);
        }
        cast.level.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS, 1.0f, second ? 0.7f : 0.9f);
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
