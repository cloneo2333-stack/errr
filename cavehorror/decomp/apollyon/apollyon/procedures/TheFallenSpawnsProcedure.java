package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.entity.TheFallenEntity;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TheFallenSpawnsProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (entity instanceof TheFallenEntity) {
            if (world instanceof ServerLevel _level) {
               _level.m_7654_()
                  .m_129892_()
                  .m_230957_(
                     new CommandSourceStack(
                           CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                        )
                        .m_81324_(),
                     "advancement grant @a only apollyon:apollyon"
                  );
            }

            entity.getPersistentData().m_128379_("seen", false);
            ApollyonMod.queueServerWork(1200, () -> entity.m_6074_());
         }

         if (entity.m_5830_()) {
            while (entity.m_5830_()) {
               if (!entity.m_9236_().m_5776_() && entity.m_20194_() != null) {
                  entity.m_20194_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                           CommandSource.f_80164_,
                           entity.m_20182_(),
                           entity.m_20155_(),
                           entity.m_9236_() instanceof ServerLevel ? (ServerLevel)entity.m_9236_() : null,
                           4,
                           entity.m_7755_().getString(),
                           entity.m_5446_(),
                           entity.m_9236_().m_7654_(),
                           entity
                        ),
                        "tp @s ~ ~1 ~"
                     );
               }

               if (!entity.m_5830_()) {
                  break;
               }
            }
         }
      }
   }
}
