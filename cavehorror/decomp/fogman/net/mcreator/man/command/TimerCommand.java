package net.mcreator.man.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.mcreator.man.procedures.JumpscareoverlaycommandProcedure;
import net.mcreator.man.procedures.ResettimerpProcedure;
import net.mcreator.man.procedures.SpawnmancommandProcedure;
import net.mcreator.man.procedures.TimercommandProcedure;
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
public class TimerCommand {
   @SubscribeEvent
   public static void registerCommand(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("man").requires(s -> s.m_6761_(4)))
                  .then(Commands.m_82127_("spawn").executes(arguments -> {
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

                     SpawnmancommandProcedure.execute(world, x, y, z, entity);
                     return 0;
                  })))
               .then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("debug").then(Commands.m_82127_("timer").executes(arguments -> {
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

                  TimercommandProcedure.execute(world, entity);
                  return 0;
               }))).then(Commands.m_82127_("jumpscare_screen").executes(arguments -> {
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

                  JumpscareoverlaycommandProcedure.execute(world);
                  return 0;
               }))).then(Commands.m_82127_("reset_timer").executes(arguments -> {
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

                  ResettimerpProcedure.execute(world);
                  return 0;
               })))
         );
   }
}
