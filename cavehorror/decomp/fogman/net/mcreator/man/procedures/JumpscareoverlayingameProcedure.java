package net.mcreator.man.procedures;

import javax.annotation.Nullable;
import net.mcreator.man.network.ManModVariables;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class JumpscareoverlayingameProcedure {
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END) {
         execute(event, event.player.m_9236_());
      }
   }

   public static boolean execute(LevelAccessor world) {
      return execute(null, world);
   }

   private static boolean execute(@Nullable Event event, LevelAccessor world) {
      if (ManModVariables.MapVariables.get(world).overlay_ingame > 0.0) {
         ManModVariables.MapVariables.get(world).overlay_ingame--;
         ManModVariables.MapVariables.get(world).syncData(world);
         return true;
      } else {
         return false;
      }
   }
}
