package apollyon.procedures;

import apollyon.network.ApollyonModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;

public class CmdSpawnVolumeProcedure {
   public static void execute(LevelAccessor world) {
      if (ApollyonModVariables.WorldVariables.get(world).spawn_volume.equals("soft")) {
         if (!world.m_5776_() && world.m_7654_() != null) {
            world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Apollyon> Spawn Volume Is Currently Soft."), false);
         }
      } else if (ApollyonModVariables.WorldVariables.get(world).spawn_volume.equals("loud") && !world.m_5776_() && world.m_7654_() != null) {
         world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Apollyon> Spawn Volume Is Currently Loud."), false);
      }
   }
}
