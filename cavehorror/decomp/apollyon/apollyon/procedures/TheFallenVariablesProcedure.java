package apollyon.procedures;

import apollyon.network.ApollyonModVariables;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TheFallenVariablesProcedure {
   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      execute(event, event.getEntity());
   }

   public static void execute(Entity entity) {
      execute(null, entity);
   }

   private static void execute(@Nullable Event event, Entity entity) {
      if (entity != null) {
         String _setval = "false";
         entity.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
            capability.fallen_overlay = _setval;
            capability.syncPlayerVariables(entity);
         });
      }
   }
}
