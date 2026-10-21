package dev.kunas.eldenblade.client;

import dev.kunas.eldenblade.BladeBalance;
import dev.kunas.eldenblade.EldenBlade;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Blood-red name and Shift lore, restricted to this blade. */
@Mod.EventBusSubscriber(modid = EldenBlade.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BladeTooltip {
    private static final int BLOOD_RED = BladeBalance.BLOOD_RED;

    private BladeTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!BladeBalance.isBloodBlade(event.getItemStack())) return;
        if (!event.getToolTip().isEmpty()) {
            event.getToolTip().set(0, event.getToolTip().get(0).copy()
                .withStyle(style -> style.withColor(BLOOD_RED)));
        }
        if (!Screen.hasShiftDown()) return;
        event.getToolTip().add(Component.empty());
        for (int line = 1; line <= 6; line++) {
            if (line == 3) event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.eldenblade.rivers_of_blood.lore." + line)
                .withStyle(style -> style.withColor(BLOOD_RED)));
        }
    }
}
