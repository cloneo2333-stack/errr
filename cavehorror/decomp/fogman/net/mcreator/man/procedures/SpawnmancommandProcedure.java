package net.mcreator.man.procedures;

import net.mcreator.man.configuration.ManconfigConfiguration;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SpawnmancommandProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if ((Boolean)ManconfigConfiguration.ENABLE_SPAWNING.get()) {
            if ((!(Boolean)ManconfigConfiguration.SPAWN_AT_DAY.get() || !(world instanceof Level _lvl2) || !_lvl2.m_46461_())
               && world instanceof Level _lvl3
               && _lvl3.m_46461_()) {
               if (world instanceof ServerLevel _level) {
                  _level.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                           )
                           .m_81324_(),
                        "/tellraw @p {\"text\":\"Enable spawn_at_day in the config!\",\"color\":\"red\",\"italic\":true}"
                     );
               }

               return;
            }

            if (entity.m_9236_().m_46472_() == Level.f_46428_) {
               ManfromthefogOnEntityTickUpdateProcedure.execute(world, x, y, z, entity);
            } else if (world instanceof ServerLevel _level) {
               _level.m_7654_()
                  .m_129892_()
                  .m_230957_(
                     new CommandSourceStack(
                           CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                        )
                        .m_81324_(),
                     "/tellraw @p {\"text\":\"The Man can only spawn on the surface dimension!\",\"color\":\"red\",\"italic\":true}"
                  );
            }
         } else if (world instanceof ServerLevel _level) {
            _level.m_7654_()
               .m_129892_()
               .m_230957_(
                  new CommandSourceStack(
                        CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                     )
                     .m_81324_(),
                  "/tellraw @p {\"text\":\"Enable spawning in the config!\",\"color\":\"red\",\"italic\":true}"
               );
         }
      }
   }
}
