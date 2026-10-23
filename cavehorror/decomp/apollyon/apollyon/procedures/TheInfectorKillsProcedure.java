package apollyon.procedures;

import apollyon.entity.TheInfectorEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class TheInfectorKillsProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      if (entity != null && sourceentity != null) {
         if ((entity instanceof Player || entity instanceof ServerPlayer) && sourceentity instanceof TheInfectorEntity) {
            TheDamnedKillsProcedure.execute(world, x, y, z, entity, sourceentity);
            if (!sourceentity.m_9236_().m_5776_()) {
               sourceentity.m_146870_();
            }
         }
      }
   }
}
