package apollyon.procedures;

import apollyon.ApollyonMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;

public class CmdTempResponseProcedure {
   public static void execute(LevelAccessor world) {
      if (!world.m_5776_() && world.m_7654_() != null) {
         world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Apollyon> The \"/apollyonconfig\" command is temporarily disabled."), false);
      }

      ApollyonMod.queueServerWork(30, () -> {
         if (!world.m_5776_() && world.m_7654_() != null) {
            world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Apollyon> The command will function in the next Release-type build."), false);
         }
      });
   }
}
