package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.entity.TheDamnedEntity;
import apollyon.network.ApollyonModVariables;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
public class TheDamnedTargetsProcedure {
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
         if (sourceentity instanceof TheDamnedEntity
            && (entity instanceof Player || entity instanceof ServerPlayer)
            && !ApollyonModVariables.WorldVariables.get(world).damned_fakeout
            && !sourceentity.getPersistentData().m_128471_("targetingPlayer")) {
            sourceentity.getPersistentData().m_128379_("targetingPlayer", true);
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
                     "playsound apollyon:cutscene_rise ambient @p[distance=..50]"
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
                     "effect give @e[distance=..50] slowness 6 255"
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
                     "effect give @e[distance=..50] weakness 6 255"
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
                     "tp @e[type=apollyon:the_damned,nbt={ForgeData:{targetingPlayer: 0b}}] ~ ~-1000 ~"
                  );
            }

            if (sourceentity instanceof TheDamnedEntity) {
               ((TheDamnedEntity)sourceentity).setAnimation("animation.theDamned.spotted");
            }

            sourceentity.m_7618_(Anchor.EYES, new Vec3(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()));
            ApollyonMod.queueServerWork(
               110,
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
                           "effect give @p darkness 3 255"
                        );
                  }

                  ApollyonMod.queueServerWork(
                     10,
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
                                 "effect give @s invisibility 100000 255"
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
                                 "effect give @s weakness 100000 255"
                              );
                        }

                        if (sourceentity instanceof Mob _entity) {
                           _entity.m_21573_().m_26519_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), 2.0);
                        }
                     }
                  );
                  ApollyonMod.queueServerWork(
                     100,
                     () -> {
                        if (sourceentity.m_20145_() && !sourceentity.m_9236_().m_5776_() && sourceentity.m_20194_() != null) {
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
                                 "tp @s @p"
                              );
                        }
                     }
                  );
               }
            );
         }
      }
   }
}
