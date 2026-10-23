package net.mcreator.man.procedures;

import java.util.Comparator;
import net.mcreator.man.entity.MftfhangEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MftfhangNaturalEntitySpawningConditionProcedure {
   public static boolean execute(LevelAccessor world, double x, double y, double z) {
      if ((!(world instanceof Level _lvl0) || !_lvl0.m_46461_())
         && world.m_6443_(MftfhangEntity.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).isEmpty()
         && !world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).isEmpty()) {
         if (world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).stream().sorted((new Object() {
                  Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                     return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                  }
               }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20186_()
               >= (double)world.m_6924_(
                  Types.MOTION_BLOCKING_NO_LEAVES,
                  (int)world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20189_(),
                  (int)world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20185_()
               )
            && y >= (double)world.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z)) {
            if (!world.m_46861_(BlockPos.m_274561_(x, y, z))) {
               return true;
            }
         } else if (world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).stream().sorted((new Object() {
                  Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                     return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                  }
               }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20186_()
               < (double)world.m_6924_(
                  Types.MOTION_BLOCKING_NO_LEAVES,
                  (int)world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20189_(),
                  (int)world.m_6443_(Player.class, AABB.m_165882_(new Vec3(x, y, z), 300.0, 300.0, 300.0), e -> true).stream().sorted((new Object() {
                     Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                        return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                     }
                  }).compareDistOf(x, y, z)).findFirst().orElse(null).m_20185_()
               )
            && !world.m_46861_(BlockPos.m_274561_(x, y, z))) {
            return true;
         }
      }

      return false;
   }
}
