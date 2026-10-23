package apollyon.procedures;

import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TheDamnedSilencesProcedure {
   @SubscribeEvent
   public static void onEntityDeath(LivingDeathEvent event) {
      if (event != null && event.getEntity() != null) {
         execute(
            event, event.getEntity().m_9236_(), event.getEntity().m_20185_(), event.getEntity().m_20186_(), event.getEntity().m_20189_(), event.getEntity()
         );
      }
   }

   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      execute(null, world, x, y, z, entity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (entity instanceof Player || entity instanceof ServerPlayer) {
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
                     "playsound apollyon:player_death ambient @s"
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
                     "tp @e[type=apollyon:the_damned,nbt={ForgeData:{targetingPlayer: 1b}}] ~ ~-1000 ~"
                  );
            }
         }
      }
   }
}
