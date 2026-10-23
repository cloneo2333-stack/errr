package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.entity.TheInfectorEntity;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;

@EventBusSubscriber
public class TheInfectorTalksProcedure {
   @SubscribeEvent
   public static void onRightClickEntity(EntityInteract event) {
      if (event.getHand() == event.getEntity().m_7655_()) {
         execute(
            event,
            event.getLevel(),
            (double)event.getPos().m_123341_(),
            (double)event.getPos().m_123342_(),
            (double)event.getPos().m_123343_(),
            event.getTarget(),
            event.getEntity()
         );
      }
   }

   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      execute(null, world, x, y, z, entity, sourceentity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      if (entity != null && sourceentity != null) {
         if (entity instanceof GeoEntity && entity.getPersistentData().m_128471_("neutral") && !entity.getPersistentData().m_128471_("talking")) {
            if (entity.getPersistentData().m_128459_("dialogueProgression") == 1.0) {
               entity.getPersistentData().m_128379_("talking", true);
               if (!world.m_5776_() && world.m_7654_() != null) {
                  world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> You shouldn't be here."), false);
               }

               if (world instanceof Level _level) {
                  if (!_level.m_5776_()) {
                     _level.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _level.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               ApollyonMod.queueServerWork(
                  40,
                  () -> {
                     if (!world.m_5776_() && world.m_7654_() != null) {
                        world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> I am to succumb to this infection soon."), false);
                     }

                     if (world instanceof Level _levelx) {
                        if (!_levelx.m_5776_()) {
                           _levelx.m_5594_(
                              null,
                              BlockPos.m_274561_(x, y, z),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _levelx.m_7785_(
                              x,
                              y,
                              z,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     ApollyonMod.queueServerWork(70, () -> {
                        entity.getPersistentData().m_128347_("dialogueProgression", entity.getPersistentData().m_128459_("dialogueProgression") + 1.0);
                        entity.getPersistentData().m_128379_("talking", false);
                     });
                  }
               );
            } else if (entity.getPersistentData().m_128459_("dialogueProgression") == 2.0) {
               entity.getPersistentData().m_128379_("talking", true);
               if (!world.m_5776_() && world.m_7654_() != null) {
                  world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> Persistent, aren't you?"), false);
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.m_5776_()) {
                     _levelx.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelx.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               ApollyonMod.queueServerWork(
                  40,
                  () -> {
                     if (!world.m_5776_() && world.m_7654_() != null) {
                        world.m_7654_()
                           .m_6846_()
                           .m_240416_(Component.m_237113_("<Villager #32> Well, if you want to hear how I ended up like this, then stay."), false);
                     }

                     if (world instanceof Level _levelxx) {
                        if (!_levelxx.m_5776_()) {
                           _levelxx.m_5594_(
                              null,
                              BlockPos.m_274561_(x, y, z),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _levelxx.m_7785_(
                              x,
                              y,
                              z,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     ApollyonMod.queueServerWork(
                        80,
                        () -> {
                           if (!world.m_5776_() && world.m_7654_() != null) {
                              world.m_7654_()
                                 .m_6846_()
                                 .m_240416_(Component.m_237113_("<Villager #32> But if this infection takes me over, I want you to kill me."), false);
                           }

                           if (world instanceof Level _levelxxx) {
                              if (!_levelxxx.m_5776_()) {
                                 _levelxxx.m_5594_(
                                    null,
                                    BlockPos.m_274561_(x, y, z),
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F
                                 );
                              } else {
                                 _levelxxx.m_7785_(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }

                           ApollyonMod.queueServerWork(80, () -> {
                              entity.getPersistentData().m_128347_("dialogueProgression", entity.getPersistentData().m_128459_("dialogueProgression") + 1.0);
                              entity.getPersistentData().m_128379_("talking", false);
                           });
                        }
                     );
                  }
               );
            } else if (entity.getPersistentData().m_128459_("dialogueProgression") == 3.0) {
               entity.getPersistentData().m_128379_("talking", true);
               if (!world.m_5776_() && world.m_7654_() != null) {
                  world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> Oh well, alright."), false);
               }

               if (world instanceof Level _levelxx) {
                  if (!_levelxx.m_5776_()) {
                     _levelxx.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelxx.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               ApollyonMod.queueServerWork(
                  40,
                  () -> {
                     if (!world.m_5776_() && world.m_7654_() != null) {
                        world.m_7654_()
                           .m_6846_()
                           .m_240416_(Component.m_237113_("<Villager #32> Not very long ago, there was another player, just like you."), false);
                     }

                     if (world instanceof Level _levelxxx) {
                        if (!_levelxxx.m_5776_()) {
                           _levelxxx.m_5594_(
                              null,
                              BlockPos.m_274561_(x, y, z),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _levelxxx.m_7785_(
                              x,
                              y,
                              z,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     ApollyonMod.queueServerWork(
                        80,
                        () -> {
                           if (!world.m_5776_() && world.m_7654_() != null) {
                              world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> I gave him the name \"Villager #1202\"."), false);
                           }

                           if (world instanceof Level _levelxxxx) {
                              if (!_levelxxxx.m_5776_()) {
                                 _levelxxxx.m_5594_(
                                    null,
                                    BlockPos.m_274561_(x, y, z),
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F
                                 );
                              } else {
                                 _levelxxxx.m_7785_(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }

                           ApollyonMod.queueServerWork(
                              100,
                              () -> {
                                 if (!world.m_5776_() && world.m_7654_() != null) {
                                    world.m_7654_()
                                       .m_6846_()
                                       .m_240416_(Component.m_237113_("<Villager #32> But I quicky found that he was... different."), false);
                                 }

                                 if (world instanceof Level _levelxxxxx) {
                                    if (!_levelxxxxx.m_5776_()) {
                                       _levelxxxxx.m_5594_(
                                          null,
                                          BlockPos.m_274561_(x, y, z),
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F
                                       );
                                    } else {
                                       _levelxxxxx.m_7785_(
                                          x,
                                          y,
                                          z,
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F,
                                          false
                                       );
                                    }
                                 }

                                 ApollyonMod.queueServerWork(100, () -> {
                                    entity.getPersistentData()
                                       .m_128347_("dialogueProgression", entity.getPersistentData().m_128459_("dialogueProgression") + 1.0);
                                    entity.getPersistentData().m_128379_("talking", false);
                                 });
                              }
                           );
                        }
                     );
                  }
               );
            } else if (entity.getPersistentData().m_128459_("dialogueProgression") == 4.0) {
               entity.getPersistentData().m_128379_("talking", true);
               if (world instanceof ServerLevel _levelxxx) {
                  _levelxxx.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _levelxxx, 4, "", Component.m_237113_(""), _levelxxx.m_7654_(), null
                           )
                           .m_81324_(),
                        "playsound apollyon:light_suspense ambient @a[distance=..50]"
                     );
               }

               if (!world.m_5776_() && world.m_7654_() != null) {
                  world.m_7654_()
                     .m_6846_()
                     .m_240416_(Component.m_237113_("<Villager #32> There would be times where he would smile so wide, it looked like it hurt."), false);
               }

               if (world instanceof Level _levelxxx) {
                  if (!_levelxxx.m_5776_()) {
                     _levelxxx.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelxxx.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               ApollyonMod.queueServerWork(
                  120,
                  () -> {
                     if (!world.m_5776_() && world.m_7654_() != null) {
                        world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> He would always stare into space, with manic eyes."), false);
                     }

                     if (world instanceof Level _levelxxxx) {
                        if (!_levelxxxx.m_5776_()) {
                           _levelxxxx.m_5594_(
                              null,
                              BlockPos.m_274561_(x, y, z),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _levelxxxx.m_7785_(
                              x,
                              y,
                              z,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     ApollyonMod.queueServerWork(
                        100,
                        () -> {
                           if (!world.m_5776_() && world.m_7654_() != null) {
                              world.m_7654_()
                                 .m_6846_()
                                 .m_240416_(
                                    Component.m_237113_("<Villager #32> I took him under my wing, and he became a member of the now abandoned Council."), false
                                 );
                           }

                           if (world instanceof Level _levelxxxxx) {
                              if (!_levelxxxxx.m_5776_()) {
                                 _levelxxxxx.m_5594_(
                                    null,
                                    BlockPos.m_274561_(x, y, z),
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F
                                 );
                              } else {
                                 _levelxxxxx.m_7785_(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }

                           ApollyonMod.queueServerWork(
                              140,
                              () -> {
                                 if (!world.m_5776_() && world.m_7654_() != null) {
                                    world.m_7654_()
                                       .m_6846_()
                                       .m_240416_(Component.m_237113_("<Villager #32> The same Council this building served its purpose to."), false);
                                 }

                                 if (world instanceof Level _levelxxxxxx) {
                                    if (!_levelxxxxxx.m_5776_()) {
                                       _levelxxxxxx.m_5594_(
                                          null,
                                          BlockPos.m_274561_(x, y, z),
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F
                                       );
                                    } else {
                                       _levelxxxxxx.m_7785_(
                                          x,
                                          y,
                                          z,
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F,
                                          false
                                       );
                                    }
                                 }

                                 ApollyonMod.queueServerWork(80, () -> {
                                    entity.getPersistentData()
                                       .m_128347_("dialogueProgression", entity.getPersistentData().m_128459_("dialogueProgression") + 1.0);
                                    entity.getPersistentData().m_128379_("talking", false);
                                 });
                              }
                           );
                        }
                     );
                  }
               );
            } else if (entity.getPersistentData().m_128459_("dialogueProgression") == 5.0) {
               entity.getPersistentData().m_128379_("talking", true);
               if (!world.m_5776_() && world.m_7654_() != null) {
                  world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> *Cough, cough*"), false);
               }

               if (world instanceof Level _levelxxxx) {
                  if (!_levelxxxx.m_5776_()) {
                     _levelxxxx.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelxxxx.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               ApollyonMod.queueServerWork(
                  50,
                  () -> {
                     if (!world.m_5776_() && world.m_7654_() != null) {
                        world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> I grew suspicious of him."), false);
                     }

                     if (world instanceof Level _levelxxxxx) {
                        if (!_levelxxxxx.m_5776_()) {
                           _levelxxxxx.m_5594_(
                              null,
                              BlockPos.m_274561_(x, y, z),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _levelxxxxx.m_7785_(
                              x,
                              y,
                              z,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     ApollyonMod.queueServerWork(
                        40,
                        () -> {
                           if (!world.m_5776_() && world.m_7654_() != null) {
                              world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #32> His behavior only got worse, and one day..."), false);
                           }

                           if (world instanceof Level _levelxxxxxx) {
                              if (!_levelxxxxxx.m_5776_()) {
                                 _levelxxxxxx.m_5594_(
                                    null,
                                    BlockPos.m_274561_(x, y, z),
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F
                                 );
                              } else {
                                 _levelxxxxxx.m_7785_(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }

                           ApollyonMod.queueServerWork(
                              100,
                              () -> {
                                 if (!world.m_5776_() && world.m_7654_() != null) {
                                    world.m_7654_()
                                       .m_6846_()
                                       .m_240416_(Component.m_237113_("<Villager #32> He lured one of those... things... in here."), false);
                                 }

                                 if (world instanceof Level _levelxxxxxxx) {
                                    if (!_levelxxxxxxx.m_5776_()) {
                                       _levelxxxxxxx.m_5594_(
                                          null,
                                          BlockPos.m_274561_(x, y, z),
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F
                                       );
                                    } else {
                                       _levelxxxxxxx.m_7785_(
                                          x,
                                          y,
                                          z,
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F,
                                          false
                                       );
                                    }
                                 }

                                 ApollyonMod.queueServerWork(100, () -> {
                                    entity.getPersistentData()
                                       .m_128347_("dialogueProgression", entity.getPersistentData().m_128459_("dialogueProgression") + 1.0);
                                    entity.getPersistentData().m_128379_("talking", false);
                                 });
                              }
                           );
                        }
                     );
                  }
               );
            } else if (entity.getPersistentData().m_128459_("dialogueProgression") == 6.0) {
               entity.getPersistentData().m_128379_("dying", false);
               entity.getPersistentData().m_128379_("talking", true);
               if (!world.m_5776_() && world.m_7654_() != null) {
                  world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Villager #...or> Like he had full control over..."), false);
               }

               if (world instanceof Level _levelxxxxx) {
                  if (!_levelxxxxx.m_5776_()) {
                     _levelxxxxx.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelxxxxx.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               if (world instanceof Level _levelxxxxxx) {
                  if (!_levelxxxxxx.m_5776_()) {
                     _levelxxxxxx.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:infector_hurt")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _levelxxxxxx.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:infector_hurt")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               if (entity instanceof TheInfectorEntity) {
                  ((TheInfectorEntity)entity).setAnimation("animation.theInfector.twitchone");
               }

               ApollyonMod.queueServerWork(
                  60,
                  () -> {
                     if (!world.m_5776_() && world.m_7654_() != null) {
                        world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Village...ector> The pain is unbeara..."), false);
                     }

                     if (world instanceof Level _levelxxxxxxx) {
                        if (!_levelxxxxxxx.m_5776_()) {
                           _levelxxxxxxx.m_5594_(
                              null,
                              BlockPos.m_274561_(x, y, z),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _levelxxxxxxx.m_7785_(
                              x,
                              y,
                              z,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     if (world instanceof Level _levelxxxxxxxx) {
                        if (!_levelxxxxxxxx.m_5776_()) {
                           _levelxxxxxxxx.m_5594_(
                              null,
                              BlockPos.m_274561_(x, y, z),
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:infector_hurt")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F
                           );
                        } else {
                           _levelxxxxxxxx.m_7785_(
                              x,
                              y,
                              z,
                              (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:infector_hurt")),
                              SoundSource.HOSTILE,
                              1.0F,
                              1.0F,
                              false
                           );
                        }
                     }

                     if (entity instanceof TheInfectorEntity) {
                        ((TheInfectorEntity)entity).setAnimation("animation.theInfector.twitchtwo");
                     }

                     ApollyonMod.queueServerWork(
                        40,
                        () -> {
                           if (!world.m_5776_() && world.m_7654_() != null) {
                              world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<Vill...Infector> You best be g..."), false);
                           }

                           if (world instanceof Level _levelxxxxxxxxx) {
                              if (!_levelxxxxxxxxx.m_5776_()) {
                                 _levelxxxxxxxxx.m_5594_(
                                    null,
                                    BlockPos.m_274561_(x, y, z),
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F
                                 );
                              } else {
                                 _levelxxxxxxxxx.m_7785_(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }

                           if (world instanceof Level _levelxxxxxxxxxx) {
                              if (!_levelxxxxxxxxxx.m_5776_()) {
                                 _levelxxxxxxxxxx.m_5594_(
                                    null,
                                    BlockPos.m_274561_(x, y, z),
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:infector_hurt")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F
                                 );
                              } else {
                                 _levelxxxxxxxxxx.m_7785_(
                                    x,
                                    y,
                                    z,
                                    (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:infector_hurt")),
                                    SoundSource.HOSTILE,
                                    1.0F,
                                    1.0F,
                                    false
                                 );
                              }
                           }

                           if (entity instanceof TheInfectorEntity) {
                              ((TheInfectorEntity)entity).setAnimation("animation.theInfector.twitchthree");
                           }

                           ApollyonMod.queueServerWork(
                              60,
                              () -> {
                                 if (!world.m_5776_() && world.m_7654_() != null) {
                                    world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<The Infector> It's time."), false);
                                 }

                                 if (world instanceof Level _levelxxxxxxxxxxx) {
                                    if (!_levelxxxxxxxxxxx.m_5776_()) {
                                       _levelxxxxxxxxxxx.m_5594_(
                                          null,
                                          BlockPos.m_274561_(x, y, z),
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F
                                       );
                                    } else {
                                       _levelxxxxxxxxxxx.m_7785_(
                                          x,
                                          y,
                                          z,
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:villager_dialogue")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F,
                                          false
                                       );
                                    }
                                 }

                                 if (world instanceof Level _levelxxxxxxxxxxxx) {
                                    if (!_levelxxxxxxxxxxxx.m_5776_()) {
                                       _levelxxxxxxxxxxxx.m_5594_(
                                          null,
                                          BlockPos.m_274561_(x, y, z),
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:infector_hurt")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F
                                       );
                                    } else {
                                       _levelxxxxxxxxxxxx.m_7785_(
                                          x,
                                          y,
                                          z,
                                          (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:infector_hurt")),
                                          SoundSource.HOSTILE,
                                          1.0F,
                                          1.0F,
                                          false
                                       );
                                    }
                                 }

                                 if (entity instanceof TheInfectorEntity) {
                                    ((TheInfectorEntity)entity).setAnimation("animation.theInfector.twitchfour");
                                 }

                                 ApollyonMod.queueServerWork(100, () -> {
                                    entity.getPersistentData()
                                       .m_128347_("dialogueProgression", entity.getPersistentData().m_128459_("dialogueProgression") + 1.0);
                                    entity.getPersistentData().m_128379_("talking", false);
                                    entity.getPersistentData().m_128379_("neutral", false);
                                    TheInfectorTargetsProcedure.execute(world, x, y, z, entity, sourceentity);
                                 });
                              }
                           );
                        }
                     );
                  }
               );
            } else {
               entity.getPersistentData().m_128379_("talking", true);
               entity.getPersistentData().m_128379_("neutral", false);
               if (!world.m_5776_() && world.m_7654_() != null) {
                  world.m_7654_().m_6846_().m_240416_(Component.m_237113_("<The Infector> You Should Have Listened."), false);
               }

               ApollyonMod.queueServerWork(1000, () -> entity.getPersistentData().m_128379_("talking", false));
            }
         }
      }
   }
}
