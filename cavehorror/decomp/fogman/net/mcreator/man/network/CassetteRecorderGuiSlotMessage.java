package net.mcreator.man.network;

import java.util.HashMap;
import java.util.function.Supplier;
import net.mcreator.man.ManMod;
import net.mcreator.man.procedures.CassettestopProcedure;
import net.mcreator.man.world.inventory.CassetteRecorderGuiMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent.Context;

@EventBusSubscriber(
   bus = Bus.MOD
)
public class CassetteRecorderGuiSlotMessage {
   private final int slotID;
   private final int x;
   private final int y;
   private final int z;
   private final int changeType;
   private final int meta;

   public CassetteRecorderGuiSlotMessage(int slotID, int x, int y, int z, int changeType, int meta) {
      this.slotID = slotID;
      this.x = x;
      this.y = y;
      this.z = z;
      this.changeType = changeType;
      this.meta = meta;
   }

   public CassetteRecorderGuiSlotMessage(FriendlyByteBuf buffer) {
      this.slotID = buffer.readInt();
      this.x = buffer.readInt();
      this.y = buffer.readInt();
      this.z = buffer.readInt();
      this.changeType = buffer.readInt();
      this.meta = buffer.readInt();
   }

   public static void buffer(CassetteRecorderGuiSlotMessage message, FriendlyByteBuf buffer) {
      buffer.writeInt(message.slotID);
      buffer.writeInt(message.x);
      buffer.writeInt(message.y);
      buffer.writeInt(message.z);
      buffer.writeInt(message.changeType);
      buffer.writeInt(message.meta);
   }

   public static void handler(CassetteRecorderGuiSlotMessage message, Supplier<Context> contextSupplier) {
      Context context = contextSupplier.get();
      context.enqueueWork(() -> {
         Player entity = context.getSender();
         int slotID = message.slotID;
         int changeType = message.changeType;
         int meta = message.meta;
         int x = message.x;
         int y = message.y;
         int z = message.z;
         handleSlotAction(entity, slotID, changeType, meta, x, y, z);
      });
      context.setPacketHandled(true);
   }

   public static void handleSlotAction(Player entity, int slot, int changeType, int meta, int x, int y, int z) {
      Level world = entity.m_9236_();
      HashMap guistate = CassetteRecorderGuiMenu.guistate;
      if (world.m_46805_(new BlockPos(x, y, z))) {
         if (slot == 0 && changeType == 0) {
            CassettestopProcedure.execute(world, (double)x, (double)y, (double)z, entity);
         }
      }
   }

   @SubscribeEvent
   public static void registerMessage(FMLCommonSetupEvent event) {
      ManMod.addNetworkMessage(
         CassetteRecorderGuiSlotMessage.class,
         CassetteRecorderGuiSlotMessage::buffer,
         CassetteRecorderGuiSlotMessage::new,
         CassetteRecorderGuiSlotMessage::handler
      );
   }
}
