package net.mcreator.man.procedures;

import net.mcreator.man.network.ManModVariables;
import net.minecraft.world.level.LevelAccessor;

public class ResettimerpProcedure {
   public static void execute(LevelAccessor world) {
      ManModVariables.MapVariables.get(world).reset = 0.0;
      ManModVariables.MapVariables.get(world).syncData(world);
   }
}
