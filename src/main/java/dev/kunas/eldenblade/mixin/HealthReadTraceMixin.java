package dev.kunas.eldenblade.mixin;

import dev.kunas.eldenblade.HealthStorage;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SynchedEntityData.class)
public abstract class HealthReadTraceMixin {
    // Both names are explicit: MCP development and SRG production, no generated refmap dependency.
    @Inject(method = {"get(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;",
        "m_135370_(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;"},
        at = @At("RETURN"), remap = false, require = 1)
    private <T> void eldenblade$observeRead(EntityDataAccessor<T> key, CallbackInfoReturnable<T> result) {
        HealthStorage.observe((SynchedEntityData)(Object)this, key, result.getReturnValue());
    }
}
