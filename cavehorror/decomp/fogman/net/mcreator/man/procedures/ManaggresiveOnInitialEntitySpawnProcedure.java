package net.mcreator.man.procedures;

import javax.annotation.Nullable;
import net.mcreator.man.configuration.ManconfigConfiguration;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber
public class ManaggresiveOnInitialEntitySpawnProcedure {
   @SubscribeEvent
   public static void onEntitySpawned(EntityJoinLevelEvent event) {
      execute(event, event.getLevel(), event.getEntity().m_20185_(), event.getEntity().m_20186_(), event.getEntity().m_20189_(), event.getEntity());
   }

   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      execute(null, world, x, y, z, entity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (entity instanceof ManaggresiveEntity) {
            if ((Boolean)ManconfigConfiguration.LIGHTNINGS.get() && world instanceof ServerLevel _level) {
               LightningBolt entityToSpawn = (LightningBolt)EntityType.f_20465_.m_20615_(_level);
               entityToSpawn.m_20219_(Vec3.m_82539_(BlockPos.m_274561_(x, y, z)));
               entityToSpawn.m_20874_(true);
               _level.m_7967_(entityToSpawn);
            }

            entity.getPersistentData()
               .m_128347_(
                  "deadline",
                  Mth.m_216263_(
                     RandomSource.m_216327_(), (Double)ManconfigConfiguration.MIN_CHASE_DURATION.get(), (Double)ManconfigConfiguration.MAX_CHASE_DURATION.get()
                  )
               );
            entity.getPersistentData().m_128379_("ready", true);
            if (world instanceof Level _level) {
               if (!_level.m_5776_()) {
                  _level.m_5594_(
                     null,
                     BlockPos.m_274561_(x, y, z),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:look")),
                     SoundSource.HOSTILE,
                     3.0F,
                     1.0F
                  );
               } else {
                  _level.m_7785_(
                     x, y, z, (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:look")), SoundSource.HOSTILE, 3.0F, 1.0F, false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.m_5776_()) {
                  _levelx.m_5594_(
                     null,
                     BlockPos.m_274561_(x, y, z),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:loop")),
                     SoundSource.HOSTILE,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelx.m_7785_(
                     x, y, z, (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:loop")), SoundSource.HOSTILE, 1.0F, 1.0F, false
                  );
               }
            }
         }
      }
   }
}
