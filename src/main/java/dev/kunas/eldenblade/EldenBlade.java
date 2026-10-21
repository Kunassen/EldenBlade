package dev.kunas.eldenblade;

import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(EldenBlade.ID)
public final class EldenBlade {
    public static final String ID = "eldenblade";
    private static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES =
        DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES, ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<BloodSlashEntity>> BLOOD_SLASH =
        ENTITIES.register("blood_slash", () -> net.minecraft.world.entity.EntityType.Builder
            .<BloodSlashEntity>of(BloodSlashEntity::new, net.minecraft.world.entity.MobCategory.MISC)
            .sized(.1f, .1f).clientTrackingRange(4).updateInterval(Integer.MAX_VALUE)
            .noSave().noSummon().fireImmune().build(ID + ":blood_slash"));
    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
        DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS, ID);
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> BLOOD_SLASH_FIRST =
        SOUNDS.register("corpse_piler_first", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id("corpse_piler_first")));
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> BLOOD_SLASH_SECOND =
        SOUNDS.register("corpse_piler_second", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id("corpse_piler_second")));
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES =
        DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS, ID);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<BloodBladeRecipe>> BLOOD_RECIPE =
        RECIPES.register("blood_blade", () -> new mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipeSerializer<>(
            net.minecraft.world.item.crafting.RecipeSerializer.SHAPED_RECIPE, BloodBladeRecipe::new));
    public static ResourceLocation id(String name) { return new ResourceLocation(ID, name); }
    private static final DeferredRegister<SlashArts> ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY, ID);
    private static final DeferredRegister<SpecialEffect> EFFECTS = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, ID);
    private static final DeferredRegister<ComboState> COMBOS = DeferredRegister.create(ComboState.REGISTRY_KEY, ID);
    // Dedicated motion shifts only JointA1 (sword) 0.8 blocks forward; sheath stays anchored.
    public static final RegistryObject<ComboState> CORPSE_PILER_FINISH = COMBOS.register("corpse_piler_finish", () -> {
        ComboState motion = ComboStateRegistry.SAKURA_END_RIGHT.get();
        return ComboState.Builder.newInstance().motionLoc(id("combostate/corpse_piler.vmd"))
            .startAndEnd(motion.getStartFrame(), motion.getEndFrame()).speed(2f).priority(100).timeout(250)
            .next(e -> ComboStateRegistry.STANDBY.getId())
            .nextOfTimeout(e -> ComboStateRegistry.STANDBY.getId()).build();
    });
    public static final RegistryObject<ComboState> CORPSE_PILER_COMBO = COMBOS.register("corpse_piler", () -> {
        ComboState motion = ComboStateRegistry.SAKURA_END_LEFT.get();
        float speed = Math.max(1f, Math.abs(motion.getEndFrame()-motion.getStartFrame()) / 7.5f);
        return ComboState.Builder.newInstance().motionLoc(id("combostate/corpse_piler.vmd"))
            .startAndEnd(motion.getStartFrame(), motion.getEndFrame()).speed(speed).priority(100)
            .next(e -> CORPSE_PILER_FINISH.getId()).nextOfTimeout(e -> CORPSE_PILER_FINISH.getId())
            .clickAction(BloodCombat::start).build();
    });
    public static final RegistryObject<SlashArts> CORPSE_PILER = ARTS.register("corpse_piler", () ->
        new SlashArts(e -> CORPSE_PILER_COMBO.getId())
            .setComboStateJust(e -> CORPSE_PILER_COMBO.getId())
            .setComboStateSuper(e -> CORPSE_PILER_COMBO.getId()));
    public static final RegistryObject<SpecialEffect> BLOOD_BLESSING = EFFECTS.register("blood_blessing", () -> new SpecialEffect(0));
    public EldenBlade() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(BladeConfig::onConfigEvent);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, BladeConfig.SPEC, "eldenblade-server.toml");
        MinecraftForge.EVENT_BUS.register(BladeBalance.class);
        ARTS.register(bus); EFFECTS.register(bus); COMBOS.register(bus); RECIPES.register(bus);
        ENTITIES.register(bus); SOUNDS.register(bus);
        MinecraftForge.EVENT_BUS.register(BloodCombat.class);
        MinecraftForge.EVENT_BUS.register(BladeCommands.class);
    }
}
