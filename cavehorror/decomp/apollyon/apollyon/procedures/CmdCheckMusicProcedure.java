package apollyon.procedures;

import apollyon.network.ApollyonModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;

public class CmdCheckMusicProcedure {
   public static void execute(LevelAccessor world) {
      if (ApollyonModVariables.WorldVariables.get(world).chase_music) {
         if (!world.m_5776_() && world.m_7654_() != null) {
            world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Apollyon> Chase Music Is Currently On."), false);
         }
      } else if (!ApollyonModVariables.WorldVariables.get(world).chase_music && !world.m_5776_() && world.m_7654_() != null) {
         world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Apollyon> Chase Music Is Currently Off."), false);
      }
   }
}
