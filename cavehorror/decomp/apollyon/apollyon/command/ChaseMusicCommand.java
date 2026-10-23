package apollyon.command;

import apollyon.procedures.CmdChaseMusicProcedure;
import apollyon.procedures.CmdCheckMusicProcedure;
import apollyon.procedures.CmdSpawnVolumeProcedure;
import apollyon.procedures.CmdToggleVolumeProcedure;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class ChaseMusicCommand {
   @SubscribeEvent
   public static void registerCommand(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("apollyonconfig")
                  .then(((LiteralArgumentBuilder)Commands.m_82127_("chase_music").then(Commands.m_82127_("toggle").executes(arguments -> {
                     Level world = ((CommandSourceStack)arguments.getSource()).getUnsidedLevel();
                     double x = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7096_();
                     double y = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7098_();
                     double z = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7094_();
                     Entity entity = ((CommandSourceStack)arguments.getSource()).m_81373_();
                     if (entity == null && world instanceof ServerLevel _servLevel) {
                        entity = FakePlayerFactory.getMinecraft(_servLevel);
                     }

                     Direction direction = Direction.DOWN;
                     if (entity != null) {
                        direction = entity.m_6350_();
                     }

                     CmdChaseMusicProcedure.execute(world, x, y, z);
                     return 0;
                  }))).then(Commands.m_82127_("status").executes(arguments -> {
                     Level world = ((CommandSourceStack)arguments.getSource()).getUnsidedLevel();
                     double x = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7096_();
                     double y = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7098_();
                     double z = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7094_();
                     Entity entity = ((CommandSourceStack)arguments.getSource()).m_81373_();
                     if (entity == null && world instanceof ServerLevel _servLevel) {
                        entity = FakePlayerFactory.getMinecraft(_servLevel);
                     }

                     Direction direction = Direction.DOWN;
                     if (entity != null) {
                        direction = entity.m_6350_();
                     }

                     CmdCheckMusicProcedure.execute(world);
                     return 0;
                  }))))
               .then(((LiteralArgumentBuilder)Commands.m_82127_("spawn_volume").then(Commands.m_82127_("status").executes(arguments -> {
                  Level world = ((CommandSourceStack)arguments.getSource()).getUnsidedLevel();
                  double x = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7096_();
                  double y = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7098_();
                  double z = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7094_();
                  Entity entity = ((CommandSourceStack)arguments.getSource()).m_81373_();
                  if (entity == null && world instanceof ServerLevel _servLevel) {
                     entity = FakePlayerFactory.getMinecraft(_servLevel);
                  }

                  Direction direction = Direction.DOWN;
                  if (entity != null) {
                     direction = entity.m_6350_();
                  }

                  CmdSpawnVolumeProcedure.execute(world);
                  return 0;
               }))).then(Commands.m_82127_("toggle").executes(arguments -> {
                  Level world = ((CommandSourceStack)arguments.getSource()).getUnsidedLevel();
                  double x = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7096_();
                  double y = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7098_();
                  double z = ((CommandSourceStack)arguments.getSource()).m_81371_().m_7094_();
                  Entity entity = ((CommandSourceStack)arguments.getSource()).m_81373_();
                  if (entity == null && world instanceof ServerLevel _servLevel) {
                     entity = FakePlayerFactory.getMinecraft(_servLevel);
                  }

                  Direction direction = Direction.DOWN;
                  if (entity != null) {
                     direction = entity.m_6350_();
                  }

                  CmdToggleVolumeProcedure.execute(world);
                  return 0;
               })))
         );
   }
}
