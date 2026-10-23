package apollyon.procedures;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TheDamnedDiesProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity sourceentity) {
      if (sourceentity != null) {
         if (sourceentity instanceof Player || !(sourceentity instanceof ServerPlayer)) {
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

            if (sourceentity instanceof ServerPlayer _player) {
               Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:back_to_hell"));
               AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
               if (!_ap.m_8193_()) {
                  for (String criteria : _ap.m_8219_()) {
                     _player.m_8960_().m_135988_(_adv, criteria);
                  }
               }
            }
         }
      }
   }
}
