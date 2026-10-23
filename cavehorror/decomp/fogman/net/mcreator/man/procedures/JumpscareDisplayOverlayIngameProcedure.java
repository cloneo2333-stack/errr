package net.mcreator.man.procedures;

import java.util.Comparator;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.mcreator.man.configuration.ManconfigConfiguration;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.mcreator.man.init.ManModItems;
import net.mcreator.man.network.ManModVariables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber
public class JumpscareDisplayOverlayIngameProcedure {
   @SubscribeEvent
   public static void onEntityDeath(LivingDeathEvent event) {
      if (event != null && event.getEntity() != null) {
         execute(
            event,
            event.getEntity().m_9236_(),
            event.getEntity().m_20185_(),
            event.getEntity().m_20186_(),
            event.getEntity().m_20189_(),
            event.getEntity(),
            event.getSource().m_7639_()
         );
      }
   }

   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      execute(null, world, x, y, z, entity, sourceentity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
      if (entity != null && sourceentity != null) {
         ItemStack item = ItemStack.f_41583_;
         if (entity instanceof Player && sourceentity instanceof ManaggresiveEntity) {
            if ((Boolean)ManconfigConfiguration.JUMPSCARE.get()) {
               ManModVariables.MapVariables.get(world).overlay_ingame = 40.0;
               ManModVariables.MapVariables.get(world).syncData(world);
               if (sourceentity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 40, 10, false, false));
               }

               if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
                  _entity.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 100, 3, false, false));
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
                        "tp @e[type=man:managgresive] ^ ^ ^2"
                     );
               }

               sourceentity.m_7618_(Anchor.EYES, new Vec3(x, 1.5 + y, z));
               entity.m_7618_(Anchor.EYES, new Vec3(sourceentity.m_20185_(), 1.5 + sourceentity.m_20186_(), sourceentity.m_20189_()));
               if (sourceentity instanceof ManaggresiveEntity) {
                  ((ManaggresiveEntity)sourceentity).setAnimation("jumpscare");
               }

               if (world instanceof ServerLevel _level) {
                  _level.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                           )
                           .m_81324_(),
                        "stopsound @p"
                     );
               }

               if (world instanceof Level _level) {
                  if (!_level.m_5776_()) {
                     _level.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:jumpscare")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F
                     );
                  } else {
                     _level.m_7785_(
                        x,
                        y,
                        z,
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:jumpscare")),
                        SoundSource.HOSTILE,
                        1.0F,
                        1.0F,
                        false
                     );
                  }
               }

               if (!world.m_6443_(ManaggresiveEntity.class, AABB.m_165882_(new Vec3(x, y, z), 10.0, 10.0, 10.0), e -> true).stream().sorted((new Object() {
                  Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                     return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                  }
               }).compareDistOf(x, y, z)).findFirst().orElse(null).m_9236_().m_5776_()) {
                  world.m_6443_(ManaggresiveEntity.class, AABB.m_165882_(new Vec3(x, y, z), 10.0, 10.0, 10.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_146870_();
               }
            }

            Vec3 _center = new Vec3(x, y, z);

            for (Entity entityiterator : world.m_6443_(Entity.class, new AABB(_center, _center).m_82400_(4.0), e -> true)
               .stream()
               .sorted(Comparator.comparingDouble(_entcnd -> _entcnd.m_20238_(_center)))
               .toList()) {
               if (entityiterator instanceof ItemEntity) {
                  item = entityiterator instanceof ItemEntity _itemEnt ? _itemEnt.m_32055_() : ItemStack.f_41583_;
                  if (item.m_41720_() == ManModItems.TEAROFTHEMAN.get()
                     && (
                        (entity instanceof LivingEntity _livEntx ? _livEntx.m_21205_() : ItemStack.f_41583_).m_41720_() == Items.f_42517_
                           || (entity instanceof LivingEntity _livEnt ? _livEnt.m_21206_() : ItemStack.f_41583_).m_41720_() == Items.f_42517_
                     )) {
                     label250: {
                        if (entity instanceof LivingEntity _livEnt23 && _livEnt23.m_21023_(MobEffects.f_19596_)) {
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
                                    "summon item ~ ~ ~ {Item:{id:\"minecraft:enchanted_book\",Count:1b,tag:{StoredEnchantments:[{id:\"man:fogspeed\",lvl:1s}]}}}"
                                 );
                           }

                           if (!entityiterator.m_9236_().m_5776_()) {
                              entityiterator.m_146870_();
                           }

                           if (!sourceentity.m_9236_().m_5776_()) {
                              sourceentity.m_146870_();
                           }

                           if (event != null && event.isCancelable()) {
                              event.setCanceled(true);
                           } else if (event != null && event.hasResult()) {
                              event.setResult(Result.DENY);
                           }

                           if (entity instanceof LivingEntity _entity) {
                              _entity.m_21153_(1.0F);
                           }

                           if (!(entity instanceof ServerPlayer _player)) {
                              break label250;
                           }

                           Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("man:fog_speed_advancement"));
                           AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
                           if (_ap.m_8193_()) {
                              break label250;
                           }

                           Iterator var40 = _ap.m_8219_().iterator();

                           while (true) {
                              if (!var40.hasNext()) {
                                 break label250;
                              }

                              String criteria = (String)var40.next();
                              _player.m_8960_().m_135988_(_adv, criteria);
                           }
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
                                 "summon item ~ ~ ~ {Item:{id:\"minecraft:enchanted_book\",Count:1b,tag:{StoredEnchantments:[{id:\"man:fog_protection\",lvl:1s}]}}}"
                              );
                        }

                        if (!entityiterator.m_9236_().m_5776_()) {
                           entityiterator.m_146870_();
                        }

                        if (!sourceentity.m_9236_().m_5776_()) {
                           sourceentity.m_146870_();
                        }

                        if (event != null && event.isCancelable()) {
                           event.setCanceled(true);
                        } else if (event != null && event.hasResult()) {
                           event.setResult(Result.DENY);
                        }

                        if (entity instanceof LivingEntity _entity) {
                           _entity.m_21153_(1.0F);
                        }

                        if (entity instanceof ServerPlayer _player) {
                           Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("man:fog_protection_a_dvancement"));
                           AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
                           if (!_ap.m_8193_()) {
                              for (String criteria : _ap.m_8219_()) {
                                 _player.m_8960_().m_135988_(_adv, criteria);
                              }
                           }
                        }
                     }

                     if ((entity instanceof LivingEntity _livEntxx ? _livEntxx.m_21205_() : ItemStack.f_41583_).m_41720_() == Items.f_42517_) {
                        if (entity instanceof Player _playerx) {
                           ItemStack _stktoremove;
                           ItemStack var47 = _stktoremove = entity instanceof LivingEntity _livEntxxx ? _livEntxxx.m_21205_() : ItemStack.f_41583_;
                           _playerx.m_150109_().m_36022_(p -> _stktoremove.m_41720_() == p.m_41720_(), 1, _playerx.f_36095_.m_39730_());
                        }
                     } else if ((entity instanceof LivingEntity _livEntxxx ? _livEntxxx.m_21206_() : ItemStack.f_41583_).m_41720_() == Items.f_42517_
                        && entity instanceof Player _playerx) {
                        ItemStack _stktoremove;
                        ItemStack var49 = _stktoremove = entity instanceof LivingEntity _livEntxxxx ? _livEntxxxx.m_21206_() : ItemStack.f_41583_;
                        _playerx.m_150109_().m_36022_(p -> _stktoremove.m_41720_() == p.m_41720_(), 1, _playerx.f_36095_.m_39730_());
                     }
                  }
               }
            }
         }
      }
   }
}
