package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.entity.TheDamnedEntity;
import apollyon.network.ApollyonModVariables;
import java.util.Comparator;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TheDamnedSpawnsProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0) <= 4.0) {
            ApollyonModVariables.WorldVariables.get(world).damned_fakeout = true;
            ApollyonModVariables.WorldVariables.get(world).syncData(world);
         } else {
            ApollyonModVariables.WorldVariables.get(world).damned_fakeout = false;
            ApollyonModVariables.WorldVariables.get(world).syncData(world);
         }

         if (entity instanceof TheDamnedEntity) {
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

            if (ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
               if (entity instanceof Player _player) {
                  _player.m_150110_().f_35934_ = ApollyonModVariables.WorldVariables.get(world).damned_fakeout;
                  _player.m_6885_();
               }

               if (world instanceof ServerLevel _level) {
                  _level.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                           )
                           .m_81324_(),
                        "playsound apollyon:damned_sneak ambient @a ~ ~ ~ 100"
                     );
               }

               for (int index0 = 0; index0 < 5; index0++) {
                  if (entity instanceof Mob _entity) {
                     _entity.m_21573_()
                        .m_26519_(
                           world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20185_(),
                           world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20186_(),
                           world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20189_(),
                           1.0
                        );
                  }

                  if (entity instanceof Mob _entity) {
                     _entity.m_21573_()
                        .m_26519_(
                           world.m_6443_(ServerPlayer.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20185_(),
                           world.m_6443_(ServerPlayer.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20186_(),
                           world.m_6443_(ServerPlayer.class, AABB.m_165882_(new Vec3(x, y, z), 100.0, 100.0, 100.0), e -> true).stream().sorted((new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20189_(),
                           1.0
                        );
                  }
               }

               ApollyonMod.queueServerWork(
                  159,
                  () -> {
                     if (entity.m_6084_()) {
                        if (world instanceof ServerLevel _level) {
                           _level.m_7654_()
                              .m_129892_()
                              .m_230957_(
                                 new CommandSourceStack(
                                       CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                                    )
                                    .m_81324_(),
                                 "playsound apollyon:damned_flee ambient @a ~ ~ ~ 100"
                              );
                        }

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
                                 "particle minecraft:cloud ~ ~1 ~"
                              );
                        }

                        ApollyonMod.queueServerWork(
                           1,
                           () -> {
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
                                       "particle minecraft:cloud ~ ~1 ~"
                                    );
                              }

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
                                       "tp @s ~ ~-1000 ~"
                                    );
                              }
                           }
                        );
                     }
                  }
               );
            }
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
