package apollyon.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

public class StewOfLifeWhenConsumedProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
            _entity.m_7292_(new MobEffectInstance(MobEffects.f_19616_, 2400, 4));
         }

         if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
            _entity.m_7292_(new MobEffectInstance(MobEffects.f_19605_, 20, 6));
         }

         if (entity instanceof LivingEntity _entity) {
            _entity.m_21195_(MobEffects.f_19615_);
         }

         if (world instanceof Level _level) {
            if (!_level.m_5776_()) {
               _level.m_5594_(
                  null,
                  BlockPos.m_274561_(x, y, z),
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:stew_consumed")),
                  SoundSource.AMBIENT,
                  1.0F,
                  1.0F
               );
            } else {
               _level.m_7785_(
                  x,
                  y,
                  z,
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:stew_consumed")),
                  SoundSource.AMBIENT,
                  1.0F,
                  1.0F,
                  false
               );
            }
         }
      }
   }
}
