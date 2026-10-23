package net.mcreator.man.procedures;

import net.mcreator.man.network.ManModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class TimercommandProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
            _player.m_5661_(
               Component.m_237113_(ManModVariables.MapVariables.get(world).timer_global + "/" + ManModVariables.MapVariables.get(world).reset), false
            );
         }
      }
   }
}
