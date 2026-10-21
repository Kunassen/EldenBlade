package dev.kunas.eldenblade;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Synthetic getters cover storage behavior without depending on a particular mod. Not shipped. */
@GameTestHolder("eldenblade")
@PrefixGameTestTemplate(false)
public final class HealthStorageGameTests {
    @GameTest(template="empty", timeoutTicks=40)
    public static void customStorageSaAndSe(GameTestHelper h) {
        var level = h.getLevel();
        var player = FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "HealthSATest"));
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        player.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(24);
        Subject target = new Subject(level); target.mode = 1;
        target.setNoAi(true); target.setNoGravity(true); target.setInvulnerable(true);
        target.moveTo(pos.getX(), pos.getY(), pos.getZ()+3);
        level.addFreshEntity(target);
        BloodCombat.start(player);
        h.assertTrue(target.getHealth()==900, "First SA custom percentage through invulnerability");
        h.runAfterDelay(7, () -> {
            h.assertTrue(target.getHealth()==800, "Second SA custom percentage through invulnerability");
            var stack = level.registryAccess().registryOrThrow(
                mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition.REGISTRY_KEY)
                .get(EldenBlade.id("rivers_of_blood")).getBlade();
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, stack);
            var state = stack.getCapability(mods.flammpfeil.slashblade.item.ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
            double previous = BladeConfig.seChance();
            try {
                BladeConfig.setDouble(BladeConfig.SE_CHANCE, 100);
                BloodCombat.onBladeHit(new mods.flammpfeil.slashblade.event.SlashBladeEvent.HitEvent(stack, state, target, player));
            } finally { BladeConfig.setDouble(BladeConfig.SE_CHANCE, previous); }
            h.assertTrue(target.getHealth()==700, "SE must use the same resolved custom health pool");
            target.discard(); h.succeed();
        });
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void installedCustomHealthBoss(GameTestHelper h) throws Exception {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("revelationfix")) {
            System.out.println("ELDENBLADE_CUSTOM_BOSS_TEST=SKIPPED (optional mod absent)");
            h.succeed(); return;
        }
        var level = h.getLevel().getServer().getLevel(Level.NETHER);
        var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
            new net.minecraft.resources.ResourceLocation("goety:apostle"));
        var boss = (net.minecraft.world.entity.Mob)type.create(level);
        // Test setup uses the installed mod's flags; the shipped resolver has no mod-specific references.
        boss.getClass().getMethod("allTitlesApostle_1_20_1$setApollyon", boolean.class).invoke(boss, true);
        boss.getClass().getMethod("revelaionfix$setApollyonHealth", float.class).invoke(boss, boss.getMaxHealth());
        float max = boss.getMaxHealth();
        var initial = HealthStorage.inspect(boss);
        h.assertTrue(initial.supported() && initial.health() == max, "Resolve installed boss custom getter");
        boss.setNoAi(true); boss.setNoGravity(true); boss.setInvulnerable(true);
        boss.moveTo(0, 110, 0);
        var player = FakePlayerFactory.getMinecraft(level);
        float first = BloodDamage.apply(player, boss, max * .1f);
        float second = BloodDamage.apply(player, boss, max * .1f);
        h.assertTrue(Math.abs(first-max*.1f)<.1 && Math.abs(second-max*.1f)<.1,
            "Both custom-health debits must succeed through native cooldown");
        h.assertTrue(Math.abs(boss.getHealth()-max*.8f)<.1, "Installed boss must lose 20% actual max health");
        // Tick once explicitly: test the native entity, including its phase logic, without spawning a fight.
        boss.tick();
        h.assertTrue(boss.getHealth() < max * .85f, "Native tick must not restore the pre-hit health pool");
        System.out.println("ELDENBLADE_CUSTOM_BOSS_TEST=PASS version="
            + net.minecraftforge.fml.ModList.get().getModContainerById("revelationfix").orElseThrow().getModInfo().getVersion()
            + " max=" + max + " first=" + first + " second=" + second + " afterTick=" + boss.getHealth());
        BloodDamage.apply(player, boss, max);
        h.assertTrue(boss.getHealth() == 0 && boss.isDeadOrDying(), "Custom boss health must reach zero");
        boss.tick();
        int deathTime = (Integer)boss.getClass().getMethod("getDeathTime").invoke(boss);
        h.assertTrue(deathTime > 0, "Native custom death sequence must start; do not force-remove the boss");
        System.out.println("ELDENBLADE_CUSTOM_BOSS_DEATH_STARTED=" + deathTime);
        boss.discard();
        h.succeed();
    }

    public static final class Subject extends Zombie {
        static final EntityDataAccessor<Float> FIRST = SynchedEntityData.defineId(Subject.class, EntityDataSerializers.FLOAT);
        static final EntityDataAccessor<Float> SECOND = SynchedEntityData.defineId(Subject.class, EntityDataSerializers.FLOAT);
        int mode;
        boolean rejectSet;
        Subject other;

        Subject(Level level) {
            super(EntityType.ZOMBIE, level);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            setHealth(1000);
        }

        @Override protected void defineSynchedData() {
            super.defineSynchedData();
            entityData.define(FIRST, 1000f);
            entityData.define(SECOND, 1000f);
        }
        private float selected() { return entityData.get(mode == 2 ? SECOND : FIRST); }
        @Override public float getHealth() {
            return switch (mode) {
                case 1, 2 -> selected();
                case 3 -> selected() * .5f;
                case 4 -> Math.min(entityData.get(FIRST), entityData.get(SECOND));
                case 5 -> 1000f;
                case 6 -> throw new IllegalStateException("fixture getter failure");
                case 7 -> other.getHealth();
                default -> super.getHealth();
            };
        }
        @Override public void setHealth(float value) { if (!rejectSet) super.setHealth(value); }
    }

    @GameTest(template="empty")
    public static void activeStorageAndSync(GameTestHelper h) {
        Subject s = new Subject(h.getLevel());
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        s.rejectSet = true;
        s.mode = 1;
        s.getEntityData().packDirty();
        h.assertTrue(BloodDamage.apply(player, s, 100) == 100, "Custom getter chain must lose exactly 100");
        h.assertTrue(s.getHealth() == 900, "Read actual custom health");
        h.assertTrue(s.getEntityData().get(Subject.SECOND) == 1000, "Same-valued unrelated slot must stay intact");
        s.mode = 0;
        h.assertTrue(s.getHealth() == 1000, "Vanilla shadow must stay intact");
        var dirty = s.getEntityData().packDirty();
        h.assertTrue(dirty != null && dirty.stream().anyMatch(v -> v.id() == Subject.FIRST.getId()),
            "Custom health changes must enter client sync");
        s.mode = 2;
        BloodDamage.apply(player, s, 200);
        h.assertTrue(s.getHealth() == 800, "Same entity changing phase must select its new health slot");
        s.mode = 1;
        h.assertTrue(s.getHealth() == 900, "Previous phase slot must stay intact");
        Subject another = new Subject(h.getLevel());
        another.mode = 2;
        BloodDamage.apply(player, another, 50);
        h.assertTrue(another.getHealth() == 950, "Same class, different instance/branch");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void rejectUnknownGetters(GameTestHelper h) {
        Subject s = new Subject(h.getLevel());
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        for (int mode : new int[] {3, 4, 5}) {
            s.mode = mode;
            h.assertTrue(BloodDamage.apply(player, s, 100) == 0, "Unsupported getter must not be mutated: " + mode);
            h.assertTrue(s.getEntityData().get(Subject.FIRST) == 1000 && s.getEntityData().get(Subject.SECOND) == 1000,
                "No probing writes to unknown health storage");
        }
        s.other = new Subject(h.getLevel()); s.other.mode = 1; s.mode = 7;
        h.assertTrue(BloodDamage.apply(player, s, 100) == 0 && s.other.getHealth() == 1000,
            "Getter forwarding to another entity must not write either entity");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void traceCleanupAndDeath(GameTestHelper h) {
        Subject s = new Subject(h.getLevel()); s.mode = 6;
        try { HealthStorage.inspect(s); h.fail("Fixture must throw"); }
        catch (IllegalStateException expected) { /* finally must clear trace */ }
        s.mode = 1;
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        h.assertTrue(BloodDamage.apply(player, s, 1500) == 1000, "Clamp at zero, report actual removed health");
        h.assertTrue(s.isDeadOrDying() && s.getLastHurtByMob() == player, "Custom health still enters normal death flow");
        Zombie vanilla = new Zombie(EntityType.ZOMBIE, h.getLevel());
        h.assertTrue(BloodDamage.apply(player, vanilla, 1) == 1, "Exception must not leak tracing to later entities");
        h.succeed();
    }
}
