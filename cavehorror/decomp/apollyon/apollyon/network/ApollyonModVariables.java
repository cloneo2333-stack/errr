package apollyon.network;

import apollyon.ApollyonMod;
import java.util.ArrayList;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

@EventBusSubscriber(
   bus = Bus.MOD
)
public class ApollyonModVariables {
   public static final Capability<ApollyonModVariables.PlayerVariables> PLAYER_VARIABLES_CAPABILITY = CapabilityManager.get(
      new CapabilityToken<ApollyonModVariables.PlayerVariables>() {
      }
   );

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
      ApollyonMod.addNetworkMessage(
         ApollyonModVariables.SavedDataSyncMessage.class,
         ApollyonModVariables.SavedDataSyncMessage::buffer,
         ApollyonModVariables.SavedDataSyncMessage::new,
         ApollyonModVariables.SavedDataSyncMessage::handler
      );
      ApollyonMod.addNetworkMessage(
         ApollyonModVariables.PlayerVariablesSyncMessage.class,
         ApollyonModVariables.PlayerVariablesSyncMessage::buffer,
         ApollyonModVariables.PlayerVariablesSyncMessage::new,
         ApollyonModVariables.PlayerVariablesSyncMessage::handler
      );
   }

   @SubscribeEvent
   public static void init(RegisterCapabilitiesEvent event) {
      event.register(ApollyonModVariables.PlayerVariables.class);
   }

   @SubscribeEvent
   public static void registerMessage(FMLCommonSetupEvent event) {
      ApollyonMod.addNetworkMessage(
         ApollyonModVariables.PlayerVariablesSyncMessage.class,
         ApollyonModVariables.PlayerVariablesSyncMessage::buffer,
         ApollyonModVariables.PlayerVariablesSyncMessage::new,
         ApollyonModVariables.PlayerVariablesSyncMessage::handler
      );
   }

   @EventBusSubscriber
   public static class EventBusVariableHandlers {
      @SubscribeEvent
      public static void onPlayerLoggedInSyncPlayerVariables(PlayerLoggedInEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            for (Entity entityiterator : new ArrayList(event.getEntity().m_9236_().m_6907_())) {
               ((ApollyonModVariables.PlayerVariables)entityiterator.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                     .orElse(new ApollyonModVariables.PlayerVariables()))
                  .syncPlayerVariables(entityiterator);
            }
         }
      }

      @SubscribeEvent
      public static void onPlayerRespawnedSyncPlayerVariables(PlayerRespawnEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            for (Entity entityiterator : new ArrayList(event.getEntity().m_9236_().m_6907_())) {
               ((ApollyonModVariables.PlayerVariables)entityiterator.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                     .orElse(new ApollyonModVariables.PlayerVariables()))
                  .syncPlayerVariables(entityiterator);
            }
         }
      }

      @SubscribeEvent
      public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerChangedDimensionEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            for (Entity entityiterator : new ArrayList(event.getEntity().m_9236_().m_6907_())) {
               ((ApollyonModVariables.PlayerVariables)entityiterator.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                     .orElse(new ApollyonModVariables.PlayerVariables()))
                  .syncPlayerVariables(entityiterator);
            }
         }
      }

      @SubscribeEvent
      public static void clonePlayer(Clone event) {
         event.getOriginal().revive();
         ApollyonModVariables.PlayerVariables original = (ApollyonModVariables.PlayerVariables)event.getOriginal()
            .getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
            .orElse(new ApollyonModVariables.PlayerVariables());
         ApollyonModVariables.PlayerVariables clone = (ApollyonModVariables.PlayerVariables)event.getEntity()
            .getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
            .orElse(new ApollyonModVariables.PlayerVariables());
         if (!event.isWasDeath()) {
            clone.fallen_overlay = original.fallen_overlay;
         }

         if (!event.getEntity().m_9236_().m_5776_()) {
            for (Entity entityiterator : new ArrayList(event.getEntity().m_9236_().m_6907_())) {
               ((ApollyonModVariables.PlayerVariables)entityiterator.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                     .orElse(new ApollyonModVariables.PlayerVariables()))
                  .syncPlayerVariables(entityiterator);
            }
         }
      }

      @SubscribeEvent
      public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            SavedData mapdata = ApollyonModVariables.MapVariables.get(event.getEntity().m_9236_());
            SavedData worlddata = ApollyonModVariables.WorldVariables.get(event.getEntity().m_9236_());
            if (mapdata != null) {
               ApollyonMod.PACKET_HANDLER
                  .send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)event.getEntity()), new ApollyonModVariables.SavedDataSyncMessage(0, mapdata));
            }

            if (worlddata != null) {
               ApollyonMod.PACKET_HANDLER
                  .send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)event.getEntity()), new ApollyonModVariables.SavedDataSyncMessage(1, worlddata));
            }
         }
      }

      @SubscribeEvent
      public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            SavedData worlddata = ApollyonModVariables.WorldVariables.get(event.getEntity().m_9236_());
            if (worlddata != null) {
               ApollyonMod.PACKET_HANDLER
                  .send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)event.getEntity()), new ApollyonModVariables.SavedDataSyncMessage(1, worlddata));
            }
         }
      }
   }

   public static class MapVariables extends SavedData {
      public static final String DATA_NAME = "apollyon_mapvars";
      static ApollyonModVariables.MapVariables clientSide = new ApollyonModVariables.MapVariables();

      public static ApollyonModVariables.MapVariables load(CompoundTag tag) {
         ApollyonModVariables.MapVariables data = new ApollyonModVariables.MapVariables();
         data.read(tag);
         return data;
      }

      public void read(CompoundTag nbt) {
         if (nbt == null) {
            nbt = this.m_7176_(new CompoundTag());
         }
      }

      public CompoundTag m_7176_(CompoundTag nbt) {
         return nbt;
      }

      public void syncData(LevelAccessor world) {
         this.m_77762_();
         if (world instanceof Level && !world.m_5776_()) {
            ApollyonMod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new ApollyonModVariables.SavedDataSyncMessage(0, this));
         }
      }

      public static ApollyonModVariables.MapVariables get(LevelAccessor world) {
         return world instanceof ServerLevelAccessor serverLevelAcc
            ? (ApollyonModVariables.MapVariables)serverLevelAcc.m_6018_()
               .m_7654_()
               .m_129880_(Level.f_46428_)
               .m_8895_()
               .m_164861_(e -> load(e), ApollyonModVariables.MapVariables::new, "apollyon_mapvars")
            : clientSide;
      }
   }

   public static class PlayerVariables {
      public String fallen_overlay = "false";

      public void syncPlayerVariables(Entity entity) {
         if (entity instanceof ServerPlayer serverPlayer) {
            ApollyonMod.PACKET_HANDLER
               .send(PacketDistributor.DIMENSION.with(entity.m_9236_()::m_46472_), new ApollyonModVariables.PlayerVariablesSyncMessage(this, entity.m_19879_()));
         }
      }

      public Tag writeNBT() {
         CompoundTag nbt = new CompoundTag();
         nbt.m_128359_("fallen_overlay", this.fallen_overlay);
         return nbt;
      }

      public void readNBT(Tag tag) {
         if (tag == null) {
            tag = this.writeNBT();
         }

         CompoundTag nbt = (CompoundTag)tag;
         if (nbt == null) {
            nbt = (CompoundTag)this.writeNBT();
         }

         this.fallen_overlay = nbt.m_128461_("fallen_overlay");
      }
   }

   @EventBusSubscriber
   private static class PlayerVariablesProvider implements ICapabilitySerializable<Tag> {
      private final ApollyonModVariables.PlayerVariables playerVariables = new ApollyonModVariables.PlayerVariables();
      private final LazyOptional<ApollyonModVariables.PlayerVariables> instance = LazyOptional.of(() -> this.playerVariables);

      @SubscribeEvent
      public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
         if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer)) {
            event.addCapability(new ResourceLocation("apollyon", "player_variables"), new ApollyonModVariables.PlayerVariablesProvider());
         }
      }

      public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
         return cap == ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY ? this.instance.cast() : LazyOptional.empty();
      }

      public Tag serializeNBT() {
         return this.playerVariables.writeNBT();
      }

      public void deserializeNBT(Tag nbt) {
         this.playerVariables.readNBT(nbt);
      }
   }

   public static class PlayerVariablesSyncMessage {
      private final int target;
      private final ApollyonModVariables.PlayerVariables data;

      public PlayerVariablesSyncMessage(FriendlyByteBuf buffer) {
         this.data = new ApollyonModVariables.PlayerVariables();
         this.data.readNBT(buffer.m_130260_());
         this.target = buffer.readInt();
      }

      public PlayerVariablesSyncMessage(ApollyonModVariables.PlayerVariables data, int entityid) {
         this.data = data;
         this.target = entityid;
      }

      public static void buffer(ApollyonModVariables.PlayerVariablesSyncMessage message, FriendlyByteBuf buffer) {
         buffer.m_130079_((CompoundTag)message.data.writeNBT());
         buffer.writeInt(message.target);
      }

      public static void handler(ApollyonModVariables.PlayerVariablesSyncMessage message, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (!context.getDirection().getReceptionSide().isServer()) {
                  ApollyonModVariables.PlayerVariables variables = (ApollyonModVariables.PlayerVariables)Minecraft.m_91087_()
                     .f_91074_
                     .m_9236_()
                     .m_6815_(message.target)
                     .getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                     .orElse(new ApollyonModVariables.PlayerVariables());
                  variables.fallen_overlay = message.data.fallen_overlay;
               }
            }
         );
         context.setPacketHandled(true);
      }
   }

   public static class SavedDataSyncMessage {
      private final int type;
      private SavedData data;

      public SavedDataSyncMessage(FriendlyByteBuf buffer) {
         this.type = buffer.readInt();
         CompoundTag nbt = buffer.m_130260_();
         if (nbt != null) {
            this.data = (SavedData)(this.type == 0 ? new ApollyonModVariables.MapVariables() : new ApollyonModVariables.WorldVariables());
            if (this.data instanceof ApollyonModVariables.MapVariables mapVariables) {
               mapVariables.read(nbt);
            } else if (this.data instanceof ApollyonModVariables.WorldVariables worldVariables) {
               worldVariables.read(nbt);
            }
         }
      }

      public SavedDataSyncMessage(int type, SavedData data) {
         this.type = type;
         this.data = data;
      }

      public static void buffer(ApollyonModVariables.SavedDataSyncMessage message, FriendlyByteBuf buffer) {
         buffer.writeInt(message.type);
         if (message.data != null) {
            buffer.m_130079_(message.data.m_7176_(new CompoundTag()));
         }
      }

      public static void handler(ApollyonModVariables.SavedDataSyncMessage message, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer() && message.data != null) {
               if (message.type == 0) {
                  ApollyonModVariables.MapVariables.clientSide = (ApollyonModVariables.MapVariables)message.data;
               } else {
                  ApollyonModVariables.WorldVariables.clientSide = (ApollyonModVariables.WorldVariables)message.data;
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static class WorldVariables extends SavedData {
      public static final String DATA_NAME = "apollyon_worldvars";
      public boolean chase_music = true;
      public String spawn_volume = "soft";
      public String damned_spawning = "False";
      public boolean damned_fakeout = true;
      public boolean fallen_spawning = false;
      static ApollyonModVariables.WorldVariables clientSide = new ApollyonModVariables.WorldVariables();

      public static ApollyonModVariables.WorldVariables load(CompoundTag tag) {
         ApollyonModVariables.WorldVariables data = new ApollyonModVariables.WorldVariables();
         data.read(tag);
         return data;
      }

      public void read(CompoundTag nbt) {
         this.chase_music = nbt.m_128471_("chase_music");
         this.spawn_volume = nbt.m_128461_("spawn_volume");
         this.damned_spawning = nbt.m_128461_("damned_spawning");
         this.damned_fakeout = nbt.m_128471_("damned_fakeout");
         this.fallen_spawning = nbt.m_128471_("fallen_spawning");
      }

      public CompoundTag m_7176_(CompoundTag nbt) {
         nbt.m_128379_("chase_music", this.chase_music);
         nbt.m_128359_("spawn_volume", this.spawn_volume);
         nbt.m_128359_("damned_spawning", this.damned_spawning);
         nbt.m_128379_("damned_fakeout", this.damned_fakeout);
         nbt.m_128379_("fallen_spawning", this.fallen_spawning);
         return nbt;
      }

      public void syncData(LevelAccessor world) {
         this.m_77762_();
         if (world instanceof Level level && !level.m_5776_()) {
            ApollyonMod.PACKET_HANDLER.send(PacketDistributor.DIMENSION.with(level::m_46472_), new ApollyonModVariables.SavedDataSyncMessage(1, this));
         }
      }

      public static ApollyonModVariables.WorldVariables get(LevelAccessor world) {
         return world instanceof ServerLevel level
            ? (ApollyonModVariables.WorldVariables)level.m_8895_().m_164861_(e -> load(e), ApollyonModVariables.WorldVariables::new, "apollyon_worldvars")
            : clientSide;
      }
   }
}
