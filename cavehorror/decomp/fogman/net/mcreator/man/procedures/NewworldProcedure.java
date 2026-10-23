package net.mcreator.man.procedures;

import javax.annotation.Nullable;
import net.mcreator.man.network.ManModVariables;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class NewworldProcedure {
   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      execute(event, event.getEntity().m_9236_(), event.getEntity().m_20185_(), event.getEntity().m_20186_(), event.getEntity().m_20189_());
   }

   public static void execute(LevelAccessor world, double x, double y, double z) {
      execute(null, world, x, y, z);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z) {
      if (!ManModVariables.MapVariables.get(world).new_world) {
         ManModVariables.MapVariables.get(world).new_world = true;
         ManModVariables.MapVariables.get(world).syncData(world);
         if (world instanceof ServerLevel _level) {
            _level.m_7654_()
               .m_129892_()
               .m_230957_(
                  new CommandSourceStack(
                        CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                     )
                     .m_81324_(),
                  "/tellraw @p {\"text\":\"Join our Kofi and get early access to new updates!\",\"color\":\"blue\",\"bold\":true,\"italic\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"Click to join us!\",\"color\":\"gold\",\"italic\":true}]},\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://ko-fi.com/mproductions\"}}"
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
                  "give @p man:cassette_1safetyinstructions"
               );
         }
      }
   }
}
