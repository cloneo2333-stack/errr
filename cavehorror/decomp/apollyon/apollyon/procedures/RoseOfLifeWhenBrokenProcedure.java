package apollyon.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class RoseOfLifeWhenBrokenProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z) {
      if (world instanceof Level _level) {
         BlockPos _bp = BlockPos.m_274561_(x, y - 1.0, z);
         if ((BoneMealItem.m_40627_(new ItemStack(Items.f_42499_), _level, _bp) || BoneMealItem.m_40631_(new ItemStack(Items.f_42499_), _level, _bp, null))
            && !_level.m_5776_()) {
            _level.m_46796_(2005, _bp, 0);
         }
      }
   }
}
