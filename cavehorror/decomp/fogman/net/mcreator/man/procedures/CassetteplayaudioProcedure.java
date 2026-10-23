package net.mcreator.man.procedures;

import java.util.Map;
import java.util.function.Supplier;
import net.mcreator.man.init.ManModItems;
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

public class CassetteplayaudioProcedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         Object var10000;
         ItemStack var10001;
         label56: {
            var10000 = ManModItems.CASSETTE_1SAFETYINSTRUCTIONS.get();
            if (entity instanceof Player _plrSlotItem && _plrSlotItem.f_36096_ instanceof Supplier _splr && _splr.get() instanceof Map _slt) {
               var10001 = ((Slot)_slt.get(0)).m_7993_();
               break label56;
            }

            var10001 = ItemStack.f_41583_;
         }

         if (var10000 != var10001.m_41720_()) {
            label47: {
               var10000 = ManModItems.CASSETTE_1SAFETYINSTRUCTIONS.get();
               if (entity instanceof Player _plrSlotItem && _plrSlotItem.f_36096_ instanceof Supplier _splr && _splr.get() instanceof Map _slt) {
                  var10001 = ((Slot)_slt.get(1)).m_7993_();
                  break label47;
               }

               var10001 = ItemStack.f_41583_;
            }

            if (var10000 != var10001.m_41720_()) {
               return;
            }
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

         if (entity instanceof Player _player) {
            _player.m_6915_();
         }

         if (world instanceof Level _levelx) {
            if (!_levelx.m_5776_()) {
               _levelx.m_5594_(
                  null,
                  BlockPos.m_274561_(x, y, z),
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:safety_instructions")),
                  SoundSource.AMBIENT,
                  1.0F,
                  1.0F
               );
            } else {
               _levelx.m_7785_(
                  x,
                  y,
                  z,
                  (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:safety_instructions")),
                  SoundSource.AMBIENT,
                  1.0F,
                  1.0F,
                  false
               );
            }
         }

         if (world instanceof ServerLevel _levelxx) {
            _levelxx.m_7654_()
               .m_129892_()
               .m_230957_(
                  new CommandSourceStack(
                        CommandSource.f_80164_, new Vec3(x, y, z), Vec2.f_82462_, _levelxx, 4, "", Component.m_237113_(""), _levelxx.m_7654_(), null
                     )
                     .m_81324_(),
                  "tellraw @p {\"text\":\"Watch Video\",\"italic\":true,\"underlined\":true,\"hoverEvent\":{\"action\":\"show_text\",\"value\":[{\"text\":\"click to get directed\"}]},\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://youtu.be/xRFD7WI4N44?si=j_kIveexgHe7AaLU\"}}"
               );
         }
      }
   }
}
