package dev.kunas.eldenblade;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BladeBalance {
    public static final int BLOOD_RED = 0xFF2020;
    public static final String OVERRIDE = "EldenBladeAttackOverride";
    public static boolean isBloodBlade(ItemStack stack) {
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
            .map(s -> "item.eldenblade.rivers_of_blood".equals(s.getTranslationKey())).orElse(false);
    }
    public static boolean apply(ItemStack stack) {
        if (!isBloodBlade(stack)) return false;
        var name = stack.getHoverName();
        var color = name.getStyle().getColor();
        boolean changed = color == null || color.getValue() != BLOOD_RED;
        if (changed) stack.setHoverName(name.copy().withStyle(style -> style.withColor(BLOOD_RED)));
        double attack = BladeConfig.attack();
        var tag = stack.getTag();
        if (tag != null && tag.contains(OVERRIDE, Tag.TAG_ANY_NUMERIC)) {
            double value = tag.getDouble(OVERRIDE);
            if (Double.isFinite(value) && value >= 0 && value <= 1000000) attack = value;
        }
        final float desired = (float)attack;
        var state = stack.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new);
        if (state.getBaseAttackModifier() == desired) return changed;
        state.setBaseAttackModifier(desired);
        return true;
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        boolean changed = apply(event.player.getMainHandItem());
        if (event.player.tickCount % 20 == 0) {
            var inventory = event.player.getInventory();
            for (int i=0; i<inventory.getContainerSize(); i++) changed |= apply(inventory.getItem(i));
        }
        if (changed) {
            event.player.getInventory().setChanged();
            event.player.containerMenu.broadcastChanges();
        }
    }
    private BladeBalance() {}
}
