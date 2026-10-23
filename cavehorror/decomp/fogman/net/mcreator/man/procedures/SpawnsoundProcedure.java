package net.mcreator.man.procedures;

import net.mcreator.man.network.ManModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

public class SpawnsoundProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         ManModVariables.random_sound_spawn = (double)Math.round(Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0));
         if (ManModVariables.random_sound_spawn == 1.0) {
            if (world instanceof Level _level) {
               if (!_level.m_5776_()) {
                  _level.m_5594_(
                     null,
                     BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_1")),
                     SoundSource.HOSTILE,
                     3.0F,
                     0.5F
                  );
               } else {
                  _level.m_7785_(
                     entity.m_20185_(),
                     entity.m_20186_(),
                     entity.m_20189_(),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_1")),
                     SoundSource.HOSTILE,
                     3.0F,
                     0.5F,
                     false
                  );
               }
            }
         } else if (ManModVariables.random_sound_spawn == 2.0) {
            if (world instanceof Level _levelx) {
               if (!_levelx.m_5776_()) {
                  _levelx.m_5594_(
                     null,
                     BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_2")),
                     SoundSource.HOSTILE,
                     3.0F,
                     0.5F
                  );
               } else {
                  _levelx.m_7785_(
                     entity.m_20185_(),
                     entity.m_20186_(),
                     entity.m_20189_(),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_2")),
                     SoundSource.HOSTILE,
                     3.0F,
                     0.5F,
                     false
                  );
               }
            }
         } else if (ManModVariables.random_sound_spawn == 3.0) {
            if (world instanceof Level _levelxx) {
               if (!_levelxx.m_5776_()) {
                  _levelxx.m_5594_(
                     null,
                     BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_3")),
                     SoundSource.HOSTILE,
                     3.0F,
                     0.5F
                  );
               } else {
                  _levelxx.m_7785_(
                     entity.m_20185_(),
                     entity.m_20186_(),
                     entity.m_20189_(),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_3")),
                     SoundSource.HOSTILE,
                     3.0F,
                     0.5F,
                     false
                  );
               }
            }
         } else if (ManModVariables.random_sound_spawn == 4.0) {
            if (world instanceof Level _levelxxx) {
               if (!_levelxxx.m_5776_()) {
                  _levelxxx.m_5594_(
                     null,
                     BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_4")),
                     SoundSource.HOSTILE,
                     3.0F,
                     1.0F
                  );
               } else {
                  _levelxxx.m_7785_(
                     entity.m_20185_(),
                     entity.m_20186_(),
                     entity.m_20189_(),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_4")),
                     SoundSource.HOSTILE,
                     3.0F,
                     1.0F,
                     false
                  );
               }
            }
         } else if (ManModVariables.random_sound_spawn == 5.0 && world instanceof Level _levelxxxx) {
            if (!_levelxxxx.m_5776_()) {
               _levelxxxx.m_5594_(
                  null,
                  BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_5")),
                  SoundSource.HOSTILE,
                  3.0F,
                  1.0F
               );
            } else {
               _levelxxxx.m_7785_(
                  entity.m_20185_(),
                  entity.m_20186_(),
                  entity.m_20189_(),
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:spawn_5")),
                  SoundSource.HOSTILE,
                  3.0F,
                  1.0F,
                  false
               );
            }
         }
      }
   }
}
