package net.mcreator.man.world.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.mcreator.man.ManMod;
import net.mcreator.man.init.ManModMenus;
import net.mcreator.man.network.CassetteRecorderGuiSlotMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CassetteRecorderGuiMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
   public static final HashMap<String, Object> guistate = new HashMap<>();
   public final Level world;
   public final Player entity;
   public int x;
   public int y;
   public int z;
   private ContainerLevelAccess access = ContainerLevelAccess.f_39287_;
   private IItemHandler internal;
   private final Map<Integer, Slot> customSlots = new HashMap<>();
   private boolean bound = false;
   private Supplier<Boolean> boundItemMatcher = null;
   private Entity boundEntity = null;
   private BlockEntity boundBlockEntity = null;

   public CassetteRecorderGuiMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
      super((MenuType)ManModMenus.CASSETTE_RECORDER_GUI.get(), id);
      this.entity = inv.f_35978_;
      this.world = inv.f_35978_.m_9236_();
      this.internal = new ItemStackHandler(2);
      BlockPos pos = null;
      if (extraData != null) {
         pos = extraData.m_130135_();
         this.x = pos.m_123341_();
         this.y = pos.m_123342_();
         this.z = pos.m_123343_();
         this.access = ContainerLevelAccess.m_39289_(this.world, pos);
      }

      if (pos != null) {
         if (extraData.readableBytes() == 1) {
            byte hand = extraData.readByte();
            ItemStack itemstack = hand == 0 ? this.entity.m_21205_() : this.entity.m_21206_();
            this.boundItemMatcher = () -> itemstack == (hand == 0 ? this.entity.m_21205_() : this.entity.m_21206_());
            itemstack.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
               this.internal = capability;
               this.bound = true;
            });
         } else if (extraData.readableBytes() > 1) {
            extraData.readByte();
            this.boundEntity = this.world.m_6815_(extraData.m_130242_());
            if (this.boundEntity != null) {
               this.boundEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
                  this.internal = capability;
                  this.bound = true;
               });
            }
         } else {
            this.boundBlockEntity = this.world.m_7702_(pos);
            if (this.boundBlockEntity != null) {
               this.boundBlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
                  this.internal = capability;
                  this.bound = true;
               });
            }
         }
      }

      this.customSlots.put(0, this.m_38897_(new SlotItemHandler(this.internal, 0, 94, 20) {
         private final int slot = 0;

         public void m_6654_() {
            super.m_6654_();
            CassetteRecorderGuiMenu.this.slotChanged(0, 0, 0);
         }
      }));
      this.customSlots.put(1, this.m_38897_(new SlotItemHandler(this.internal, 1, 94, 48) {
         private final int slot = 1;
      }));

      for (int si = 0; si < 3; si++) {
         for (int sj = 0; sj < 9; sj++) {
            this.m_38897_(new Slot(inv, sj + (si + 1) * 9, 6 + sj * 18, 98 + si * 18));
         }
      }

      for (int si = 0; si < 9; si++) {
         this.m_38897_(new Slot(inv, si, 6 + si * 18, 156));
      }
   }

   public boolean m_6875_(Player player) {
      if (this.bound) {
         if (this.boundItemMatcher != null) {
            return this.boundItemMatcher.get();
         }

         if (this.boundBlockEntity != null) {
            return AbstractContainerMenu.m_38889_(this.access, player, this.boundBlockEntity.m_58900_().m_60734_());
         }

         if (this.boundEntity != null) {
            return this.boundEntity.m_6084_();
         }
      }

      return true;
   }

   public ItemStack m_7648_(Player playerIn, int index) {
      ItemStack itemstack = ItemStack.f_41583_;
      Slot slot = (Slot)this.f_38839_.get(index);
      if (slot != null && slot.m_6657_()) {
         ItemStack itemstack1 = slot.m_7993_();
         itemstack = itemstack1.m_41777_();
         if (index < 2) {
            if (!this.m_38903_(itemstack1, 2, this.f_38839_.size(), true)) {
               return ItemStack.f_41583_;
            }

            slot.m_40234_(itemstack1, itemstack);
         } else if (!this.m_38903_(itemstack1, 0, 2, false)) {
            if (index < 29) {
               if (!this.m_38903_(itemstack1, 29, this.f_38839_.size(), true)) {
                  return ItemStack.f_41583_;
               }
            } else if (!this.m_38903_(itemstack1, 2, 29, false)) {
               return ItemStack.f_41583_;
            }

            return ItemStack.f_41583_;
         }

         if (itemstack1.m_41613_() == 0) {
            slot.m_5852_(ItemStack.f_41583_);
         } else {
            slot.m_6654_();
         }

         if (itemstack1.m_41613_() == itemstack.m_41613_()) {
            return ItemStack.f_41583_;
         }

         slot.m_142406_(playerIn, itemstack1);
      }

      return itemstack;
   }

   protected boolean m_38903_(ItemStack p_38904_, int p_38905_, int p_38906_, boolean p_38907_) {
      boolean flag = false;
      int i = p_38905_;
      if (p_38907_) {
         i = p_38906_ - 1;
      }

      if (p_38904_.m_41753_()) {
         while (!p_38904_.m_41619_() && (p_38907_ ? i >= p_38905_ : i < p_38906_)) {
            Slot slot = (Slot)this.f_38839_.get(i);
            ItemStack itemstack = slot.m_7993_();
            if (slot.m_5857_(itemstack) && !itemstack.m_41619_() && ItemStack.m_150942_(p_38904_, itemstack)) {
               int j = itemstack.m_41613_() + p_38904_.m_41613_();
               int maxSize = Math.min(slot.m_6641_(), p_38904_.m_41741_());
               if (j <= maxSize) {
                  p_38904_.m_41764_(0);
                  itemstack.m_41764_(j);
                  slot.m_5852_(itemstack);
                  flag = true;
               } else if (itemstack.m_41613_() < maxSize) {
                  p_38904_.m_41774_(maxSize - itemstack.m_41613_());
                  itemstack.m_41764_(maxSize);
                  slot.m_5852_(itemstack);
                  flag = true;
               }
            }

            if (p_38907_) {
               i--;
            } else {
               i++;
            }
         }
      }

      if (!p_38904_.m_41619_()) {
         if (p_38907_) {
            i = p_38906_ - 1;
         } else {
            i = p_38905_;
         }

         while (p_38907_ ? i >= p_38905_ : i < p_38906_) {
            Slot slot1 = (Slot)this.f_38839_.get(i);
            ItemStack itemstack1 = slot1.m_7993_();
            if (itemstack1.m_41619_() && slot1.m_5857_(p_38904_)) {
               if (p_38904_.m_41613_() > slot1.m_6641_()) {
                  slot1.m_269060_(p_38904_.m_41620_(slot1.m_6641_()));
               } else {
                  slot1.m_269060_(p_38904_.m_41620_(p_38904_.m_41613_()));
               }

               slot1.m_6654_();
               flag = true;
               break;
            }

            if (p_38907_) {
               i--;
            } else {
               i++;
            }
         }
      }

      return flag;
   }

   public void m_6877_(Player playerIn) {
      super.m_6877_(playerIn);
      if (!this.bound && playerIn instanceof ServerPlayer serverPlayer) {
         if (serverPlayer.m_6084_() && !serverPlayer.m_9232_()) {
            for (int i = 0; i < this.internal.getSlots(); i++) {
               playerIn.m_150109_().m_150079_(this.internal.extractItem(i, this.internal.getStackInSlot(i).m_41613_(), false));
            }
         } else {
            for (int j = 0; j < this.internal.getSlots(); j++) {
               playerIn.m_36176_(this.internal.extractItem(j, this.internal.getStackInSlot(j).m_41613_(), false), false);
            }
         }
      }
   }

   private void slotChanged(int slotid, int ctype, int meta) {
      if (this.world != null && this.world.m_5776_()) {
         ManMod.PACKET_HANDLER.sendToServer(new CassetteRecorderGuiSlotMessage(slotid, this.x, this.y, this.z, ctype, meta));
         CassetteRecorderGuiSlotMessage.handleSlotAction(this.entity, slotid, ctype, meta, this.x, this.y, this.z);
      }
   }

   public Map<Integer, Slot> get() {
      return this.customSlots;
   }
}
