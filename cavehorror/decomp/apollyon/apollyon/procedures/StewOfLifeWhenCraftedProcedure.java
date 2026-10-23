package apollyon.procedures;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class StewOfLifeWhenCraftedProcedure {
   public static void execute(Entity entity) {
      if (entity != null) {
         boolean var10000;
         label32: {
            if (entity instanceof ServerPlayer _plr0
               && _plr0.m_9236_() instanceof ServerLevel
               && _plr0.m_8960_().m_135996_(_plr0.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:i_think_i_can_cook_with_that"))).m_8193_()) {
               var10000 = true;
               break label32;
            }

            var10000 = false;
         }

         if (!var10000 && entity instanceof ServerPlayer _player) {
            Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:i_think_i_can_cook_with_that"));
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
