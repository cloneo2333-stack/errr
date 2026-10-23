package apollyon.procedures;

import apollyon.network.ApollyonModVariables;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TheFallenConditionsProcedure {
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END) {
         execute(event, event.player);
      }
   }

   public static boolean execute(Entity entity) {
      return execute(null, entity);
   }

   private static boolean execute(@Nullable Event event, Entity entity) {
      return entity == null
         ? false
         : ((ApollyonModVariables.PlayerVariables)entity.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
               .orElse(new ApollyonModVariables.PlayerVariables()))
            .fallen_overlay
            .equals("true");
   }
}
