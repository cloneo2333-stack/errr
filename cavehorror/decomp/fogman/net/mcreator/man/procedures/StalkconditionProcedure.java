package net.mcreator.man.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StalkconditionProcedure {
   public static boolean execute(LevelAccessor world, double x, double y, double z) {
      return !world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 200.0, 200.0, 200.0), e -> true).isEmpty();
   }
}
