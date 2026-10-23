package vn.cavemind.mixin;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vn.cavemind.compat.Compat;

@Mixin(Entity.class)
public abstract class EntityVisibilityMixin {
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void cavemind$onlyOwner(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (Compat.ONLY_MAP.isEmpty()) return;
        UUID owner = Compat.ONLY_MAP.get(((Entity) (Object) this).getUUID());
        if (owner != null && !owner.equals(player.getUUID())) cir.setReturnValue(false);
    }
}
