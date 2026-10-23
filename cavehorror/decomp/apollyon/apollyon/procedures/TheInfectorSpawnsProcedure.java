package apollyon.procedures;

import apollyon.entity.TheInfectorEntity;
import net.minecraft.world.entity.Entity;

public class TheInfectorSpawnsProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         if (entity instanceof TheInfectorEntity) {
            entity.m_20260_(true);
         }
      }
   }
}
