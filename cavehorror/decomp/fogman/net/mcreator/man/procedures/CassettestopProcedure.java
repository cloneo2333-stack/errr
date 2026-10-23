package net.mcreator.man.procedures;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class CassettestopProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         ItemStack var10000;
         label45: {
            if (entity instanceof Player _plrSlotItem && _plrSlotItem.f_36096_ instanceof Supplier _splr && _splr.get() instanceof Map _slt) {
               var10000 = ((Slot)_slt.get(0)).m_7993_();
               break label45;
            }

            var10000 = ItemStack.f_41583_;
         }

         if (var10000.m_41720_() != ItemStack.f_41583_.m_41720_()) {
            label36: {
               if (entity instanceof Player _plrSlotItem && _plrSlotItem.f_36096_ instanceof Supplier _splr && _splr.get() instanceof Map _slt) {
                  var10000 = ((Slot)_slt.get(1)).m_7993_();
                  break label36;
               }

               var10000 = ItemStack.f_41583_;
            }

            if (var10000.m_41720_() != ItemStack.f_41583_.m_41720_()) {
               return;
            }
         }

         if (world instanceof ServerLevel _level) {
            _level.m_7654_()
               .m_129892_()
               .m_230957_(
                  new CommandSourceStack(
                        CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _level, 4, "", Component.m_237113_(""), _level.m_7654_(), null
                     )
                     .m_81324_(),
                  "stopsound @p"
               );
         }

         if (world instanceof Level _level) {
            if (!_level.m_5776_()) {
               _level.m_5594_(
                  null,
                  BlockPos.m_274561_(x, y, z),
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.lever.click")),
                  SoundSource.BLOCKS,
                  1.0F,
                  1.0F
               );
            } else {
               _level.m_7785_(
                  x, y, z, (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.lever.click")), SoundSource.BLOCKS, 1.0F, 1.0F, false
               );
            }
         }
      }
   }
}
