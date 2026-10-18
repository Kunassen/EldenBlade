package dev.kunas.eldenblade;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.ItemStack;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("eldenblade")
@PrefixGameTestTemplate(false)
public final class CombatGameTests {
    @GameTest(template="empty")
    public static void percentageBypassesInvulnerability(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        Zombie target = new Zombie(EntityType.ZOMBIE, level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000); target.setInvulnerable(true); target.invulnerableTime = 100;
        target.getAttribute(Attributes.ARMOR).setBaseValue(30);
        BloodDamage.apply(player, target, 100);
        BloodDamage.apply(player, target, 100);
        helper.assertTrue(Math.abs(target.getHealth()-800)<.01, "Expected exactly 20% max-health loss through immunity/armor");
        helper.assertTrue(target.getMaxHealth()==1000, "Maximum health must not change");
        helper.assertTrue(target.getLastHurtByMob()==player, "Kill credit must be assigned");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void percentageKillsNormally(GameTestHelper helper) {
        var target = new Zombie(EntityType.ZOMBIE, helper.getLevel());
        BloodDamage.apply(FakePlayerFactory.getMinecraft(helper.getLevel()), target, 1000);
        helper.assertTrue(target.getHealth()==0 && target.isDeadOrDying(), "Lethal debit must enter death flow");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void bladeDefinition(GameTestHelper helper) {
        var definition = helper.getLevel().registryAccess().registryOrThrow(
            mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition.REGISTRY_KEY)
            .get(EldenBlade.id("rivers_of_blood"));
        helper.assertTrue(definition != null, "Named blade must be loaded from data");
        var state = definition.getBlade().getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(
            () -> new IllegalStateException("Missing SlashBlade capability"));
        helper.assertTrue(state.getBaseAttackModifier()==24, "Base attack must be 24");
        helper.assertTrue(state.getSlashArtsKey().equals(EldenBlade.CORPSE_PILER.getId()), "SA binding");
        helper.assertTrue(state.hasSpecialEffect(EldenBlade.BLOOD_BLESSING.getId()), "SE binding");
        assertBladeDefaults(helper, definition.getBlade());
        helper.succeed();
    }
    private static void assertBladeDefaults(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(stack.getMaxDamage()==779, "Maximum durability must be 779");
        for (var e : new net.minecraft.world.item.enchantment.Enchantment[] {
            net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,
            net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING,
            net.minecraft.world.item.enchantment.Enchantments.POWER_ARROWS,
            net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT,
            net.minecraft.world.item.enchantment.Enchantments.UNBREAKING}) {
            helper.assertTrue(stack.getEnchantmentLevel(e)==10, "Initial enchantment must be X: " + e);
        }
    }
    @GameTest(template="empty")
    public static void craftingRequirements(GameTestHelper helper) {
        var level=helper.getLevel();
        var recipe=(BloodBladeRecipe) level.getRecipeManager().byKey(EldenBlade.id("rivers_of_blood")).orElseThrow();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
            public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int slot) { return ItemStack.EMPTY; }
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
        String[] items={"slashblade:proudsoul_trapezohedron", "minecraft:nether_star", "slashblade:proudsoul_crystal",
            "slashblade:slashblade", "minecraft:echo_shard", "slashblade:slashblade_wood",
            "slashblade:proudsoul_sphere", "minecraft:dragon_egg", "slashblade:proudsoul_ingot"};
        for (int i=0;i<9;i++) grid.setItem(i,new ItemStack(ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(items[i]))));
        var yamato=level.registryAccess().registryOrThrow(mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition.REGISTRY_KEY)
            .get(new net.minecraft.resources.ResourceLocation("slashblade:yamato")).getBlade();
        grid.setItem(3,yamato);
        var ys=yamato.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
        var ws=grid.getItem(5).getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
        ys.setKillCount(200); ys.setRefine(10); ys.setProudSoulCount(5000);
        ws.setKillCount(20); ws.setRefine(5); ws.setProudSoulCount(2000);
        helper.assertTrue(recipe.matches(grid,level), "Exact six thresholds must match");
        for (var state : java.util.List.of(ys,ws)) {
            int kills=state.getKillCount(), refine=state.getRefine(), souls=state.getProudSoulCount();
            state.setKillCount(kills-1); helper.assertTrue(!recipe.matches(grid,level), "Reject insufficient kills"); state.setKillCount(kills);
            state.setRefine(refine-1); helper.assertTrue(!recipe.matches(grid,level), "Reject insufficient refine"); state.setRefine(refine);
            state.setProudSoulCount(souls-1); helper.assertTrue(!recipe.matches(grid,level), "Reject insufficient souls"); state.setProudSoulCount(souls);
        }
        // Material enchantments must never downgrade the output's level X defaults.
        yamato.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 3);
        yamato.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
        var result=recipe.assemble(grid,level.registryAccess());
        assertBladeDefaults(helper,result);
        var state=result.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
        helper.assertTrue(state.getKillCount()==220 && state.getRefine()==15 && state.getProudSoulCount()==7000, "Native material stats inheritance");
        ys.setTranslationKey("item.slashblade.other");
        helper.assertTrue(!recipe.matches(grid,level), "Reject wrong named blade");
        helper.succeed();
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void twoStageSa(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "EldenTest"));
        var origin = helper.absolutePos(new net.minecraft.core.BlockPos(4, 2, 1));
        player.moveTo(origin.getX(), origin.getY(), origin.getZ(), 0, 0);
        var target = helper.spawn(EntityType.ZOMBIE, new net.minecraft.core.BlockPos(4, 2, 4));
        target.setNoAi(true); target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(1000);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(24);
        BloodCombat.start(player);
        helper.assertTrue(Math.abs(target.getHealth()-864)<.01, "First segment must deal 36 + 100");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(Math.abs(target.getHealth()-728)<.01, "Two segments must deal 72 + 200");
            var slashes=level.getEntitiesOfClass(mods.flammpfeil.slashblade.entity.EntitySlashEffect.class,
                player.getBoundingBox().inflate(9));
            helper.assertTrue(slashes.size()==2, "Exactly two visible slashes must be spawned");
            helper.assertTrue(slashes.stream().allMatch(s -> s.getShooter()==null && s.getColor()==0xE01832),
                "Red slashes must be visual-only without a damage owner");
            helper.assertTrue(slashes.stream().anyMatch(s -> s.getRotationRoll()==45f)
                && slashes.stream().anyMatch(s -> s.getRotationRoll()==-45f), "Slashes must cross");
            target.discard(); helper.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void installedBossHealth(GameTestHelper helper) {
        String[] ids = {"cataclysm:ender_guardian", "cataclysm:ignis", "mowziesmobs:ferrous_wroughtnaut", "goety:apostle"};
        java.util.Map<LivingEntity, Float> expected = new java.util.HashMap<>();
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        for (String id : ids) {
            var key = new net.minecraft.resources.ResourceLocation(id);
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(key)) continue;
            var entity = ForgeRegistries.ENTITY_TYPES.getValue(key).create(helper.getLevel());
            helper.assertTrue(entity instanceof LivingEntity, "Boss must be a living entity: " + id);
            var boss = (LivingEntity)entity;
            var pos = helper.absolutePos(new net.minecraft.core.BlockPos(4, 2, 4));
            boss.moveTo(pos.getX(), pos.getY(), pos.getZ());
            if (boss instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
            boss.setNoGravity(true); boss.setInvulnerable(true);
            boss.setHealth(boss.getMaxHealth());
            helper.getLevel().addFreshEntity(boss);
            float max = boss.getMaxHealth();
            BloodDamage.apply(player, boss, max * .1f);
            BloodDamage.apply(player, boss, max * .1f);
            float health = max * .8f;
            helper.assertTrue(Math.abs(boss.getHealth()-health)<.02, "Boss immediate debit failed: " + id);
            expected.put(boss, health);
        }
        System.out.println("ELDENBLADE_BOSS_TEST_COUNT=" + expected.size());
        helper.runAfterDelay(5, () -> {
            expected.forEach((boss, health) -> {
                helper.assertTrue(Math.abs(boss.getHealth()-health)<.02, "Boss debit reverted: " + boss.getType());
                boss.discard();
            });
            helper.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=40)
    public static void seCooldown(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BloodTest"));
        var definition = level.registryAccess().registryOrThrow(
            mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition.REGISTRY_KEY).get(EldenBlade.id("rivers_of_blood"));
        var stack = definition.getBlade();
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        var state = stack.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(() -> new IllegalStateException());
        var target = new Zombie(EntityType.ZOMBIE, level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000); target.setInvulnerable(true);
        long winningSeed=0;
        for (; winningSeed<100000; winningSeed++) {
            player.getRandom().setSeed(winningSeed);
            if (player.getRandom().nextFloat()<.1f) break;
        }
        final long seed=winningSeed;
        var hit = new mods.flammpfeil.slashblade.event.SlashBladeEvent.HitEvent(stack,state,target,player);
        player.getRandom().setSeed(seed); BloodCombat.onBladeHit(hit);
        helper.assertTrue(target.getHealth()==900, "SE must remove 10% through invulnerability");
        player.getRandom().setSeed(seed); BloodCombat.onBladeHit(hit);
        helper.assertTrue(target.getHealth()==900, "Same-tick duplicate must be blocked by global cooldown");
        helper.runAfterDelay(2, () -> {
            player.getRandom().setSeed(seed); BloodCombat.onBladeHit(hit);
            helper.assertTrue(target.getHealth()==800, "SE must become available after 2 ticks");
            helper.succeed();
        });
    }
    @GameTest(template="empty")
    public static void balanceCommandsAndConfig(GameTestHelper helper) throws Exception {
        var level=helper.getLevel();
        var player=FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ConfigTest"));
        var source=player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        var dispatcher=level.getServer().getCommands().getDispatcher();
        double attack=BladeConfig.attack(), sa=BladeConfig.saPercent(), chance=BladeConfig.seChance(), percent=BladeConfig.sePercent();
        int cooldown=BladeConfig.cooldown();
        try {
            dispatcher.execute("eldenblade config attack 60",source);
            var recipe=(BloodBladeRecipe)level.getRecipeManager().byKey(EldenBlade.id("rivers_of_blood")).orElseThrow();
            var stack=recipe.getResultItem(level.registryAccess());
            helper.assertTrue(BladeBalance.isBloodBlade(stack), "Identify named blade");
            var state=stack.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
            helper.assertTrue(state.getBaseAttackModifier()==60, "Configured recipe output attack");
            player.setItemSlot(EquipmentSlot.MAINHAND,stack);
            dispatcher.execute("eldenblade attack 80",source);
            dispatcher.execute("eldenblade config attack 70",source);
            BladeBalance.apply(stack);
            helper.assertTrue(state.getBaseAttackModifier()==80, "Held override survives default change");
            var loaded=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
            BladeBalance.apply(loaded);
            helper.assertTrue(loaded.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new).getBaseAttackModifier()==80,
                "Override persists through item serialization");
            dispatcher.execute("eldenblade attack reset",source);
            helper.assertTrue(state.getBaseAttackModifier()==70, "Reset follows default: state="+state.getBaseAttackModifier()+", config="+BladeConfig.attack()+", heldSame="+(player.getMainHandItem()==stack)+", tag="+stack.getTag());
            try { dispatcher.execute("eldenblade config attack 99",source.withPermission(0));
                helper.fail("Non-admin changed configuration");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {}
            try { dispatcher.execute("eldenblade config se_chance 101",source);
                helper.fail("Out-of-range chance accepted");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {}
            dispatcher.execute("eldenblade config sa_percent 2.5",source);
            helper.assertTrue(BladeConfig.saPercent()==2.5,"Fractional percentage accepted");
            dispatcher.execute("eldenblade config se_chance 100",source);
            dispatcher.execute("eldenblade config se_percent 7",source);
            dispatcher.execute("eldenblade config se_cooldown 0",source);
            var target=new Zombie(EntityType.ZOMBIE,level);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);target.setInvulnerable(true);
            var hit=new mods.flammpfeil.slashblade.event.SlashBladeEvent.HitEvent(stack,state,target,player);
            BloodCombat.onBladeHit(hit);BloodCombat.onBladeHit(hit);
            helper.assertTrue(target.getHealth()==860,"100% SE, 7% damage, zero cooldown must proc twice");
            dispatcher.execute("eldenblade config se_chance 0",source);
            BloodCombat.onBladeHit(hit);
            helper.assertTrue(target.getHealth()==860,"Zero chance disables SE");
            dispatcher.execute("eldenblade config se_chance 100",source);
            dispatcher.execute("eldenblade config se_cooldown 20",source);
            BloodCombat.onBladeHit(hit);BloodCombat.onBladeHit(hit);
            helper.assertTrue(target.getHealth()==790,"Configured cooldown blocks duplicate SE");
            for (int i=0;i<20;i++) {
                dispatcher.execute("eldenblade config attack 60",source);
                dispatcher.execute("eldenblade attack 80",source);
                dispatcher.execute("eldenblade config attack 70",source);
                dispatcher.execute("eldenblade attack reset",source);
                helper.assertTrue(state.getBaseAttackModifier()==70,"Rapid save/reload must not revert reset");
            }
            var path=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("serverconfig/eldenblade-server.toml");
            var saved=java.nio.file.Files.readString(path);
            helper.assertTrue(saved.contains("baseAttack = 70.0") && saved.contains("seCooldownTicks = 20"), "Commands persist to world config");
        } finally {
            BladeConfig.setDouble(BladeConfig.ATTACK, attack);BladeConfig.setDouble(BladeConfig.SA_PERCENT, sa);BladeConfig.setDouble(BladeConfig.SE_CHANCE, chance);
            BladeConfig.setDouble(BladeConfig.SE_PERCENT, percent);BladeConfig.setCooldown(cooldown);BladeConfig.SPEC.save();
        }
        helper.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void configuredSaSnapshot(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SaConfigTest"));
        var pos=helper.absolutePos(new net.minecraft.core.BlockPos(4,2,1));
        player.moveTo(pos.getX(),pos.getY(),pos.getZ(),0,0);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(24);
        var target=helper.spawn(EntityType.ZOMBIE,new net.minecraft.core.BlockPos(4,2,4));
        target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.getAttribute(Attributes.ARMOR).setBaseValue(0);target.setHealth(1000);
        double before=BladeConfig.saPercent();
        try { BladeConfig.setDouble(BladeConfig.SA_PERCENT, 2.5);BloodCombat.start(player); }
        finally { BladeConfig.setDouble(BladeConfig.SA_PERCENT, before); }
        helper.assertTrue(target.getHealth()==939,"Configured first SA segment: 36 + 25");
        helper.runAfterDelay(5,()->{
            helper.assertTrue(target.getHealth()==878,"Second SA segment retains cast-time percentage despite config change");
            target.discard();helper.succeed();
        });
    }
    @GameTest(template="empty")
    public static void orientedVolume(GameTestHelper helper) {
        helper.assertTrue(HitVolume.intersects(0,0,0,.3,.9,.3,0,1), "Center must intersect");
        helper.assertTrue(!HitVolume.intersects(0,0,-4,.3,.9,.3,0,1), "Rear outside must miss");
        helper.assertTrue(HitVolume.intersects(0,0,3.4,.05,.05,.05,0,1), "New seven-block depth must hit");
        helper.assertTrue(HitVolume.intersects(3.4,3.4,0,.05,.05,.05,0,1), "New width and height must hit");
        helper.assertTrue(!HitVolume.intersects(3.6,0,0,.05,.05,.05,0,1), "Outside seven-block width must miss");
        double k=Math.sqrt(.5);
        helper.assertTrue(!HitVolume.intersects(3.4,0,3.4,.1,.1,.1,k,k), "Rotated cube corner false positive");
        helper.assertTrue(HitVolume.intersects(3.4,0,0,.1,.1,.1,k,k), "Rotated edge should intersect");
        helper.succeed();
    }
}
