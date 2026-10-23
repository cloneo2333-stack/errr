package net.mcreator.man.procedures;

import java.util.Comparator;
import net.mcreator.man.ManMod;
import net.mcreator.man.configuration.ManconfigConfiguration;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.minecraft.client.Minecraft;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class ManaggresiveOnEntityTickUpdateProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         double timer = 0.0;
         boolean firstuse = false;
         Entity player = null;
         entity.getPersistentData().m_128347_("loop_timer", entity.getPersistentData().m_128459_("loop_timer") + 1.0);
         if (entity.getPersistentData().m_128459_("loop_timer") == 105.0) {
            if (world instanceof Level _level) {
               if (!_level.m_5776_()) {
                  _level.m_5594_(
                     null,
                     BlockPos.m_274561_(x, y, z),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:loop")),
                     SoundSource.HOSTILE,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.m_7785_(
                     x, y, z, (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:loop")), SoundSource.HOSTILE, 1.0F, 1.0F, false
                  );
               }
            }

            entity.getPersistentData().m_128347_("loop_timer", 0.0);
         }

         if (entity.m_20159_()) {
            entity.m_8127_();
         }

         if (world.m_6425_(BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_() + 0.2, entity.m_20189_())).m_76188_().m_60734_() == Blocks.f_49990_) {
            entity.f_19789_ = 0.0F;
            if (entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance(MobEffects.f_19593_, 3, 1, false, false));
            }

            if (entity.m_20184_().m_7098_() < 0.0) {
               entity.m_20256_(new Vec3(entity.m_20184_().m_7096_(), 0.03, entity.m_20184_().m_7094_()));
            } else {
               entity.m_20256_(new Vec3(entity.m_20184_().m_7096_(), entity.m_20184_().m_7098_() + 0.03, entity.m_20184_().m_7094_()));
            }
         }

         if ((entity instanceof Mob _mobEnt ? _mobEnt.m_5448_() : null) != null) {
            if (entity.m_20185_() < (entity instanceof Mob _mobEntx ? _mobEntx.m_5448_() : null).m_20185_()) {
               entity.m_20256_(new Vec3(entity.m_20184_().m_7096_() + 0.03, entity.m_20184_().m_7098_(), entity.m_20184_().m_7094_()));
            } else if (entity.m_20185_() > (entity instanceof Mob _mobEntx ? _mobEntx.m_5448_() : null).m_20185_()) {
               entity.m_20256_(new Vec3(entity.m_20184_().m_7096_() - 0.03, entity.m_20184_().m_7098_(), entity.m_20184_().m_7094_()));
            }

            if (entity.m_20189_() < (entity instanceof Mob _mobEntx ? _mobEntx.m_5448_() : null).m_20189_()) {
               entity.m_20256_(new Vec3(entity.m_20184_().m_7096_(), entity.m_20184_().m_7098_(), entity.m_20184_().m_7094_() + 0.03));
            } else if (entity.m_20189_() > (entity instanceof Mob _mobEntx ? _mobEntx.m_5448_() : null).m_20189_()) {
               entity.m_20256_(new Vec3(entity.m_20184_().m_7096_(), entity.m_20184_().m_7098_(), entity.m_20184_().m_7094_() - 0.03));
            }

            if ((Boolean)ManconfigConfiguration.CLIMBING.get()) {
               if (world.m_8055_(
                        new BlockPos(
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(1.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123341_(),
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(1.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123342_(),
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(1.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123343_()
                        )
                     )
                     .m_60815_()
                  && (entity instanceof Mob _mobEntx ? _mobEntx.m_5448_() : null).m_20186_() >= entity.m_20186_() + 3.0) {
                  entity.m_20256_(new Vec3(0.0, 0.75, 0.0));
                  entity.m_6858_(true);
                  ManMod.queueServerWork(10, () -> {
                     entity.m_146922_(entity.m_146908_());
                     entity.m_146926_(entity.m_146909_());
                     entity.m_5618_(entity.m_146908_());
                     entity.m_5616_(entity.m_146908_());
                     entity.f_19859_ = entity.m_146908_();
                     entity.f_19860_ = entity.m_146909_();
                     if (entity instanceof LivingEntity _entity) {
                        _entity.f_20884_ = _entity.m_146908_();
                        _entity.f_20886_ = _entity.m_146908_();
                     }
                  });
                  if (world instanceof ServerLevel _levelx) {
                     _levelx.m_7654_()
                        .m_129892_()
                        .m_230957_(
                           new CommandSourceStack(
                                 CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _levelx, 4, "", Component.m_237113_(""), _levelx.m_7654_(), null
                              )
                              .m_81324_(),
                           "execute as @e[type=!minecraft:player,type=man:manfromthefog] at @s run tp @s ~ ~ ~ facing entity @p[distance=..200]"
                        );
                  }
               } else {
                  entity.m_6858_(false);
               }
            }

            if ((Boolean)ManconfigConfiguration.BREAK_BLOCKS.get()
               && timer <= 1.0
               && (
                  world.m_8055_(
                           new BlockPos(
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                        )
                        .m_204336_(BlockTags.create(new ResourceLocation("forge:doors")))
                     || world.m_8055_(
                           new BlockPos(
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                        )
                        .m_204336_(BlockTags.create(new ResourceLocation("forge:fences")))
                     || world.m_8055_(
                           new BlockPos(
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                        )
                        .m_204336_(BlockTags.create(new ResourceLocation("forge:fence_gates")))
                     || world.m_8055_(
                           new BlockPos(
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                        )
                        .m_204336_(BlockTags.create(new ResourceLocation("forge:glass")))
                     || world.m_8055_(
                           new BlockPos(
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                        )
                        .m_204336_(BlockTags.create(new ResourceLocation("forge:glass_panes")))
                     || (double)world.m_8055_(
                                 new BlockPos(
                                    entity.m_9236_()
                                       .m_45547_(
                                          new ClipContext(
                                             entity.m_20299_(1.0F),
                                             entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                             Block.OUTLINE,
                                             Fluid.NONE,
                                             entity
                                          )
                                       )
                                       .m_82425_()
                                       .m_123341_(),
                                    entity.m_9236_()
                                       .m_45547_(
                                          new ClipContext(
                                             entity.m_20299_(1.0F),
                                             entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                             Block.OUTLINE,
                                             Fluid.NONE,
                                             entity
                                          )
                                       )
                                       .m_82425_()
                                       .m_123342_(),
                                    entity.m_9236_()
                                       .m_45547_(
                                          new ClipContext(
                                             entity.m_20299_(1.0F),
                                             entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                             Block.OUTLINE,
                                             Fluid.NONE,
                                             entity
                                          )
                                       )
                                       .m_82425_()
                                       .m_123343_()
                                 )
                              )
                              .m_60800_(
                                 world,
                                 new BlockPos(
                                    entity.m_9236_()
                                       .m_45547_(
                                          new ClipContext(
                                             entity.m_20299_(1.0F),
                                             entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                             Block.OUTLINE,
                                             Fluid.NONE,
                                             entity
                                          )
                                       )
                                       .m_82425_()
                                       .m_123341_(),
                                    entity.m_9236_()
                                       .m_45547_(
                                          new ClipContext(
                                             entity.m_20299_(1.0F),
                                             entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                             Block.OUTLINE,
                                             Fluid.NONE,
                                             entity
                                          )
                                       )
                                       .m_82425_()
                                       .m_123342_(),
                                    entity.m_9236_()
                                       .m_45547_(
                                          new ClipContext(
                                             entity.m_20299_(1.0F),
                                             entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                             Block.OUTLINE,
                                             Fluid.NONE,
                                             entity
                                          )
                                       )
                                       .m_82425_()
                                       .m_123343_()
                                 )
                              )
                           <= (Double)ManconfigConfiguration.BLOCK_HARDNESS.get()
                        && (entity instanceof Mob _mobEntxxxx ? _mobEntxxxx.m_5448_() : null).m_20186_() + 2.0
                           < (double)world.m_6924_(
                              Types.MOTION_BLOCKING_NO_LEAVES,
                              (int)(entity instanceof Mob _mobEntxxx ? _mobEntxxx.m_5448_() : null).m_20185_(),
                              (int)(entity instanceof Mob _mobEntxx ? _mobEntxx.m_5448_() : null).m_20189_()
                           )
                        && !world.m_8055_(
                              new BlockPos(
                                 entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                          Block.OUTLINE,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123341_(),
                                 entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                          Block.OUTLINE,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123342_(),
                                 entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(2.0)),
                                          Block.OUTLINE,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123343_()
                              )
                           )
                           .m_204336_(BlockTags.create(new ResourceLocation("man:container")))
               )
               && (entity instanceof ManaggresiveEntity _datEntI ? (Integer)_datEntI.m_20088_().m_135370_(ManaggresiveEntity.DATA_BreakTimer) : 0) <= 0) {
               if (world instanceof ServerLevel _origLevel) {
                  LevelAccessor var20 = _origLevel.m_7654_().m_129880_(Level.f_46428_);
                  if (var20 != null) {
                     BlockPos _pos = new BlockPos(
                        entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)), Block.OUTLINE, Fluid.NONE, entity
                              )
                           )
                           .m_82425_()
                           .m_123341_(),
                        entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)), Block.OUTLINE, Fluid.NONE, entity
                              )
                           )
                           .m_82425_()
                           .m_123342_(),
                        entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F), entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)), Block.OUTLINE, Fluid.NONE, entity
                              )
                           )
                           .m_82425_()
                           .m_123343_()
                     );
                     net.minecraft.world.level.block.Block.m_49892_(
                        var20.m_8055_(_pos),
                        var20,
                        new BlockPos(
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123341_(),
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123342_(),
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123343_()
                        ),
                        null
                     );
                     var20.m_46961_(_pos, false);
                  }

                  world = world;
               }

               if (entity instanceof ManaggresiveEntity _datEntSetI) {
                  _datEntSetI.m_20088_()
                     .m_135381_(ManaggresiveEntity.DATA_BreakTimer, (int)((Double)ManconfigConfiguration.BLOCK_BREAK_TIMER.get()).doubleValue());
               }
            }

            if (entity.getPersistentData().m_128459_("timer") >= entity.getPersistentData().m_128459_("deadline")
               && entity.getPersistentData().m_128471_("ready")) {
               if (!entity.m_9236_().m_5776_()) {
                  entity.m_146870_();
               }

               if (world instanceof ServerLevel _levelx) {
                  _levelx.m_7654_()
                     .m_129892_()
                     .m_230957_(
                        new CommandSourceStack(
                              CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _levelx, 4, "", Component.m_237113_(""), _levelx.m_7654_(), null
                           )
                           .m_81324_(),
                        "stopsound @p"
                     );
               }

               if (world instanceof Level _levelx) {
                  if (!_levelx.m_5776_()) {
                     _levelx.m_5594_(
                        null,
                        BlockPos.m_274561_(x, y, z),
                        (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:vanish")),
                        SoundSource.HOSTILE,
                        2.0F,
                        1.0F
                     );
                  } else {
                     _levelx.m_7785_(
                        x, y, z, (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:vanish")), SoundSource.HOSTILE, 2.0F, 1.0F, false
                     );
                  }
               }

               if ((Boolean)ManconfigConfiguration.LIGHTNINGS.get() && world instanceof ServerLevel _levelxx) {
                  LightningBolt entityToSpawn = (LightningBolt)EntityType.f_20465_.m_20615_(_levelxx);
                  entityToSpawn.m_20219_(Vec3.m_82539_(BlockPos.m_274561_(x, y, z)));
                  entityToSpawn.m_20874_(true);
                  _levelxx.m_7967_(entityToSpawn);
               }
            }

            if (!world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 4.0, 4.0, 4.0), e -> true).isEmpty()) {
               if (((entity instanceof Mob _mobEntxx ? _mobEntxx.m_5448_() : null) instanceof LivingEntity _livEnt ? _livEnt.m_21205_() : ItemStack.f_41583_)
                     .m_41720_()
                  == Items.f_42740_) {
                  if (entity instanceof Player _player_ && !_player_.m_21205_().m_41619_() && _player_.m_21205_().m_41613_() > 0) {
                     _player_.m_36176_(new ItemStack(_player_.m_21205_().m_41720_(), 1), true);
                     _player_.m_21205_().m_41774_(1);
                     _player_.m_150109_().m_6596_();
                  }
               } else if (((entity instanceof Mob _mobEntxxx ? _mobEntxxx.m_5448_() : null) instanceof LivingEntity _livEntx
                           ? _livEntx.m_21206_()
                           : ItemStack.f_41583_)
                        .m_41720_()
                     == Items.f_42740_
                  && entity instanceof Player _player_
                  && !_player_.m_21206_().m_41619_()
                  && _player_.m_21206_().m_41613_() > 0) {
                  _player_.m_36176_(new ItemStack(_player_.m_21206_().m_41720_(), 1), true);
                  _player_.m_21206_().m_41774_(1);
                  _player_.m_150109_().m_6596_();
               }
            }
         }

         entity.getPersistentData().m_128347_("timer", entity.getPersistentData().m_128459_("timer") + 1.0);
         if (world.m_8055_(BlockPos.m_274561_(x, y + 1.0, z)).m_60734_() != Blocks.f_50016_) {
            entity.m_20260_(true);
         } else {
            entity.m_20260_(false);
         }

         if ((entity instanceof Mob _mobEnt ? _mobEnt.m_5448_() : null) == null
            && !world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty()
            && (Boolean)ManconfigConfiguration.SEARCH_MECHANIC.get()) {
            player = world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).stream().sorted((new Object() {
               Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                  return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
               }
            }).compareDistOf(x, y, z)).findFirst().orElse(null);
            if (!(new Object() {
                     public boolean checkGamemode(Entity _ent) {
                        if (_ent instanceof ServerPlayer _serverPlayer) {
                           return _serverPlayer.f_8941_.m_9290_() == GameType.SPECTATOR;
                        } else {
                           return _ent.m_9236_().m_5776_() && _ent instanceof Player _player
                              ? Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()) != null
                                 && Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()).m_105325_() == GameType.SPECTATOR
                              : false;
                        }
                     }
                  })
                  .checkGamemode(player)
               && !(new Object() {
                     public boolean checkGamemode(Entity _ent) {
                        if (_ent instanceof ServerPlayer _serverPlayer) {
                           return _serverPlayer.f_8941_.m_9290_() == GameType.CREATIVE;
                        } else {
                           return _ent.m_9236_().m_5776_() && _ent instanceof Player _player
                              ? Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()) != null
                                 && Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()).m_105325_() == GameType.CREATIVE
                              : false;
                        }
                     }
                  })
                  .checkGamemode(player)) {
               entity.m_7618_(Anchor.EYES, new Vec3(player.m_20185_(), player.m_20186_(), player.m_20189_()));
               if (entity instanceof Mob _entity) {
                  _entity.m_21573_().m_26519_(player.m_20185_(), player.m_20186_(), player.m_20189_(), 1.0);
               }

               if (!world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 7.0, 7.0, 7.0), e -> true).isEmpty()
                  && (entity instanceof ManaggresiveEntity _datEntI ? (Integer)_datEntI.m_20088_().m_135370_(ManaggresiveEntity.DATA_BreakTimer) : 0) <= 0
                  && (double)world.m_8055_(
                           new BlockPos(
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                        )
                        .m_60800_(
                           world,
                           new BlockPos(
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                        )
                     <= (Double)ManconfigConfiguration.BLOCK_HARDNESS_SEARCH.get()) {
                  if (world instanceof ServerLevel _origLevel) {
                     LevelAccessor var21 = _origLevel.m_7654_().m_129880_(Level.f_46428_);
                     if (var21 != null) {
                        BlockPos _pos = new BlockPos(
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123341_(),
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123342_(),
                           entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123343_()
                        );
                        net.minecraft.world.level.block.Block.m_49892_(
                           var21.m_8055_(_pos),
                           var21,
                           new BlockPos(
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(3.0)),
                                       Block.OUTLINE,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           ),
                           null
                        );
                        var21.m_46961_(_pos, false);
                     }
                  }

                  if (entity instanceof ManaggresiveEntity _datEntSetI) {
                     _datEntSetI.m_20088_()
                        .m_135381_(ManaggresiveEntity.DATA_BreakTimer, (int)((Double)ManconfigConfiguration.BLOCK_BREAK_TIMER.get()).doubleValue());
                  }
               }
            }
         }

         if (entity instanceof ManaggresiveEntity _datEntSetI) {
            _datEntSetI.m_20088_()
               .m_135381_(
                  ManaggresiveEntity.DATA_BreakTimer,
                  (entity instanceof ManaggresiveEntity _datEntI ? (Integer)_datEntI.m_20088_().m_135370_(ManaggresiveEntity.DATA_BreakTimer) : 0) - 1
               );
         }
      }
   }
}
