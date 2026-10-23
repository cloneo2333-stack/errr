package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.entity.TheDamnedEntity;
import apollyon.network.ApollyonModVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TheDamnedCollidesProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      if (entity != null && sourceentity != null) {
         if (entity instanceof TheDamnedEntity) {
            if (sourceentity instanceof Player || sourceentity instanceof ServerPlayer) {
               if (!ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
                  if (entity.m_20145_()) {
                     if (entity instanceof TheDamnedEntity) {
                        ((TheDamnedEntity)entity).setAnimation("animation.theDamned.jumpscare");
                     }

                     if (sourceentity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                        _entity.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 20, 1, false, false));
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
                              "effect clear @s invisibility"
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
                              "effect give @s slowness 3 255"
                           );
                     }

                     if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                        sourceentity.m_20194_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                 CommandSource.f_80164_,
                                 sourceentity.m_20182_(),
                                 sourceentity.m_20155_(),
                                 sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                                 4,
                                 sourceentity.m_7755_().getString(),
                                 sourceentity.m_5446_(),
                                 sourceentity.m_9236_().m_7654_(),
                                 sourceentity
                              ),
                              "playsound apollyon:petrified_begin ambient @a[distance=..50]"
                           );
                     }

                     if (ApollyonModVariables.WorldVariables.get(world).chase_music && world instanceof ServerLevel _level) {
                        _level.m_7654_()
                           .m_129892_()
                           .m_230957_(
                              new CommandSourceStack(
                                    CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                                 )
                                 .m_81324_(),
                              "playsound apollyon:chase_music ambient @a[distance=..50]"
                           );
                     }

                     entity.m_7618_(Anchor.EYES, new Vec3(sourceentity.m_20185_(), sourceentity.m_20186_(), sourceentity.m_20189_()));
                     sourceentity.m_7618_(Anchor.EYES, new Vec3(entity.m_20185_(), entity.m_20186_() + 1.0, entity.m_20189_()));
                     ApollyonMod.queueServerWork(
                        30,
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
                                    "effect clear @s"
                                 );
                           }

                           ApollyonMod.queueServerWork(
                              (int)Mth.m_216263_(RandomSource.m_216327_(), 500.0, 2000.0),
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

                                 if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                                    sourceentity.m_20194_()
                                       .m_129892_()
                                       .m_230957_(
                                          new CommandSourceStack(
                                             CommandSource.f_80164_,
                                             sourceentity.m_20182_(),
                                             sourceentity.m_20155_(),
                                             sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                                             4,
                                             sourceentity.m_7755_().getString(),
                                             sourceentity.m_5446_(),
                                             sourceentity.m_9236_().m_7654_(),
                                             sourceentity
                                          ),
                                          "stopsound @a ambient apollyon:chase_music"
                                       );
                                 }

                                 if (entity.m_6084_() && !sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                                    sourceentity.m_20194_()
                                       .m_129892_()
                                       .m_230957_(
                                          new CommandSourceStack(
                                             CommandSource.f_80164_,
                                             sourceentity.m_20182_(),
                                             sourceentity.m_20155_(),
                                             sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                                             4,
                                             sourceentity.m_7755_().getString(),
                                             sourceentity.m_5446_(),
                                             sourceentity.m_9236_().m_7654_(),
                                             sourceentity
                                          ),
                                          "playsound apollyon:hallucination_sound ambient @a[distance=..50]"
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
                                          "tp @e[type=apollyon:the_damned,nbt={ForgeData:{targetingPlayer: 1b}}] ~ ~-1000 ~"
                                       );
                                 }
                              }
                           );
                        }
                     );
                  }
               } else {
                  if (sourceentity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(new MobEffectInstance(MobEffects.f_216964_, 20, 1, false, false));
                  }

                  if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                     sourceentity.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              sourceentity.m_20182_(),
                              sourceentity.m_20155_(),
                              sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                              4,
                              sourceentity.m_7755_().getString(),
                              sourceentity.m_5446_(),
                              sourceentity.m_9236_().m_7654_(),
                              sourceentity
                           ),
                           "playsound apollyon:small_jumpscare ambient @s"
                        );
                  }

                  if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                     sourceentity.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              sourceentity.m_20182_(),
                              sourceentity.m_20155_(),
                              sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                              4,
                              sourceentity.m_7755_().getString(),
                              sourceentity.m_5446_(),
                              sourceentity.m_9236_().m_7654_(),
                              sourceentity
                           ),
                           "stopsound @a ambient apollyon:damned_sneak"
                        );
                  }

                  if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                     sourceentity.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              sourceentity.m_20182_(),
                              sourceentity.m_20155_(),
                              sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                              4,
                              sourceentity.m_7755_().getString(),
                              sourceentity.m_5446_(),
                              sourceentity.m_9236_().m_7654_(),
                              sourceentity
                           ),
                           "effect give @s minecraft:regeneration 4 3 true"
                        );
                  }

                  if (world instanceof ServerLevel _level) {
                     _level.m_7654_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                                 CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                              )
                              .m_81324_(),
                           "tp @e[type=apollyon:the_damned] ~ ~-1000 ~"
                        );
                  }

                  ApollyonMod.queueServerWork(5, () -> entity.m_6074_());
               }
            }
         } else if ((entity instanceof Player || entity instanceof ServerPlayer) && sourceentity instanceof TheDamnedEntity) {
            if (!ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
               if (sourceentity.m_20145_()) {
                  if (sourceentity instanceof TheDamnedEntity) {
                     ((TheDamnedEntity)sourceentity).setAnimation("animation.theDamned.jumpscare");
                  }

                  if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                     _entity.m_7292_(new MobEffectInstance(MobEffects.f_19610_, 20, 1, false, false));
                  }

                  if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                     sourceentity.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              sourceentity.m_20182_(),
                              sourceentity.m_20155_(),
                              sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                              4,
                              sourceentity.m_7755_().getString(),
                              sourceentity.m_5446_(),
                              sourceentity.m_9236_().m_7654_(),
                              sourceentity
                           ),
                           "effect clear @s invisibility"
                        );
                  }

                  if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                     sourceentity.m_20194_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                              CommandSource.f_80164_,
                              sourceentity.m_20182_(),
                              sourceentity.m_20155_(),
                              sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                              4,
                              sourceentity.m_7755_().getString(),
                              sourceentity.m_5446_(),
                              sourceentity.m_9236_().m_7654_(),
                              sourceentity
                           ),
                           "effect give @s slowness 3 255"
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
                           "playsound apollyon:petrified_begin ambient @a[distance=..50]"
                        );
                  }

                  if (ApollyonModVariables.WorldVariables.get(world).chase_music && world instanceof ServerLevel _level) {
                     _level.m_7654_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                                 CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                              )
                              .m_81324_(),
                           "playsound apollyon:chase_music ambient @a[distance=..50]"
                        );
                  }

                  sourceentity.m_7618_(Anchor.EYES, new Vec3(entity.m_20185_(), sourceentity.m_20186_(), entity.m_20189_()));
                  entity.m_7618_(Anchor.EYES, new Vec3(sourceentity.m_20185_(), sourceentity.m_20186_() + 1.0, sourceentity.m_20189_()));
                  ApollyonMod.queueServerWork(
                     30,
                     () -> {
                        if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                           sourceentity.m_20194_()
                              .m_129892_()
                              .m_230957_(
                                 new CommandSourceStack(
                                    CommandSource.f_80164_,
                                    sourceentity.m_20182_(),
                                    sourceentity.m_20155_(),
                                    sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                                    4,
                                    sourceentity.m_7755_().getString(),
                                    sourceentity.m_5446_(),
                                    sourceentity.m_9236_().m_7654_(),
                                    sourceentity
                                 ),
                                 "effect clear @s"
                              );
                        }

                        ApollyonMod.queueServerWork(
                           (int)Mth.m_216263_(RandomSource.m_216327_(), 500.0, 2000.0),
                           () -> {
                              if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                                 sourceentity.m_20194_()
                                    .m_129892_()
                                    .m_230957_(
                                       new CommandSourceStack(
                                          CommandSource.f_80164_,
                                          sourceentity.m_20182_(),
                                          sourceentity.m_20155_(),
                                          sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                                          4,
                                          sourceentity.m_7755_().getString(),
                                          sourceentity.m_5446_(),
                                          sourceentity.m_9236_().m_7654_(),
                                          sourceentity
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
                                       "stopsound @a ambient apollyon:chase_music"
                                    );
                              }

                              if (sourceentity.m_6084_() && !entity.m_9236_().m_5776_() && entity.m_20194_() != null) {
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
                                       "playsound apollyon:hallucination_sound ambient @a[distance=..50]"
                                    );
                              }

                              if (!sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
                                 sourceentity.m_20194_()
                                    .m_129892_()
                                    .m_230957_(
                                       new CommandSourceStack(
                                          CommandSource.f_80164_,
                                          sourceentity.m_20182_(),
                                          sourceentity.m_20155_(),
                                          sourceentity.m_9236_() instanceof ServerLevel ? (ServerLevel)sourceentity.m_9236_() : null,
                                          4,
                                          sourceentity.m_7755_().getString(),
                                          sourceentity.m_5446_(),
                                          sourceentity.m_9236_().m_7654_(),
                                          sourceentity
                                       ),
                                       "tp @e[type=apollyon:the_damned,nbt={ForgeData:{targetingPlayer: 1b}}] ~ ~-1000 ~"
                                    );
                              }
                           }
                        );
                     }
                  );
               }
            } else {
               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(new MobEffectInstance(MobEffects.f_216964_, 20, 1, false, false));
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
                        "playsound apollyon:small_jumpscare ambient @s"
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
                        "stopsound @a ambient apollyon:damned_sneak"
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
                        "effect give @s minecraft:regeneration 4 3 true"
                     );
               }

               if (world instanceof ServerLevel _level) {
                  _level.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                           )
                           .m_81324_(),
                        "tp @e[type=apollyon:the_damned] ~ ~-1000 ~"
                     );
               }

               ApollyonMod.queueServerWork(5, () -> sourceentity.m_6074_());
            }
         }
      }
   }
}
