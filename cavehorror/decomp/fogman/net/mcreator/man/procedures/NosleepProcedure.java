package net.mcreator.man.procedures;

import javax.annotation.Nullable;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.mcreator.man.entity.ManfromthefogEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class NosleepProcedure {
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
         Direction direction = Direction.NORTH;
         BlockState bed = Blocks.f_50016_.m_49966_();
         if (entity instanceof LivingEntity _livEnt0
            && _livEnt0.m_5803_()
            && (
               !world.m_6443_(ManaggresiveEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty()
                  || !world.m_6443_(ManfromthefogEntity.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty()
            )) {
            if (entity instanceof Player _player && !_player.m_9236_().m_5776_()) {
               _player.m_5661_(Component.m_237113_("You can't sleep..."), false);
            }

            entity.m_6469_(new DamageSource(world.m_9598_().m_175515_(Registries.f_268580_).m_246971_(DamageTypes.f_268433_)), 0.1F);
         }
      }
   }
}
