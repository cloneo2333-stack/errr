package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.network.ApollyonModVariables;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TheDamnedRisesProcedure {
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
         if (world.m_204166_(BlockPos.m_274561_(x, y, z)).m_203656_(TagKey.m_203882_(Registries.f_256952_, new ResourceLocation("minecraft:is_overworld")))) {
            boolean var10000;
            label39: {
               if (world instanceof Level _lvl1 && _lvl1.m_46461_()) {
                  var10000 = true;
                  break label39;
               }

               var10000 = false;
            }

            if (!var10000
               && (
                  ApollyonModVariables.WorldVariables.get(world).damned_spawning.equals("False")
                     || ApollyonModVariables.WorldVariables.get(world).damned_spawning.equals("Loop")
               )) {
               ApollyonModVariables.WorldVariables.get(world).damned_spawning = "True";
               ApollyonModVariables.WorldVariables.get(world).syncData(world);
               ApollyonMod.queueServerWork(
                  (int)Mth.m_216263_(RandomSource.m_216327_(), 1200.0, 1800.0),
                  () -> {
                     if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 2.0) {
                        if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0) <= 4.0) {
                           ApollyonModVariables.WorldVariables.get(world).damned_fakeout = true;
                           ApollyonModVariables.WorldVariables.get(world).syncData(world);
                        } else {
                           ApollyonModVariables.WorldVariables.get(world).damned_fakeout = false;
                           ApollyonModVariables.WorldVariables.get(world).syncData(world);
                        }

                        ApollyonMod.queueServerWork(
                           20,
                           () -> {
                              boolean var10000x;
                              label28: {
                                 if (world instanceof Level _lvl5 && _lvl5.m_46461_()) {
                                    var10000x = true;
                                    break label28;
                                 }

                                 var10000x = false;
                              }

                              if (!var10000x) {
                                 if (ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
                                    if (world instanceof ServerLevel _level) {
                                       _level.m_7654_()
                                          .m_129892_()
                                          .m_230957_(
                                             new CommandSourceStack(
                                                   CommandSource.f_80164_,
                                                   new Vec3(
                                                      entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0),
                                                      entity.m_20186_(),
                                                      entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0)
                                                   ),
                                                   Vec2.f_82462_,
                                                   _level,
                                                   4,
                                                   "",
                                                   Component.m_237113_(""),
                                                   _level.m_7654_(),
                                                   null
                                                )
                                                .m_81324_(),
                                             "summon apollyon:the_damned"
                                          );
                                    }
                                 } else {
                                    if (world instanceof ServerLevel _level) {
                                       _level.m_7654_()
                                          .m_129892_()
                                          .m_230957_(
                                             new CommandSourceStack(
                                                   CommandSource.f_80164_,
                                                   new Vec3(
                                                      entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0),
                                                      entity.m_20186_(),
                                                      entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0)
                                                   ),
                                                   Vec2.f_82462_,
                                                   _level,
                                                   4,
                                                   "",
                                                   Component.m_237113_(""),
                                                   _level.m_7654_(),
                                                   null
                                                )
                                                .m_81324_(),
                                             "summon apollyon:the_damned"
                                          );
                                    }

                                    if (world instanceof ServerLevel _level) {
                                       _level.m_7654_()
                                          .m_129892_()
                                          .m_230957_(
                                             new CommandSourceStack(
                                                   CommandSource.f_80164_,
                                                   new Vec3(x, y, z),
                                                   Vec2.f_82462_,
                                                   _level,
                                                   4,
                                                   "",
                                                   Component.m_237113_(""),
                                                   _level.m_7654_(),
                                                   null
                                                )
                                                .m_81324_(),
                                             "playsound apollyon:quieter_spawn ambient @a ~ ~ ~ 100"
                                          );
                                    }
                                 }
                              }
                           }
                        );
                     }

                     ApollyonMod.queueServerWork(
                        (int)Mth.m_216263_(RandomSource.m_216327_(), 1200.0, 1800.0),
                        () -> {
                           if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 3.0) {
                              if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0) <= 4.0) {
                                 ApollyonModVariables.WorldVariables.get(world).damned_fakeout = true;
                                 ApollyonModVariables.WorldVariables.get(world).syncData(world);
                              } else {
                                 ApollyonModVariables.WorldVariables.get(world).damned_fakeout = false;
                                 ApollyonModVariables.WorldVariables.get(world).syncData(world);
                              }

                              ApollyonMod.queueServerWork(
                                 20,
                                 () -> {
                                    boolean var10000x;
                                    label28: {
                                       if (world instanceof Level _lvl23 && _lvl23.m_46461_()) {
                                          var10000x = true;
                                          break label28;
                                       }

                                       var10000x = false;
                                    }

                                    if (!var10000x) {
                                       if (ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
                                          if (world instanceof ServerLevel _level) {
                                             _level.m_7654_()
                                                .m_129892_()
                                                .m_230957_(
                                                   new CommandSourceStack(
                                                         CommandSource.f_80164_,
                                                         new Vec3(
                                                            entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0),
                                                            entity.m_20186_(),
                                                            entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0)
                                                         ),
                                                         Vec2.f_82462_,
                                                         _level,
                                                         4,
                                                         "",
                                                         Component.m_237113_(""),
                                                         _level.m_7654_(),
                                                         null
                                                      )
                                                      .m_81324_(),
                                                   "summon apollyon:the_damned"
                                                );
                                          }
                                       } else {
                                          if (world instanceof ServerLevel _level) {
                                             _level.m_7654_()
                                                .m_129892_()
                                                .m_230957_(
                                                   new CommandSourceStack(
                                                         CommandSource.f_80164_,
                                                         new Vec3(
                                                            entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0),
                                                            entity.m_20186_(),
                                                            entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0)
                                                         ),
                                                         Vec2.f_82462_,
                                                         _level,
                                                         4,
                                                         "",
                                                         Component.m_237113_(""),
                                                         _level.m_7654_(),
                                                         null
                                                      )
                                                      .m_81324_(),
                                                   "summon apollyon:the_damned"
                                                );
                                          }

                                          if (world instanceof ServerLevel _level) {
                                             _level.m_7654_()
                                                .m_129892_()
                                                .m_230957_(
                                                   new CommandSourceStack(
                                                         CommandSource.f_80164_,
                                                         new Vec3(x, y, z),
                                                         Vec2.f_82462_,
                                                         _level,
                                                         4,
                                                         "",
                                                         Component.m_237113_(""),
                                                         _level.m_7654_(),
                                                         null
                                                      )
                                                      .m_81324_(),
                                                   "playsound apollyon:quieter_spawn ambient @a ~ ~ ~ 100"
                                                );
                                          }
                                       }
                                    }
                                 }
                              );
                           }

                           ApollyonMod.queueServerWork(
                              (int)Mth.m_216263_(RandomSource.m_216327_(), 1200.0, 1800.0),
                              () -> {
                                 if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 4.0) {
                                    if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0) <= 4.0) {
                                       ApollyonModVariables.WorldVariables.get(world).damned_fakeout = true;
                                       ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                    } else {
                                       ApollyonModVariables.WorldVariables.get(world).damned_fakeout = false;
                                       ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                    }

                                    ApollyonMod.queueServerWork(
                                       20,
                                       () -> {
                                          boolean var10000x;
                                          label28: {
                                             if (world instanceof Level _lvl41 && _lvl41.m_46461_()) {
                                                var10000x = true;
                                                break label28;
                                             }

                                             var10000x = false;
                                          }

                                          if (!var10000x) {
                                             if (ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
                                                if (world instanceof ServerLevel _level) {
                                                   _level.m_7654_()
                                                      .m_129892_()
                                                      .m_230957_(
                                                         new CommandSourceStack(
                                                               CommandSource.f_80164_,
                                                               new Vec3(
                                                                  entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0),
                                                                  entity.m_20186_(),
                                                                  entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0)
                                                               ),
                                                               Vec2.f_82462_,
                                                               _level,
                                                               4,
                                                               "",
                                                               Component.m_237113_(""),
                                                               _level.m_7654_(),
                                                               null
                                                            )
                                                            .m_81324_(),
                                                         "summon apollyon:the_damned"
                                                      );
                                                }
                                             } else {
                                                if (world instanceof ServerLevel _level) {
                                                   _level.m_7654_()
                                                      .m_129892_()
                                                      .m_230957_(
                                                         new CommandSourceStack(
                                                               CommandSource.f_80164_,
                                                               new Vec3(
                                                                  entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0),
                                                                  entity.m_20186_(),
                                                                  entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0)
                                                               ),
                                                               Vec2.f_82462_,
                                                               _level,
                                                               4,
                                                               "",
                                                               Component.m_237113_(""),
                                                               _level.m_7654_(),
                                                               null
                                                            )
                                                            .m_81324_(),
                                                         "summon apollyon:the_damned"
                                                      );
                                                }

                                                if (world instanceof ServerLevel _level) {
                                                   _level.m_7654_()
                                                      .m_129892_()
                                                      .m_230957_(
                                                         new CommandSourceStack(
                                                               CommandSource.f_80164_,
                                                               new Vec3(x, y, z),
                                                               Vec2.f_82462_,
                                                               _level,
                                                               4,
                                                               "",
                                                               Component.m_237113_(""),
                                                               _level.m_7654_(),
                                                               null
                                                            )
                                                            .m_81324_(),
                                                         "playsound apollyon:quieter_spawn ambient @a ~ ~ ~ 100"
                                                      );
                                                }
                                             }
                                          }
                                       }
                                    );
                                 }

                                 ApollyonMod.queueServerWork(
                                    (int)Mth.m_216263_(RandomSource.m_216327_(), 1200.0, 1800.0),
                                    () -> {
                                       if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 4.0) {
                                          if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0) <= 4.0) {
                                             ApollyonModVariables.WorldVariables.get(world).damned_fakeout = true;
                                             ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                          } else {
                                             ApollyonModVariables.WorldVariables.get(world).damned_fakeout = false;
                                             ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                          }

                                          ApollyonMod.queueServerWork(
                                             20,
                                             () -> {
                                                boolean var10000x;
                                                label28: {
                                                   if (world instanceof Level _lvl59 && _lvl59.m_46461_()) {
                                                      var10000x = true;
                                                      break label28;
                                                   }

                                                   var10000x = false;
                                                }

                                                if (!var10000x) {
                                                   if (ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
                                                      if (world instanceof ServerLevel _level) {
                                                         _level.m_7654_()
                                                            .m_129892_()
                                                            .m_230957_(
                                                               new CommandSourceStack(
                                                                     CommandSource.f_80164_,
                                                                     new Vec3(
                                                                        entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0),
                                                                        entity.m_20186_(),
                                                                        entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0)
                                                                     ),
                                                                     Vec2.f_82462_,
                                                                     _level,
                                                                     4,
                                                                     "",
                                                                     Component.m_237113_(""),
                                                                     _level.m_7654_(),
                                                                     null
                                                                  )
                                                                  .m_81324_(),
                                                               "summon apollyon:the_damned"
                                                            );
                                                      }
                                                   } else {
                                                      if (world instanceof ServerLevel _level) {
                                                         _level.m_7654_()
                                                            .m_129892_()
                                                            .m_230957_(
                                                               new CommandSourceStack(
                                                                     CommandSource.f_80164_,
                                                                     new Vec3(
                                                                        entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0),
                                                                        entity.m_20186_(),
                                                                        entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0)
                                                                     ),
                                                                     Vec2.f_82462_,
                                                                     _level,
                                                                     4,
                                                                     "",
                                                                     Component.m_237113_(""),
                                                                     _level.m_7654_(),
                                                                     null
                                                                  )
                                                                  .m_81324_(),
                                                               "summon apollyon:the_damned"
                                                            );
                                                      }

                                                      if (world instanceof ServerLevel _level) {
                                                         _level.m_7654_()
                                                            .m_129892_()
                                                            .m_230957_(
                                                               new CommandSourceStack(
                                                                     CommandSource.f_80164_,
                                                                     new Vec3(x, y, z),
                                                                     Vec2.f_82462_,
                                                                     _level,
                                                                     4,
                                                                     "",
                                                                     Component.m_237113_(""),
                                                                     _level.m_7654_(),
                                                                     null
                                                                  )
                                                                  .m_81324_(),
                                                               "playsound apollyon:quieter_spawn ambient @a ~ ~ ~ 100"
                                                            );
                                                      }
                                                   }
                                                }
                                             }
                                          );
                                       }

                                       ApollyonMod.queueServerWork(
                                          (int)Mth.m_216263_(RandomSource.m_216327_(), 1200.0, 1800.0),
                                          () -> {
                                             if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 3.0) {
                                                if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0) <= 4.0) {
                                                   ApollyonModVariables.WorldVariables.get(world).damned_fakeout = true;
                                                   ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                                } else {
                                                   ApollyonModVariables.WorldVariables.get(world).damned_fakeout = false;
                                                   ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                                }

                                                ApollyonMod.queueServerWork(
                                                   20,
                                                   () -> {
                                                      boolean var10000x;
                                                      label28: {
                                                         if (world instanceof Level _lvl77 && _lvl77.m_46461_()) {
                                                            var10000x = true;
                                                            break label28;
                                                         }

                                                         var10000x = false;
                                                      }

                                                      if (!var10000x) {
                                                         if (ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
                                                            if (world instanceof ServerLevel _level) {
                                                               _level.m_7654_()
                                                                  .m_129892_()
                                                                  .m_230957_(
                                                                     new CommandSourceStack(
                                                                           CommandSource.f_80164_,
                                                                           new Vec3(
                                                                              entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0),
                                                                              entity.m_20186_(),
                                                                              entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0)
                                                                           ),
                                                                           Vec2.f_82462_,
                                                                           _level,
                                                                           4,
                                                                           "",
                                                                           Component.m_237113_(""),
                                                                           _level.m_7654_(),
                                                                           null
                                                                        )
                                                                        .m_81324_(),
                                                                     "summon apollyon:the_damned"
                                                                  );
                                                            }
                                                         } else {
                                                            if (world instanceof ServerLevel _level) {
                                                               _level.m_7654_()
                                                                  .m_129892_()
                                                                  .m_230957_(
                                                                     new CommandSourceStack(
                                                                           CommandSource.f_80164_,
                                                                           new Vec3(
                                                                              entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0),
                                                                              entity.m_20186_(),
                                                                              entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0)
                                                                           ),
                                                                           Vec2.f_82462_,
                                                                           _level,
                                                                           4,
                                                                           "",
                                                                           Component.m_237113_(""),
                                                                           _level.m_7654_(),
                                                                           null
                                                                        )
                                                                        .m_81324_(),
                                                                     "summon apollyon:the_damned"
                                                                  );
                                                            }

                                                            if (world instanceof ServerLevel _level) {
                                                               _level.m_7654_()
                                                                  .m_129892_()
                                                                  .m_230957_(
                                                                     new CommandSourceStack(
                                                                           CommandSource.f_80164_,
                                                                           new Vec3(x, y, z),
                                                                           Vec2.f_82462_,
                                                                           _level,
                                                                           4,
                                                                           "",
                                                                           Component.m_237113_(""),
                                                                           _level.m_7654_(),
                                                                           null
                                                                        )
                                                                        .m_81324_(),
                                                                     "playsound apollyon:quieter_spawn ambient @a ~ ~ ~ 100"
                                                                  );
                                                            }
                                                         }
                                                      }
                                                   }
                                                );
                                             }

                                             ApollyonMod.queueServerWork(
                                                (int)Mth.m_216263_(RandomSource.m_216327_(), 1200.0, 1800.0),
                                                () -> {
                                                   if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 2.0) {
                                                      if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0) <= 4.0) {
                                                         ApollyonModVariables.WorldVariables.get(world).damned_fakeout = true;
                                                         ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                                      } else {
                                                         ApollyonModVariables.WorldVariables.get(world).damned_fakeout = false;
                                                         ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                                      }

                                                      ApollyonMod.queueServerWork(
                                                         20,
                                                         () -> {
                                                            boolean var10000x;
                                                            label28: {
                                                               if (world instanceof Level _lvl95 && _lvl95.m_46461_()) {
                                                                  var10000x = true;
                                                                  break label28;
                                                               }

                                                               var10000x = false;
                                                            }

                                                            if (!var10000x) {
                                                               if (ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
                                                                  if (world instanceof ServerLevel _level) {
                                                                     _level.m_7654_()
                                                                        .m_129892_()
                                                                        .m_230957_(
                                                                           new CommandSourceStack(
                                                                                 CommandSource.f_80164_,
                                                                                 new Vec3(
                                                                                    entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0),
                                                                                    entity.m_20186_(),
                                                                                    entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0)
                                                                                 ),
                                                                                 Vec2.f_82462_,
                                                                                 _level,
                                                                                 4,
                                                                                 "",
                                                                                 Component.m_237113_(""),
                                                                                 _level.m_7654_(),
                                                                                 null
                                                                              )
                                                                              .m_81324_(),
                                                                           "summon apollyon:the_damned"
                                                                        );
                                                                  }
                                                               } else {
                                                                  if (world instanceof ServerLevel _level) {
                                                                     _level.m_7654_()
                                                                        .m_129892_()
                                                                        .m_230957_(
                                                                           new CommandSourceStack(
                                                                                 CommandSource.f_80164_,
                                                                                 new Vec3(
                                                                                    entity.m_20185_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0),
                                                                                    entity.m_20186_(),
                                                                                    entity.m_20189_() + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0)
                                                                                 ),
                                                                                 Vec2.f_82462_,
                                                                                 _level,
                                                                                 4,
                                                                                 "",
                                                                                 Component.m_237113_(""),
                                                                                 _level.m_7654_(),
                                                                                 null
                                                                              )
                                                                              .m_81324_(),
                                                                           "summon apollyon:the_damned"
                                                                        );
                                                                  }

                                                                  if (world instanceof ServerLevel _level) {
                                                                     _level.m_7654_()
                                                                        .m_129892_()
                                                                        .m_230957_(
                                                                           new CommandSourceStack(
                                                                                 CommandSource.f_80164_,
                                                                                 new Vec3(x, y, z),
                                                                                 Vec2.f_82462_,
                                                                                 _level,
                                                                                 4,
                                                                                 "",
                                                                                 Component.m_237113_(""),
                                                                                 _level.m_7654_(),
                                                                                 null
                                                                              )
                                                                              .m_81324_(),
                                                                           "playsound apollyon:quieter_spawn ambient @a ~ ~ ~ 100"
                                                                        );
                                                                  }
                                                               }
                                                            }
                                                         }
                                                      );
                                                   }

                                                   ApollyonMod.queueServerWork(
                                                      (int)Mth.m_216263_(RandomSource.m_216327_(), 1200.0, 1800.0),
                                                      () -> {
                                                         boolean var10000x;
                                                         label23: {
                                                            if (world instanceof Level _lvl111 && _lvl111.m_46461_()) {
                                                               var10000x = true;
                                                               break label23;
                                                            }

                                                            var10000x = false;
                                                         }

                                                         if (!var10000x) {
                                                            ApollyonModVariables.WorldVariables.get(world).damned_spawning = "Loop";
                                                            ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                                            if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 2.0) {
                                                               if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 5.0) <= 4.0) {
                                                                  ApollyonModVariables.WorldVariables.get(world).damned_fakeout = true;
                                                                  ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                                               } else {
                                                                  ApollyonModVariables.WorldVariables.get(world).damned_fakeout = false;
                                                                  ApollyonModVariables.WorldVariables.get(world).syncData(world);
                                                               }

                                                               ApollyonMod.queueServerWork(
                                                                  20,
                                                                  () -> {
                                                                     boolean var10000xx;
                                                                     label28: {
                                                                        if (world instanceof Level _lvl114 && _lvl114.m_46461_()) {
                                                                           var10000xx = true;
                                                                           break label28;
                                                                        }

                                                                        var10000xx = false;
                                                                     }

                                                                     if (!var10000xx) {
                                                                        if (ApollyonModVariables.WorldVariables.get(world).damned_fakeout) {
                                                                           if (world instanceof ServerLevel _level) {
                                                                              _level.m_7654_()
                                                                                 .m_129892_()
                                                                                 .m_230957_(
                                                                                    new CommandSourceStack(
                                                                                          CommandSource.f_80164_,
                                                                                          new Vec3(
                                                                                             entity.m_20185_()
                                                                                                + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0),
                                                                                             entity.m_20186_(),
                                                                                             entity.m_20189_()
                                                                                                + Mth.m_216263_(RandomSource.m_216327_(), -15.0, 15.0)
                                                                                          ),
                                                                                          Vec2.f_82462_,
                                                                                          _level,
                                                                                          4,
                                                                                          "",
                                                                                          Component.m_237113_(""),
                                                                                          _level.m_7654_(),
                                                                                          null
                                                                                       )
                                                                                       .m_81324_(),
                                                                                    "summon apollyon:the_damned"
                                                                                 );
                                                                           }
                                                                        } else {
                                                                           if (world instanceof ServerLevel _level) {
                                                                              _level.m_7654_()
                                                                                 .m_129892_()
                                                                                 .m_230957_(
                                                                                    new CommandSourceStack(
                                                                                          CommandSource.f_80164_,
                                                                                          new Vec3(
                                                                                             entity.m_20185_()
                                                                                                + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0),
                                                                                             entity.m_20186_(),
                                                                                             entity.m_20189_()
                                                                                                + Mth.m_216263_(RandomSource.m_216327_(), -50.0, 50.0)
                                                                                          ),
                                                                                          Vec2.f_82462_,
                                                                                          _level,
                                                                                          4,
                                                                                          "",
                                                                                          Component.m_237113_(""),
                                                                                          _level.m_7654_(),
                                                                                          null
                                                                                       )
                                                                                       .m_81324_(),
                                                                                    "summon apollyon:the_damned"
                                                                                 );
                                                                           }

                                                                           if (world instanceof ServerLevel _level) {
                                                                              _level.m_7654_()
                                                                                 .m_129892_()
                                                                                 .m_230957_(
                                                                                    new CommandSourceStack(
                                                                                          CommandSource.f_80164_,
                                                                                          new Vec3(x, y, z),
                                                                                          Vec2.f_82462_,
                                                                                          _level,
                                                                                          4,
                                                                                          "",
                                                                                          Component.m_237113_(""),
                                                                                          _level.m_7654_(),
                                                                                          null
                                                                                       )
                                                                                       .m_81324_(),
                                                                                    "playsound apollyon:quieter_spawn ambient @a ~ ~ ~ 100"
                                                                                 );
                                                                           }
                                                                        }
                                                                     }
                                                                  }
                                                               );
                                                            }
                                                         }
                                                      }
                                                   );
                                                }
                                             );
                                          }
                                       );
                                    }
                                 );
                              }
                           );
                        }
                     );
                  }
               );
            }

            label27: {
               if (world instanceof Level _lvl136 && _lvl136.m_46461_()) {
                  var10000 = true;
                  break label27;
               }

               var10000 = false;
            }

            if (var10000) {
               ApollyonModVariables.WorldVariables.get(world).damned_spawning = "False";
               ApollyonModVariables.WorldVariables.get(world).syncData(world);
            }
         }
      }
   }
}
