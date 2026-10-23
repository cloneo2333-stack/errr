package apollyon.procedures;

import net.minecraft.world.entity.Entity;

public class TheInfectorUnleashesProcedure {
   public static boolean execute(Entity entity) {
      return entity == null ? false : !entity.getPersistentData().m_128471_("neutral");
   }
}
