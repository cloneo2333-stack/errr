package net.mcreator.man.procedures;

import net.mcreator.man.network.ManModVariables;
import net.minecraft.world.level.LevelAccessor;

public class JumpscareoverlaycommandProcedure {
   public static void execute(LevelAccessor world) {
      ManModVariables.MapVariables.get(world).overlay_ingame = 30.0;
      ManModVariables.MapVariables.get(world).syncData(world);
   }
}
