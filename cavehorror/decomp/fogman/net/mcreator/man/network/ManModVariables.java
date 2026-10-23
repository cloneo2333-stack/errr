package net.mcreator.man.network;

import java.util.function.Supplier;
import net.mcreator.man.ManMod;
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
public class ManModVariables {
   public static double timer_creeping = 0.0;
   public static double attack = 0.0;
   public static double amplifier_despawn = 0.0;
   public static boolean reset_end = false;
   public static boolean third_time = false;
   public static boolean in_water = false;
   public static double random_sound_spawn = 0.0;
   public static double fakeman_looked = 0.0;
   public static boolean jumpscare_sound = false;
   public static boolean dirty_lens = false;
   public static boolean jp_ig_1 = false;
   public static boolean jp_ig_2 = false;
   public static boolean play_safety_instructions = false;
   public static boolean renderfog_spawn = false;
   public static boolean renderfog_chase = false;
   public static final Capability<ManModVariables.PlayerVariables> PLAYER_VARIABLES_CAPABILITY = CapabilityManager.get(
      new CapabilityToken<ManModVariables.PlayerVariables>() {
      }
   );

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
      ManMod.addNetworkMessage(
         ManModVariables.SavedDataSyncMessage.class,
         ManModVariables.SavedDataSyncMessage::buffer,
         ManModVariables.SavedDataSyncMessage::new,
         ManModVariables.SavedDataSyncMessage::handler
      );
      ManMod.addNetworkMessage(
         ManModVariables.PlayerVariablesSyncMessage.class,
         ManModVariables.PlayerVariablesSyncMessage::buffer,
         ManModVariables.PlayerVariablesSyncMessage::new,
         ManModVariables.PlayerVariablesSyncMessage::handler
      );
   }

   @SubscribeEvent
   public static void init(RegisterCapabilitiesEvent event) {
      event.register(ManModVariables.PlayerVariables.class);
   }

   @EventBusSubscriber
   public static class EventBusVariableHandlers {
      @SubscribeEvent
      public static void onPlayerLoggedInSyncPlayerVariables(PlayerLoggedInEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            ((ManModVariables.PlayerVariables)event.getEntity()
                  .getCapability(ManModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                  .orElse(new ManModVariables.PlayerVariables()))
               .syncPlayerVariables(event.getEntity());
         }
      }

      @SubscribeEvent
      public static void onPlayerRespawnedSyncPlayerVariables(PlayerRespawnEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            ((ManModVariables.PlayerVariables)event.getEntity()
                  .getCapability(ManModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                  .orElse(new ManModVariables.PlayerVariables()))
               .syncPlayerVariables(event.getEntity());
         }
      }

      @SubscribeEvent
      public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerChangedDimensionEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            ((ManModVariables.PlayerVariables)event.getEntity()
                  .getCapability(ManModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                  .orElse(new ManModVariables.PlayerVariables()))
               .syncPlayerVariables(event.getEntity());
         }
      }

      @SubscribeEvent
      public static void clonePlayer(Clone event) {
         event.getOriginal().revive();
         ManModVariables.PlayerVariables original = (ManModVariables.PlayerVariables)event.getOriginal()
            .getCapability(ManModVariables.PLAYER_VARIABLES_CAPABILITY, null)
            .orElse(new ManModVariables.PlayerVariables());
         ManModVariables.PlayerVariables clone = (ManModVariables.PlayerVariables)event.getEntity()
            .getCapability(ManModVariables.PLAYER_VARIABLES_CAPABILITY, null)
            .orElse(new ManModVariables.PlayerVariables());
         clone.sanity_timer = original.sanity_timer;
         clone.sanity = original.sanity;
         clone.bed_clicked = original.bed_clicked;
         clone.blackout_overlay = original.blackout_overlay;
         clone.book = original.book;
         clone.tear = original.tear;
         clone.FOVDefault = original.FOVDefault;
         if (!event.isWasDeath()) {
         }
      }

      @SubscribeEvent
      public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            SavedData mapdata = ManModVariables.MapVariables.get(event.getEntity().m_9236_());
            SavedData worlddata = ManModVariables.WorldVariables.get(event.getEntity().m_9236_());
            if (mapdata != null) {
               ManMod.PACKET_HANDLER
                  .send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)event.getEntity()), new ManModVariables.SavedDataSyncMessage(0, mapdata));
            }

            if (worlddata != null) {
               ManMod.PACKET_HANDLER
                  .send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)event.getEntity()), new ManModVariables.SavedDataSyncMessage(1, worlddata));
            }
         }
      }

      @SubscribeEvent
      public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
         if (!event.getEntity().m_9236_().m_5776_()) {
            SavedData worlddata = ManModVariables.WorldVariables.get(event.getEntity().m_9236_());
            if (worlddata != null) {
               ManMod.PACKET_HANDLER
                  .send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)event.getEntity()), new ManModVariables.SavedDataSyncMessage(1, worlddata));
            }
         }
      }
   }

   public static class MapVariables extends SavedData {
      public static final String DATA_NAME = "man_mapvars";
      public double timer_global = 0.0;
      public double reset = 5000.0;
      public double reset_creeping = 0.0;
      public boolean killed = false;
      public double overlay_death = 0.0;
      public double multiplier_ring = 0.0;
      public double ring_timer = 0.0;
      public boolean looked = false;
      public double ritual_particles = 0.0;
      public boolean new_world = false;
      public boolean fakeman_sanity = false;
      public double x_sanity = 0.0;
      public double y_sanity = 0.0;
      public double z_sanity = 0.0;
      public double overlay_ingame = 0.0;
      public boolean SpawnHangMan = false;
      static ManModVariables.MapVariables clientSide = new ManModVariables.MapVariables();

      public static ManModVariables.MapVariables load(CompoundTag tag) {
         ManModVariables.MapVariables data = new ManModVariables.MapVariables();
         data.read(tag);
         return data;
      }

      public void read(CompoundTag nbt) {
         this.timer_global = nbt.m_128459_("timer_global");
         this.reset = nbt.m_128459_("reset");
         this.reset_creeping = nbt.m_128459_("reset_creeping");
         this.killed = nbt.m_128471_("killed");
         this.overlay_death = nbt.m_128459_("overlay_death");
         this.multiplier_ring = nbt.m_128459_("multiplier_ring");
         this.ring_timer = nbt.m_128459_("ring_timer");
         this.looked = nbt.m_128471_("looked");
         this.ritual_particles = nbt.m_128459_("ritual_particles");
         this.new_world = nbt.m_128471_("new_world");
         this.fakeman_sanity = nbt.m_128471_("fakeman_sanity");
         this.x_sanity = nbt.m_128459_("x_sanity");
         this.y_sanity = nbt.m_128459_("y_sanity");
         this.z_sanity = nbt.m_128459_("z_sanity");
         this.overlay_ingame = nbt.m_128459_("overlay_ingame");
         this.SpawnHangMan = nbt.m_128471_("SpawnHangMan");
      }

      public CompoundTag m_7176_(CompoundTag nbt) {
         nbt.m_128347_("timer_global", this.timer_global);
         nbt.m_128347_("reset", this.reset);
         nbt.m_128347_("reset_creeping", this.reset_creeping);
         nbt.m_128379_("killed", this.killed);
         nbt.m_128347_("overlay_death", this.overlay_death);
         nbt.m_128347_("multiplier_ring", this.multiplier_ring);
         nbt.m_128347_("ring_timer", this.ring_timer);
         nbt.m_128379_("looked", this.looked);
         nbt.m_128347_("ritual_particles", this.ritual_particles);
         nbt.m_128379_("new_world", this.new_world);
         nbt.m_128379_("fakeman_sanity", this.fakeman_sanity);
         nbt.m_128347_("x_sanity", this.x_sanity);
         nbt.m_128347_("y_sanity", this.y_sanity);
         nbt.m_128347_("z_sanity", this.z_sanity);
         nbt.m_128347_("overlay_ingame", this.overlay_ingame);
         nbt.m_128379_("SpawnHangMan", this.SpawnHangMan);
         return nbt;
      }

      public void syncData(LevelAccessor world) {
         this.m_77762_();
         if (world instanceof Level && !world.m_5776_()) {
            ManMod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new ManModVariables.SavedDataSyncMessage(0, this));
         }
      }

      public static ManModVariables.MapVariables get(LevelAccessor world) {
         return world instanceof ServerLevelAccessor serverLevelAcc
            ? (ManModVariables.MapVariables)serverLevelAcc.m_6018_()
               .m_7654_()
               .m_129880_(Level.f_46428_)
               .m_8895_()
               .m_164861_(e -> load(e), ManModVariables.MapVariables::new, "man_mapvars")
            : clientSide;
      }
   }

   public static class PlayerVariables {
      public double sanity_timer = 0.0;
      public double sanity = 1000.0;
      public double bed_clicked = 0.0;
      public boolean blackout_overlay = false;
      public boolean book = false;
      public boolean tear = false;
      public boolean FOVDefault = false;

      public void syncPlayerVariables(Entity entity) {
         if (entity instanceof ServerPlayer serverPlayer) {
            ManMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ManModVariables.PlayerVariablesSyncMessage(this));
         }
      }

      public Tag writeNBT() {
         CompoundTag nbt = new CompoundTag();
         nbt.m_128347_("sanity_timer", this.sanity_timer);
         nbt.m_128347_("sanity", this.sanity);
         nbt.m_128347_("bed_clicked", this.bed_clicked);
         nbt.m_128379_("blackout_overlay", this.blackout_overlay);
         nbt.m_128379_("book", this.book);
         nbt.m_128379_("tear", this.tear);
         nbt.m_128379_("FOVDefault", this.FOVDefault);
         return nbt;
      }

      public void readNBT(Tag tag) {
         CompoundTag nbt = (CompoundTag)tag;
         this.sanity_timer = nbt.m_128459_("sanity_timer");
         this.sanity = nbt.m_128459_("sanity");
         this.bed_clicked = nbt.m_128459_("bed_clicked");
         this.blackout_overlay = nbt.m_128471_("blackout_overlay");
         this.book = nbt.m_128471_("book");
         this.tear = nbt.m_128471_("tear");
         this.FOVDefault = nbt.m_128471_("FOVDefault");
      }
   }

   @EventBusSubscriber
   private static class PlayerVariablesProvider implements ICapabilitySerializable<Tag> {
      private final ManModVariables.PlayerVariables playerVariables = new ManModVariables.PlayerVariables();
      private final LazyOptional<ManModVariables.PlayerVariables> instance = LazyOptional.of(() -> this.playerVariables);

      @SubscribeEvent
      public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
         if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer)) {
            event.addCapability(new ResourceLocation("man", "player_variables"), new ManModVariables.PlayerVariablesProvider());
         }
      }

      public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
         return cap == ManModVariables.PLAYER_VARIABLES_CAPABILITY ? this.instance.cast() : LazyOptional.empty();
      }

      public Tag serializeNBT() {
         return this.playerVariables.writeNBT();
      }

      public void deserializeNBT(Tag nbt) {
         this.playerVariables.readNBT(nbt);
      }
   }

   public static class PlayerVariablesSyncMessage {
      private final ManModVariables.PlayerVariables data;

      public PlayerVariablesSyncMessage(FriendlyByteBuf buffer) {
         this.data = new ManModVariables.PlayerVariables();
         this.data.readNBT(buffer.m_130260_());
      }

      public PlayerVariablesSyncMessage(ManModVariables.PlayerVariables data) {
         this.data = data;
      }

      public static void buffer(ManModVariables.PlayerVariablesSyncMessage message, FriendlyByteBuf buffer) {
         buffer.m_130079_((CompoundTag)message.data.writeNBT());
      }

      public static void handler(ManModVariables.PlayerVariablesSyncMessage message, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(
            () -> {
               if (!context.getDirection().getReceptionSide().isServer()) {
                  ManModVariables.PlayerVariables variables = (ManModVariables.PlayerVariables)Minecraft.m_91087_()
                     .f_91074_
                     .getCapability(ManModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                     .orElse(new ManModVariables.PlayerVariables());
                  variables.sanity_timer = message.data.sanity_timer;
                  variables.sanity = message.data.sanity;
                  variables.bed_clicked = message.data.bed_clicked;
                  variables.blackout_overlay = message.data.blackout_overlay;
                  variables.book = message.data.book;
                  variables.tear = message.data.tear;
                  variables.FOVDefault = message.data.FOVDefault;
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
            this.data = (SavedData)(this.type == 0 ? new ManModVariables.MapVariables() : new ManModVariables.WorldVariables());
            if (this.data instanceof ManModVariables.MapVariables mapVariables) {
               mapVariables.read(nbt);
            } else if (this.data instanceof ManModVariables.WorldVariables worldVariables) {
               worldVariables.read(nbt);
            }
         }
      }

      public SavedDataSyncMessage(int type, SavedData data) {
         this.type = type;
         this.data = data;
      }

      public static void buffer(ManModVariables.SavedDataSyncMessage message, FriendlyByteBuf buffer) {
         buffer.writeInt(message.type);
         if (message.data != null) {
            buffer.m_130079_(message.data.m_7176_(new CompoundTag()));
         }
      }

      public static void handler(ManModVariables.SavedDataSyncMessage message, Supplier<Context> contextSupplier) {
         Context context = contextSupplier.get();
         context.enqueueWork(() -> {
            if (!context.getDirection().getReceptionSide().isServer() && message.data != null) {
               if (message.type == 0) {
                  ManModVariables.MapVariables.clientSide = (ManModVariables.MapVariables)message.data;
               } else {
                  ManModVariables.WorldVariables.clientSide = (ManModVariables.WorldVariables)message.data;
               }
            }
         });
         context.setPacketHandled(true);
      }
   }

   public static class WorldVariables extends SavedData {
      public static final String DATA_NAME = "man_worldvars";
      static ManModVariables.WorldVariables clientSide = new ManModVariables.WorldVariables();

      public static ManModVariables.WorldVariables load(CompoundTag tag) {
         ManModVariables.WorldVariables data = new ManModVariables.WorldVariables();
         data.read(tag);
         return data;
      }

      public void read(CompoundTag nbt) {
      }

      public CompoundTag m_7176_(CompoundTag nbt) {
         return nbt;
      }

      public void syncData(LevelAccessor world) {
         this.m_77762_();
         if (world instanceof Level level && !level.m_5776_()) {
            ManMod.PACKET_HANDLER.send(PacketDistributor.DIMENSION.with(level::m_46472_), new ManModVariables.SavedDataSyncMessage(1, this));
         }
      }

      public static ManModVariables.WorldVariables get(LevelAccessor world) {
         return world instanceof ServerLevel level
            ? (ManModVariables.WorldVariables)level.m_8895_().m_164861_(e -> load(e), ManModVariables.WorldVariables::new, "man_worldvars")
            : clientSide;
      }
   }
}
