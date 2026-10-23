package net.mcreator.man.procedures;

import java.util.Comparator;
import javax.annotation.Nullable;
import net.mcreator.man.configuration.ManconfigConfiguration;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.mcreator.man.entity.ManfromthefogEntity;
import net.mcreator.man.init.ManModEntities;
import net.mcreator.man.network.ManModVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TimerglobalProcedure {
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END) {
         execute(event, event.player.m_9236_(), event.player.m_20185_(), event.player.m_20186_(), event.player.m_20189_(), event.player);
      }
   }

   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      execute(null, world, x, y, z, entity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         boolean spawned = false;
         double player_still = 0.0;
         double random_man = 0.0;
         if ((entity instanceof LivingEntity _livEnt ? _livEnt.m_21223_() : -1.0F) > 0.0F) {
            if (entity.m_20186_() + 2.0 >= (double)world.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, (int)entity.m_20185_(), (int)entity.m_20189_())) {
               ManModVariables.MapVariables.get(world).timer_global++;
               ManModVariables.MapVariables.get(world).syncData(world);
            }

            if (ManModVariables.MapVariables.get(world).timer_global >= ManModVariables.MapVariables.get(world).reset) {
               ManModVariables.MapVariables.get(world).reset = (double)Math.round(
                  Mth.m_216263_(
                     RandomSource.m_216327_(), (Double)ManconfigConfiguration.MIN_SPAWN_RATE.get(), (Double)ManconfigConfiguration.MAX_SPAWN_RATE.get()
                  )
               );
               ManModVariables.MapVariables.get(world).syncData(world);
               ManModVariables.MapVariables.get(world).timer_global = 0.0;
               ManModVariables.MapVariables.get(world).syncData(world);
               ManfromthefogOnEntityTickUpdateProcedure.execute(world, x, y, z, entity);
            }

            if (!world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty()
               && !(Boolean)ManconfigConfiguration.OLD_STALK_MECHANIC.get()) {
               ManModVariables.MapVariables.get(world).ring_timer++;
               ManModVariables.MapVariables.get(world).syncData(world);
            }

            if (ManModVariables.MapVariables.get(world).ring_timer == (Double)ManconfigConfiguration.STAGE1_SPAWN_RATE.get()
               && !world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty()) {
               if (!world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).stream().sorted((new Object() {
                  Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                     return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                  }
               }).compareDistOf(x, y, z)).findFirst().orElse(null).m_9236_().m_5776_()) {
                  world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_146870_();
               }

               if (world instanceof ServerLevel _level) {
                  Entity entityToSpawn = ((EntityType)ManModEntities.MANFROMTHEFOG.get())
                     .m_262496_(_level, BlockPos.m_274561_(x, 0.0, z), MobSpawnType.MOB_SUMMONED);
                  if (entityToSpawn != null) {
                     entityToSpawn.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                  }
               }

               if (world instanceof ServerLevel _levelx) {
                  _levelx.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _levelx, 4, "", Component.m_237113_(""), _levelx.m_7654_(), null
                           )
                           .m_81324_(),
                        "spreadplayers ~ ~ 35 40 false @e[type=man:manfromthefog,limit=1]"
                     );
               }

               if ((Boolean)ManconfigConfiguration.SPAWN_SOUNDS.get()) {
                  SpawnsoundProcedure.execute(world, entity);
               }
            }

            if (ManModVariables.MapVariables.get(world).ring_timer == (Double)ManconfigConfiguration.STAGE2_SPAWN_RATE.get()
               && !world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty()) {
               if (world instanceof ServerLevel _levelx) {
                  Entity entityToSpawn = ((EntityType)ManModEntities.MANAGGRESIVE.get())
                     .m_262496_(
                        _levelx,
                        BlockPos.m_274561_(
                           world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true)
                              .stream()
                              .sorted((new Object() {
                                 Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                    return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                                 }
                              }).compareDistOf(x, y, z))
                              .findFirst()
                              .orElse(null)
                              .m_20185_(),
                           world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true)
                              .stream()
                              .sorted((new Object() {
                                 Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                    return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                                 }
                              }).compareDistOf(x, y, z))
                              .findFirst()
                              .orElse(null)
                              .m_20186_(),
                           world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true)
                              .stream()
                              .sorted((new Object() {
                                 Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                    return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                                 }
                              }).compareDistOf(x, y, z))
                              .findFirst()
                              .orElse(null)
                              .m_20189_()
                        ),
                        MobSpawnType.MOB_SUMMONED
                     );
                  if (entityToSpawn != null) {
                     entityToSpawn.m_146922_(world.m_213780_().m_188501_() * 360.0F);
                  }
               }

               if ((Boolean)ManconfigConfiguration.DARKNESS_EFFECT.get() && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(
                     new MobEffectInstance(
                        MobEffects.f_216964_, (int)((Double)ManconfigConfiguration.DARKNESS_EFFECT_DURATION.get()).doubleValue(), 1, false, false
                     )
                  );
               }

               Entity var17 = world.m_6443_(ManaggresiveEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true)
                  .stream()
                  .sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z))
                  .findFirst()
                  .orElse(null);
               if (var17 instanceof Mob _entity && entity instanceof LivingEntity _ent) {
                  _entity.m_6710_(_ent);
               }

               if (!world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).stream().sorted((new Object() {
                  Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                     return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                  }
               }).compareDistOf(x, y, z)).findFirst().orElse(null).m_9236_().m_5776_()) {
                  world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_146870_();
               }

               ManModVariables.MapVariables.get(world).ring_timer = 0.0;
               ManModVariables.MapVariables.get(world).syncData(world);
            }
         }

         if (world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty()) {
            ManModVariables.renderfog_spawn = false;
         } else {
            ManModVariables.renderfog_spawn = true;
         }

         if (world.m_6443_(ManaggresiveEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty()) {
            ManModVariables.renderfog_chase = false;
         } else {
            ManModVariables.renderfog_chase = true;
         }
      }
   }
}
