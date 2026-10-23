package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.entity.TheInfectorEntity;
import apollyon.network.ApollyonModVariables;
import javax.annotation.Nullable;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TheInfectorTargetsProcedure {
   @SubscribeEvent
   public static void onEntitySetsAttackTarget(LivingChangeTargetEvent event) {
      execute(
         event,
         event.getEntity().m_9236_(),
         event.getEntity().m_20185_(),
         event.getEntity().m_20186_(),
         event.getEntity().m_20189_(),
         event.getOriginalTarget(),
         event.getEntity()
      );
   }

   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      execute(null, world, x, y, z, entity, sourceentity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      if (entity != null && sourceentity != null) {
         if (sourceentity instanceof TheInfectorEntity
            && (entity instanceof Player || entity instanceof ServerPlayer)
            && !sourceentity.getPersistentData().m_128471_("risen")) {
            sourceentity.getPersistentData().m_128379_("risen", true);
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
                     "stopsound @p music"
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
                     "stopsound @a ambient apollyon:light_suspense"
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
                     "effect give @e[distance=..50] slowness 5 255"
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
                     "effect give @e[distance=..50] weakness 5 255"
                  );
            }

            if (sourceentity instanceof TheInfectorEntity) {
               ((TheInfectorEntity)sourceentity).setAnimation("animation.theInfector.rise");
            }

            sourceentity.m_7618_(Anchor.EYES, new Vec3(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()));
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
                     "playsound apollyon:intense_rise ambient @a[distance=..50]"
                  );
            }

            ApollyonMod.queueServerWork(
               40,
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
                           "playsound apollyon:gutteral_roar ambient @a[distance=..50]"
                        );
                  }

                  ApollyonMod.queueServerWork(
                     50,
                     () -> {
                        if (ApollyonModVariables.WorldVariables.get(world).chase_music && !entity.m_9236_().m_5776_() && entity.m_20194_() != null) {
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
                                 "playsound apollyon:gateway_pursuit ambient @a[distance=..50]"
                              );
                        }

                        sourceentity.getPersistentData().m_128379_("risen", true);
                        sourceentity.m_20260_(false);
                        if (sourceentity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                           _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 60, 3, false, false));
                        }

                        if (sourceentity instanceof Mob _entity) {
                           _entity.m_21573_().m_26519_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), 1.0);
                        }

                        ApollyonMod.queueServerWork(
                           (int)Mth.m_216263_(RandomSource.m_216327_(), 400.0, 800.0),
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
                                       "advancement grant @a only apollyon:it_has_begun"
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
                                       "stopsound @a ambient apollyon:gateway_pursuit"
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
                                       "playsound apollyon:hallucination_sound ambient @a[distance=..50]"
                                    );
                              }

                              if (!sourceentity.m_9236_().m_5776_()) {
                                 sourceentity.m_146870_();
                              }

                              ApollyonMod.queueServerWork(
                                 20,
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
                                             "playsound apollyon:deep_laugh ambient @a[distance..50]"
                                          );
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
      }
   }
}
