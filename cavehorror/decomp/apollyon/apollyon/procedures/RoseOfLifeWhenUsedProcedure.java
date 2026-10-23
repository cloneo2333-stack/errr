package apollyon.procedures;

import apollyon.entity.TheDamnedEntity;
import apollyon.init.ApollyonModBlocks;
import apollyon.network.ApollyonModVariables;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber
public class RoseOfLifeWhenUsedProcedure {
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
         if (!ApollyonModVariables.WorldVariables.get(world).damned_fakeout
            && entity instanceof TheDamnedEntity
            && (sourceentity instanceof LivingEntity _entity ? _entity.m_21055_(((Block)ApollyonModBlocks.ROSE_OF_LIFE.get()).m_5456_()) : false)) {
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

            if (sourceentity instanceof ServerPlayer _player) {
               Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:back_to_hell"));
               AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
               if (!_ap.m_8193_()) {
                  for (String criteria : _ap.m_8219_()) {
                     _player.m_8960_().m_135988_(_adv, criteria);
                  }
               }
            }

            if (sourceentity instanceof ServerPlayer _playerx) {
               Advancement _adv = _playerx.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:the_rose_of_death"));
               AdvancementProgress _ap = _playerx.m_8960_().m_135996_(_adv);
               if (!_ap.m_8193_()) {
                  for (String criteria : _ap.m_8219_()) {
                     _playerx.m_8960_().m_135988_(_adv, criteria);
                  }
               }
            }

            if (world instanceof Level _level) {
               if (!_level.m_5776_()) {
                  _level.m_5594_(
                     null,
                     BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.m_7785_(
                     entity.m_20185_(),
                     entity.m_20186_(),
                     entity.m_20189_(),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            if (world instanceof Level _levelx) {
               if (!_levelx.m_5776_()) {
                  _levelx.m_5594_(
                     null,
                     BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:used_rose")),
                     SoundSource.AMBIENT,
                     1.0F,
                     1.0F
                  );
               } else {
                  _levelx.m_7785_(
                     entity.m_20185_(),
                     entity.m_20186_(),
                     entity.m_20189_(),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:used_rose")),
                     SoundSource.AMBIENT,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            if (world instanceof Level _levelxx) {
               BlockPos _bp = BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_() - 1.0, entity.m_20189_());
               if ((
                     BoneMealItem.m_40627_(new ItemStack(Items.f_42499_), _levelxx, _bp)
                        || BoneMealItem.m_40631_(new ItemStack(Items.f_42499_), _levelxx, _bp, null)
                  )
                  && !_levelxx.m_5776_()) {
                  _levelxx.m_46796_(2005, _bp, 0);
               }
            }

            if (world instanceof ServerLevel _levelxxx) {
               _levelxxx.m_8767_(ParticleTypes.f_123812_, entity.m_20185_(), entity.m_20186_(), entity.m_20189_(), 1, 1.0, 1.0, 1.0, 1.0);
            }

            if ((sourceentity instanceof Player _plr ? !_plr.m_150110_().f_35937_ : true) && sourceentity instanceof Player _playerxx) {
               ItemStack _stktoremove = new ItemStack((ItemLike)ApollyonModBlocks.ROSE_OF_LIFE.get());
               _playerxx.m_150109_().m_36022_(p -> _stktoremove.m_41720_() == p.m_41720_(), 1, _playerxx.f_36095_.m_39730_());
            }

            entity.m_6074_();
         }
      }
   }
}
