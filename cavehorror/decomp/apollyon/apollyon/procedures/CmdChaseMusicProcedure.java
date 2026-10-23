package apollyon.procedures;

import apollyon.network.ApollyonModVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CmdChaseMusicProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (ApollyonModVariables.WorldVariables.get(world).chase_music) {
         if (world instanceof ServerLevel _level) {
            _level.m_7654_()
               .m_129892_()
               .m_230957_(
                  new CommandSourceStack(
                        CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                     )
                     .m_81324_(),
                  "stopsound @a ambient apollyon:chase_music"
               );
         }

         if (!world.m_5776_() && world.m_7654_() != null) {
            world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Apollyon> Chase Music Is Now Off."), false);
         }

         ApollyonModVariables.WorldVariables.get(world).chase_music = false;
         ApollyonModVariables.WorldVariables.get(world).syncData(world);
      } else if (!ApollyonModVariables.WorldVariables.get(world).chase_music) {
         if (!world.m_5776_() && world.m_7654_() != null) {
            world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Apollyon> Chase Music Is Now On."), false);
         }

         ApollyonModVariables.WorldVariables.get(world).chase_music = true;
         ApollyonModVariables.WorldVariables.get(world).syncData(world);
      }
   }
}
