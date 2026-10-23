package net.mcreator.man.procedures;

import java.util.Comparator;
import javax.annotation.Nullable;
import net.mcreator.man.configuration.ManconfigConfiguration;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.mcreator.man.entity.ManfromthefogEntity;
import net.mcreator.man.init.ManModEntities;
import net.mcreator.man.network.ManModVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class LookProcedure {
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
         String found_entity_name = "";
         boolean entity_found = false;
         Entity man = null;
         double raytrace_distance = 0.0;
         double disappearoraggro = 0.0;
         double rx = 0.0;
         double ry = 0.0;
         double rz = 0.0;
         raytrace_distance = 0.0;
         entity_found = false;
         if (!world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).isEmpty()) {
            for (int index0 = 0; index0 < 101; index0++) {
               rx = (double)entity.m_9236_()
                  .m_45547_(
                     new ClipContext(
                        entity.m_20299_(1.0F),
                        entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                        Block.COLLIDER,
                        Fluid.NONE,
                        entity
                     )
                  )
                  .m_82425_()
                  .m_123341_();
               ry = (double)entity.m_9236_()
                  .m_45547_(
                     new ClipContext(
                        entity.m_20299_(1.0F),
                        entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                        Block.COLLIDER,
                        Fluid.NONE,
                        entity
                     )
                  )
                  .m_82425_()
                  .m_123342_();
               rz = (double)entity.m_9236_()
                  .m_45547_(
                     new ClipContext(
                        entity.m_20299_(1.0F),
                        entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                        Block.COLLIDER,
                        Fluid.NONE,
                        entity
                     )
                  )
                  .m_82425_()
                  .m_123343_();
               if (!world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(rx, ry, rz), 1.0, 1.0, 1.0), e -> true).isEmpty()
                  && world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(rx, ry, rz), 1.0, 1.0, 1.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(rx, ry, rz)).findFirst().orElse(null) != entity) {
                  entity_found = true;
                  man = world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(rx, ry, rz), 1.0, 1.0, 1.0), e -> true)
                     .stream()
                     .sorted((new Object() {
                        Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                           return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                        }
                     }).compareDistOf(rx, ry, rz))
                     .findFirst()
                     .orElse(null);
               } else {
                  entity_found = false;
                  raytrace_distance++;
               }
            }
         }

         if (entity_found
            && (
               (new Object() {
                        public boolean checkGamemode(Entity _ent) {
                           if (_ent instanceof ServerPlayer _serverPlayer) {
                              return _serverPlayer.f_8941_.m_9290_() == GameType.SURVIVAL;
                           } else {
                              return _ent.m_9236_().m_5776_() && _ent instanceof Player _player
                                 ? Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()) != null
                                    && Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()).m_105325_() == GameType.SURVIVAL
                                 : false;
                           }
                        }
                     })
                     .checkGamemode(entity)
                  || (new Object() {
                        public boolean checkGamemode(Entity _ent) {
                           if (_ent instanceof ServerPlayer _serverPlayer) {
                              return _serverPlayer.f_8941_.m_9290_() == GameType.ADVENTURE;
                           } else {
                              return _ent.m_9236_().m_5776_() && _ent instanceof Player _player
                                 ? Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()) != null
                                    && Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()).m_105325_() == GameType.ADVENTURE
                                 : false;
                           }
                        }
                     })
                     .checkGamemode(entity)
            )) {
            if ((Boolean)ManconfigConfiguration.OLD_STALK_MECHANIC.get()) {
               if (world instanceof ServerLevel _level) {
                  Entity entityToSpawn = ((EntityType)ManModEntities.MANAGGRESIVE.get())
                     .m_262496_(
                        _level,
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

               if (!man.m_9236_().m_5776_()) {
                  man.m_146870_();
               }

               Entity var24 = world.m_6443_(ManaggresiveEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true)
                  .stream()
                  .sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z))
                  .findFirst()
                  .orElse(null);
               if (var24 instanceof Mob _entity && entity instanceof LivingEntity _ent) {
                  _entity.m_6710_(_ent);
               }

               if ((Boolean)ManconfigConfiguration.DARKNESS_EFFECT.get() && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(
                     new MobEffectInstance(
                        MobEffects.f_216964_, (int)((Double)ManconfigConfiguration.DARKNESS_EFFECT_DURATION.get()).doubleValue(), 1, false, false
                     )
                  );
               }
            } else {
               disappearoraggro = (double)Math.round(Mth.m_216263_(RandomSource.m_216327_(), 1.0, 2.0));
               if (disappearoraggro == 1.0) {
                  if (world instanceof ServerLevel _levelx) {
                     _levelx.m_8767_(
                        ParticleTypes.f_123796_,
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
                           .m_20189_(),
                        10,
                        0.0,
                        2.0,
                        0.0,
                        0.5
                     );
                  }

                  if (!world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(rx, ry, rz), 1.0, 1.0, 1.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(rx, ry, rz)).findFirst().orElse(null).m_9236_().m_5776_()) {
                     world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(rx, ry, rz), 1.0, 1.0, 1.0), e -> true).stream().sorted((new Object() {
                        Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                           return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                        }
                     }).compareDistOf(rx, ry, rz)).findFirst().orElse(null).m_146870_();
                  }

                  ManModVariables.MapVariables.get(world).ring_timer = 0.0;
                  ManModVariables.MapVariables.get(world).syncData(world);
               } else if (disappearoraggro == 2.0) {
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

                  if (!man.m_9236_().m_5776_()) {
                     man.m_146870_();
                  }

                  Entity var38 = world.m_6443_(ManaggresiveEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true)
                     .stream()
                     .sorted((new Object() {
                        Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                           return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                        }
                     }).compareDistOf(x, y, z))
                     .findFirst()
                     .orElse(null);
                  if (var38 instanceof Mob _entity && entity instanceof LivingEntity _ent) {
                     _entity.m_6710_(_ent);
                  }

                  ManModVariables.MapVariables.get(world).ring_timer = 0.0;
                  ManModVariables.MapVariables.get(world).syncData(world);
                  if ((Boolean)ManconfigConfiguration.DARKNESS_EFFECT.get() && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(
                        new MobEffectInstance(
                           MobEffects.f_216964_, (int)((Double)ManconfigConfiguration.DARKNESS_EFFECT_DURATION.get()).doubleValue(), 1, false, false
                        )
                     );
                  }
               }
            }
         }
      }
   }
}
