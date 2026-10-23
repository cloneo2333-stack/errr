package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.entity.TheDamnedEntity;
import apollyon.entity.TheInfectorEntity;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TheDamnedKillsProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      if (entity != null && sourceentity != null) {
         if (entity instanceof Player || entity instanceof ServerPlayer) {
            if (sourceentity instanceof TheDamnedEntity) {
               if (world instanceof ServerLevel _level) {
                  _level.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                           )
                           .m_81324_(),
                        "stopsound @a ambient apollyon:chase_music"
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
                        "playsound apollyon:jumpscare_sound ambient @s"
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
                        "playsound apollyon:hallucination_sound ambient @s"
                     );
               }

               if (entity instanceof ServerPlayer _player) {
                  Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:condemned_to_die"));
                  AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
                  if (!_ap.m_8193_()) {
                     for (String criteria : _ap.m_8219_()) {
                        _player.m_8960_().m_135988_(_adv, criteria);
                     }
                  }
               }

               if (!sourceentity.m_9236_().m_5776_()) {
                  sourceentity.m_146870_();
               }
            } else if (sourceentity instanceof TheInfectorEntity) {
               if (world instanceof ServerLevel _level) {
                  _level.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                           )
                           .m_81324_(),
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
                        "playsound apollyon:jumpscare_sound ambient @s"
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
                        "playsound apollyon:hallucination_sound ambient @s"
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

               if (entity instanceof ServerPlayer _playerx) {
                  Advancement _adv = _playerx.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:condemned_to_die"));
                  AdvancementProgress _ap = _playerx.m_8960_().m_135996_(_adv);
                  if (!_ap.m_8193_()) {
                     for (String criteria : _ap.m_8219_()) {
                        _playerx.m_8960_().m_135988_(_adv, criteria);
                     }
                  }
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
                              "playsound apollyon:deep_laugh ambient @a[distance=..50]"
                           );
                     }
                  }
               );
            }
         }
      }
   }
}
