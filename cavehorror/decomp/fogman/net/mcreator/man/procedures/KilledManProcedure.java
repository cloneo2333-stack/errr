package net.mcreator.man.procedures;

import java.util.Comparator;
import javax.annotation.Nullable;
import net.mcreator.man.entity.ManaggresiveEntity;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class KilledManProcedure {
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
         if (entity instanceof ManaggresiveEntity
            && !world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 10.0, 10.0, 10.0), e -> true).isEmpty()
            && sourceentity == world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 10.0, 10.0, 10.0), e -> true).stream().sorted((new Object() {
               Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                  return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
               }
            }).compareDistOf(x, y, z)).findFirst().orElse(null)) {
            Entity _adv = world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 10.0, 10.0, 10.0), e -> true).stream().sorted((new Object() {
               Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                  return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
               }
            }).compareDistOf(x, y, z)).findFirst().orElse(null);
            if (_adv instanceof ServerPlayer _player) {
               Advancement _advx = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("man:kill"));
               AdvancementProgress _ap = _player.m_8960_().m_135996_(_advx);
               if (!_ap.m_8193_()) {
                  for (String criteria : _ap.m_8219_()) {
                     _player.m_8960_().m_135988_(_advx, criteria);
                  }
               }
            }
         }
      }
   }
}
