package dev.kunas.eldenblade;

import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Keep native blade-stat inheritance, but restore the output's over-level enchantments. */
public final class BloodBladeRecipe extends SlashBladeShapedRecipe {
    public BloodBladeRecipe(ShapedRecipe recipe, ResourceLocation blade) { super(recipe, blade); }

    @Override public ItemStack getResultItem(RegistryAccess access) {
        ItemStack result = super.getResultItem(access);
        BladeBalance.apply(result);
        return result;
    }

    @Override public ItemStack assemble(CraftingContainer input, RegistryAccess access) {
        ItemStack output = super.assemble(input, access);
        var enchantments = EnchantmentHelper.getEnchantments(output);
        EnchantmentHelper.getEnchantments(getResultItem(access)).forEach(
            (enchantment, level) -> enchantments.merge(enchantment, level, Math::max));
        EnchantmentHelper.setEnchantments(enchantments, output);
        return output;
    }

    @Override public RecipeSerializer<?> getSerializer() { return EldenBlade.BLOOD_RECIPE.get(); }
}
