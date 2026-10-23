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
import net.minecraft.server.level.ServerPlayer;
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
public class TheFallenRisesProcedure {
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
         if (world.m_204166_(BlockPos.m_274561_(x, y, z)).m_203656_(TagKey.m_203882_(Registries.f_256952_, new ResourceLocation("minecraft:is_overworld")))
            && entity instanceof ServerPlayer _plr1
            && _plr1.m_9236_() instanceof ServerLevel
            && _plr1.m_8960_().m_135996_(_plr1.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:apollyon"))).m_8193_()) {
            boolean var10000;
            label27: {
               if (world instanceof Level _lvl2 && _lvl2.m_46461_()) {
                  var10000 = true;
                  break label27;
               }

               var10000 = false;
            }

            if (!var10000) {
               if (!ApollyonModVariables.WorldVariables.get(world).fallen_spawning) {
                  ApollyonModVariables.WorldVariables.get(world).fallen_spawning = true;
                  ApollyonModVariables.WorldVariables.get(world).syncData(world);
                  ApollyonMod.queueServerWork(
                     (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                     () -> {
                        if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 4.0) {
                           boolean var10000x;
                           label18: {
                              if (world instanceof Level _lvl5 && _lvl5.m_46461_()) {
                                 var10000x = true;
                                 break label18;
                              }

                              var10000x = false;
                           }

                           if (!var10000x && world instanceof ServerLevel _level) {
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
                                    "summon apollyon:the_fallen"
                                 );
                           }
                        }

                        ApollyonMod.queueServerWork(
                           (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                           () -> {
                              if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 3.0) {
                                 boolean var10000xx;
                                 label18: {
                                    if (world instanceof Level _lvl14 && _lvl14.m_46461_()) {
                                       var10000xx = true;
                                       break label18;
                                    }

                                    var10000xx = false;
                                 }

                                 if (!var10000xx && world instanceof ServerLevel _levelx) {
                                    _levelx.m_7654_()
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
                                                _levelx,
                                                4,
                                                "",
                                                Component.m_237113_(""),
                                                _levelx.m_7654_(),
                                                null
                                             )
                                             .m_81324_(),
                                          "summon apollyon:the_fallen"
                                       );
                                 }
                              }

                              ApollyonMod.queueServerWork(
                                 (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                                 () -> {
                                    if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 4.0) {
                                       boolean var10000xxx;
                                       label18: {
                                          if (world instanceof Level _lvl23 && _lvl23.m_46461_()) {
                                             var10000xxx = true;
                                             break label18;
                                          }

                                          var10000xxx = false;
                                       }

                                       if (!var10000xxx && world instanceof ServerLevel _levelxx) {
                                          _levelxx.m_7654_()
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
                                                      _levelxx,
                                                      4,
                                                      "",
                                                      Component.m_237113_(""),
                                                      _levelxx.m_7654_(),
                                                      null
                                                   )
                                                   .m_81324_(),
                                                "summon apollyon:the_fallen"
                                             );
                                       }
                                    }

                                    ApollyonMod.queueServerWork(
                                       (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                                       () -> {
                                          if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 3.0) {
                                             boolean var10000xxxx;
                                             label18: {
                                                if (world instanceof Level _lvl32 && _lvl32.m_46461_()) {
                                                   var10000xxxx = true;
                                                   break label18;
                                                }

                                                var10000xxxx = false;
                                             }

                                             if (!var10000xxxx && world instanceof ServerLevel _levelxxx) {
                                                _levelxxx.m_7654_()
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
                                                            _levelxxx,
                                                            4,
                                                            "",
                                                            Component.m_237113_(""),
                                                            _levelxxx.m_7654_(),
                                                            null
                                                         )
                                                         .m_81324_(),
                                                      "summon apollyon:the_fallen"
                                                   );
                                             }
                                          }

                                          ApollyonMod.queueServerWork(
                                             (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                                             () -> {
                                                if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 4.0) {
                                                   boolean var10000xxxxx;
                                                   label18: {
                                                      if (world instanceof Level _lvl41 && _lvl41.m_46461_()) {
                                                         var10000xxxxx = true;
                                                         break label18;
                                                      }

                                                      var10000xxxxx = false;
                                                   }

                                                   if (!var10000xxxxx && world instanceof ServerLevel _levelxxxx) {
                                                      _levelxxxx.m_7654_()
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
                                                                  _levelxxxx,
                                                                  4,
                                                                  "",
                                                                  Component.m_237113_(""),
                                                                  _levelxxxx.m_7654_(),
                                                                  null
                                                               )
                                                               .m_81324_(),
                                                            "summon apollyon:the_fallen"
                                                         );
                                                   }
                                                }

                                                ApollyonMod.queueServerWork(
                                                   (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                                                   () -> {
                                                      if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 3.0) {
                                                         boolean var10000xxxxxx;
                                                         label18: {
                                                            if (world instanceof Level _lvl50 && _lvl50.m_46461_()) {
                                                               var10000xxxxxx = true;
                                                               break label18;
                                                            }

                                                            var10000xxxxxx = false;
                                                         }

                                                         if (!var10000xxxxxx && world instanceof ServerLevel _levelxxxxx) {
                                                            _levelxxxxx.m_7654_()
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
                                                                        _levelxxxxx,
                                                                        4,
                                                                        "",
                                                                        Component.m_237113_(""),
                                                                        _levelxxxxx.m_7654_(),
                                                                        null
                                                                     )
                                                                     .m_81324_(),
                                                                  "summon apollyon:the_fallen"
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
            } else if (ApollyonModVariables.WorldVariables.get(world).fallen_spawning) {
               ApollyonModVariables.WorldVariables.get(world).fallen_spawning = false;
               ApollyonModVariables.WorldVariables.get(world).syncData(world);
               ApollyonMod.queueServerWork(
                  (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                  () -> {
                     if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 4.0) {
                        boolean var10000x;
                        label18: {
                           if (world instanceof Level _lvl65 && _lvl65.m_46461_()) {
                              var10000x = true;
                              break label18;
                           }

                           var10000x = false;
                        }

                        if (!var10000x && world instanceof ServerLevel _level) {
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
                                 "summon apollyon:the_fallen"
                              );
                        }
                     }

                     ApollyonMod.queueServerWork(
                        (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                        () -> {
                           if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 3.0) {
                              boolean var10000xx;
                              label18: {
                                 if (world instanceof Level _lvl74 && _lvl74.m_46461_()) {
                                    var10000xx = true;
                                    break label18;
                                 }

                                 var10000xx = false;
                              }

                              if (!var10000xx && world instanceof ServerLevel _levelx) {
                                 _levelx.m_7654_()
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
                                             _levelx,
                                             4,
                                             "",
                                             Component.m_237113_(""),
                                             _levelx.m_7654_(),
                                             null
                                          )
                                          .m_81324_(),
                                       "summon apollyon:the_fallen"
                                    );
                              }
                           }

                           ApollyonMod.queueServerWork(
                              (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                              () -> {
                                 if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 4.0) {
                                    boolean var10000xxx;
                                    label18: {
                                       if (world instanceof Level _lvl83 && _lvl83.m_46461_()) {
                                          var10000xxx = true;
                                          break label18;
                                       }

                                       var10000xxx = false;
                                    }

                                    if (!var10000xxx && world instanceof ServerLevel _levelxx) {
                                       _levelxx.m_7654_()
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
                                                   _levelxx,
                                                   4,
                                                   "",
                                                   Component.m_237113_(""),
                                                   _levelxx.m_7654_(),
                                                   null
                                                )
                                                .m_81324_(),
                                             "summon apollyon:the_fallen"
                                          );
                                    }
                                 }

                                 ApollyonMod.queueServerWork(
                                    (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                                    () -> {
                                       if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 3.0) {
                                          boolean var10000xxxx;
                                          label18: {
                                             if (world instanceof Level _lvl92 && _lvl92.m_46461_()) {
                                                var10000xxxx = true;
                                                break label18;
                                             }

                                             var10000xxxx = false;
                                          }

                                          if (!var10000xxxx && world instanceof ServerLevel _levelxxx) {
                                             _levelxxx.m_7654_()
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
                                                         _levelxxx,
                                                         4,
                                                         "",
                                                         Component.m_237113_(""),
                                                         _levelxxx.m_7654_(),
                                                         null
                                                      )
                                                      .m_81324_(),
                                                   "summon apollyon:the_fallen"
                                                );
                                          }
                                       }

                                       ApollyonMod.queueServerWork(
                                          (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                                          () -> {
                                             if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 4.0) {
                                                boolean var10000xxxxx;
                                                label18: {
                                                   if (world instanceof Level _lvl101 && _lvl101.m_46461_()) {
                                                      var10000xxxxx = true;
                                                      break label18;
                                                   }

                                                   var10000xxxxx = false;
                                                }

                                                if (!var10000xxxxx && world instanceof ServerLevel _levelxxxx) {
                                                   _levelxxxx.m_7654_()
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
                                                               _levelxxxx,
                                                               4,
                                                               "",
                                                               Component.m_237113_(""),
                                                               _levelxxxx.m_7654_(),
                                                               null
                                                            )
                                                            .m_81324_(),
                                                         "summon apollyon:the_fallen"
                                                      );
                                                }
                                             }

                                             ApollyonMod.queueServerWork(
                                                (int)Mth.m_216263_(RandomSource.m_216327_(), 1800.0, 2400.0),
                                                () -> {
                                                   if (Mth.m_216263_(RandomSource.m_216327_(), 1.0, 10.0) < 3.0) {
                                                      boolean var10000xxxxxx;
                                                      label18: {
                                                         if (world instanceof Level _lvl110 && _lvl110.m_46461_()) {
                                                            var10000xxxxxx = true;
                                                            break label18;
                                                         }

                                                         var10000xxxxxx = false;
                                                      }

                                                      if (!var10000xxxxxx && world instanceof ServerLevel _levelxxxxx) {
                                                         _levelxxxxx.m_7654_()
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
                                                                     _levelxxxxx,
                                                                     4,
                                                                     "",
                                                                     Component.m_237113_(""),
                                                                     _levelxxxxx.m_7654_(),
                                                                     null
                                                                  )
                                                                  .m_81324_(),
                                                               "summon apollyon:the_fallen"
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
         }
      }
   }
}
