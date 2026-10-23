package net.mcreator.man.procedures;

import net.mcreator.man.configuration.ManconfigConfiguration;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.mcreator.man.entity.ManfromthefogEntity;
import net.mcreator.man.init.ManModEntities;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ManfromthefogOnEntityTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double random = 0.0;
         if ((Boolean)ManconfigConfiguration.ENABLE_SPAWNING.get()
            && world.m_6443_(
                  ManfromthefogEntity.class, AABB.m_165882_(new Vec3(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()), 200.0, 200.0, 200.0), e -> true
               )
               .isEmpty()
            && world.m_6443_(
                  ManaggresiveEntity.class, AABB.m_165882_(new Vec3(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()), 200.0, 200.0, 200.0), e -> true
               )
               .isEmpty()
            && world.m_204166_(BlockPos.m_274561_(x, y, z)).m_203656_(TagKey.m_203882_(Registries.f_256952_, new ResourceLocation("minecraft:is_overworld")))) {
            if (!(Boolean)ManconfigConfiguration.SPAWN_AT_DAY.get()
               && world.m_204166_(BlockPos.m_274561_(x, y, z))
                  .m_203656_(TagKey.m_203882_(Registries.f_256952_, new ResourceLocation("minecraft:is_overworld")))
               && world instanceof Level _lvl12
               && _lvl12.m_46461_()) {
               return;
            }

            if (world instanceof ServerLevel _level) {
               Entity entityToSpawn = ((EntityType)ManModEntities.MANFROMTHEFOG.get())
                  .m_262496_(_level, BlockPos.m_274561_(entity.m_20185_(), 0.0, entity.m_20189_()), MobSpawnType.MOB_SUMMONED);
               if (entityToSpawn != null) {
               }
            }

            if (world instanceof ServerLevel _levelx) {
               _levelx.m_7654_()
                  .m_129892_()
                  .m_230957_(
                     new CommandSourceStack(
                           CommandSource.f_80164_,
                           new Vec3(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                           Vec2.f_82462_,
                           _levelx,
                           4,
                           "",
                           Component.m_237113_(""),
                           _levelx.m_7654_(),
                           null
                        )
                        .m_81324_(),
                     "spreadplayers ~ ~ 35 99 false @e[type=man:manfromthefog,limit=1]"
                  );
            }

            if ((Boolean)ManconfigConfiguration.SPAWN_SOUNDS.get()) {
               SpawnsoundProcedure.execute(world, entity);
            }

            if (entity instanceof ServerPlayer _player) {
               Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("man:feelingwatched"));
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
