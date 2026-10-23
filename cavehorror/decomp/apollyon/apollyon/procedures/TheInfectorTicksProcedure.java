package apollyon.procedures;

import apollyon.entity.TheInfectorEntity;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TheInfectorTicksProcedure {
   @SubscribeEvent
   public static void onEntityTick(LivingTickEvent event) {
      execute(event, event.getEntity());
   }

   public static void execute(Entity entity) {
      execute(null, entity);
   }

   private static void execute(@Nullable Event event, Entity entity) {
      if (entity != null) {
         if (entity instanceof TheInfectorEntity) {
            if (!entity.getPersistentData().m_128471_("neutral") && !entity.m_9236_().m_5776_() && entity.m_20194_() != null) {
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
                     "fill ~4 ~1 ~3 ~-4 ~3 ~-2 air destroy"
                  );
            }

            if (!entity.m_20147_()) {
            }

            if (entity.getPersistentData().m_128471_("neutral") && !entity.m_6144_()) {
               entity.m_20260_(true);
            }
         }
      }
   }
}
