package vn.cavemind.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vn.cavemind.compat.Compat;

@Pseudo
@Mixin(targets = "apollyon.procedures.TheDamnedTicksProcedure", remap = false)
public abstract class DamnedTickMixin {
    @Inject(method = "execute(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cavemind$gate(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        if (Compat.managed(entity)) ci.cancel();
    }
}
