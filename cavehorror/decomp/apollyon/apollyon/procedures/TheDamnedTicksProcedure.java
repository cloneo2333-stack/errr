package apollyon.procedures;

import apollyon.entity.TheDamnedEntity;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class TheDamnedTicksProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (entity instanceof TheDamnedEntity) {
            if (entity.getPersistentData().m_128471_("targetingPlayer")
               && (entity instanceof Mob _mobEntx ? _mobEntx.m_5448_() : null) instanceof Player
               && (entity instanceof Mob _mobEnt ? _mobEnt.m_5448_() : null) instanceof ServerPlayer) {
               if (world.m_8055_(BlockPos.m_274561_(x, y + 1.0, z + 1.0)).m_204336_(BlockTags.create(new ResourceLocation("apollyon:damned_breakable")))) {
                  world.m_46961_(BlockPos.m_274561_(x, y + 1.0, z + 1.0), false);
               }

               if (world.m_8055_(BlockPos.m_274561_(x + 1.0, y + 1.0, z + 1.0)).m_204336_(BlockTags.create(new ResourceLocation("apollyon:damned_breakable")))) {
                  world.m_46961_(BlockPos.m_274561_(x + 1.0, y + 1.0, z + 1.0), false);
               }

               if (world.m_8055_(BlockPos.m_274561_(x - 1.0, y + 1.0, z + 1.0)).m_204336_(BlockTags.create(new ResourceLocation("apollyon:damned_breakable")))) {
                  world.m_46961_(BlockPos.m_274561_(x - 1.0, y + 1.0, z + 1.0), false);
               }

               if (world.m_8055_(BlockPos.m_274561_(x + 1.0, y + 1.0, z)).m_204336_(BlockTags.create(new ResourceLocation("apollyon:damned_breakable")))) {
                  world.m_46961_(BlockPos.m_274561_(x + 1.0, y + 1.0, z), false);
               }

               if (world.m_8055_(BlockPos.m_274561_(x + 1.0, y + 1.0, z - 1.0)).m_204336_(BlockTags.create(new ResourceLocation("apollyon:damned_breakable")))) {
                  world.m_46961_(BlockPos.m_274561_(x + 1.0, y + 1.0, z - 1.0), false);
               }

               if (world.m_8055_(BlockPos.m_274561_(x, y + 1.0, z - 1.0)).m_204336_(BlockTags.create(new ResourceLocation("apollyon:damned_breakable")))) {
                  world.m_46961_(BlockPos.m_274561_(x, y + 1.0, z - 1.0), false);
               }

               if (world.m_8055_(BlockPos.m_274561_(x - 1.0, y + 1.0, z - 1.0)).m_204336_(BlockTags.create(new ResourceLocation("apollyon:damned_breakable")))) {
                  world.m_46961_(BlockPos.m_274561_(x - 1.0, y + 1.0, z - 1.0), false);
               }

               if (world.m_8055_(BlockPos.m_274561_(x - 1.0, y + 1.0, z)).m_204336_(BlockTags.create(new ResourceLocation("apollyon:damned_breakable")))) {
                  world.m_46961_(BlockPos.m_274561_(x - 1.0, y + 1.0, z), false);
               }
            }

            if (entity.m_20069_() && entity instanceof LivingEntity _entity && !_entity.m_9236_().m_5776_()) {
               _entity.m_7292_(new MobEffectInstance(MobEffects.f_19593_, 1, 7, false, false));
            }

            if (entity.m_5830_() && !entity.m_9236_().m_5776_() && entity.m_20194_() != null) {
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
                     "tp @s ~ ~1 ~"
                  );
            }

            boolean var13;
            label86: {
               if (world instanceof Level _lvl34 && _lvl34.m_46461_()) {
                  var13 = true;
                  break label86;
               }

               var13 = false;
            }

            if (var13 && !entity.m_9236_().m_5776_()) {
               entity.m_146870_();
            }
         }
      }
   }
}
