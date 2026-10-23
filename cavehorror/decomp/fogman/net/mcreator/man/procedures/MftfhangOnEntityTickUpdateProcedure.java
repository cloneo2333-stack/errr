package net.mcreator.man.procedures;

import java.util.Comparator;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.mcreator.man.entity.ManfromthefogEntity;
import net.mcreator.man.init.ManModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MftfhangOnEntityTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (!world.m_46861_(BlockPos.m_274561_(x, y, z)) && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
            _entity.m_7292_(new MobEffectInstance(MobEffects.f_19620_, 3, 3, false, false));
         }

         if (!world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).isEmpty()) {
            entity.m_7618_(
               Anchor.EYES,
               new Vec3(
                  world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20185_(),
                  world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20186_() + 1.5,
                  world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20189_()
               )
            );
         }

         if (!world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 15.0, 15.0, 15.0), e -> true).isEmpty()
            && entity instanceof LivingEntity _entity
            && _entity.m_142582_(world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 15.0, 15.0, 15.0), e -> true).stream().sorted((new Object() {
               Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                  return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
               }
            }).compareDistOf(x, y, z)).findFirst().orElse(null))
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
                     .checkGamemode(world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 15.0, 15.0, 15.0), e -> true).stream().sorted((new Object() {
                        Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                           return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                        }
                     }).compareDistOf(x, y, z)).findFirst().orElse(null))
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
                     .checkGamemode(world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 15.0, 15.0, 15.0), e -> true).stream().sorted((new Object() {
                        Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                           return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                        }
                     }).compareDistOf(x, y, z)).findFirst().orElse(null))
            )) {
            if (!entity.m_9236_().m_5776_()) {
               entity.m_146870_();
            }

            if (world instanceof ServerLevel _level) {
               Entity entityToSpawn = ((EntityType)ManModEntities.MANAGGRESIVE.get()).m_262496_(_level, BlockPos.m_274561_(x, y, z), MobSpawnType.MOB_SUMMONED);
               if (entityToSpawn != null) {
                  entityToSpawn.m_146922_(world.m_213780_().m_188501_() * 360.0F);
               }
            }

            Entity var14 = world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 15.0, 15.0, 15.0), e -> true).stream().sorted((new Object() {
               Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                  return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
               }
            }).compareDistOf(x, y, z)).findFirst().orElse(null);
            if (var14 instanceof LivingEntity _entityx && !_entityx.m_9236_().m_5776_()) {
               _entityx.m_7292_(new MobEffectInstance(MobEffects.f_216964_, 60, 1, false, false));
            }
         }

         if ((
               !world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).isEmpty()
                  || !world.m_6443_(ManaggresiveEntity.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).isEmpty()
                  || world instanceof Level _lvl23 && _lvl23.m_46461_()
            )
            && !entity.m_9236_().m_5776_()) {
            entity.m_146870_();
         }
      }
   }
}
