package vn.cavemind.net;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import vn.cavemind.CaveMind;
import vn.cavemind.sense.Watch;

public final class Net {
    private static final String V = "2";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(CaveMind.MODID, "main"), () -> V, NetworkRegistry.acceptMissingOr(V), NetworkRegistry.acceptMissingOr(V));

    public static final int FOG = 1;
    public static final int SILENCE = 2;
    public static final int SHAKE = 3;
    public static final int PULSE = 4;

    public static void init() {
        CH.messageBuilder(View.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(View::write).decoder(View::new).consumerMainThread(View::handle).add();
        CH.messageBuilder(Fx.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Fx::write).decoder(Fx::new).consumerMainThread(Fx::handle).add();
        CH.messageBuilder(Hero.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Hero::write).decoder(Hero::new).consumerMainThread(Hero::handle).add();
        CH.messageBuilder(HeroAct.class, 3, NetworkDirection.PLAY_TO_SERVER)
            .encoder(HeroAct::write).decoder(HeroAct::new).consumerMainThread(HeroAct::handle).add();
    }

    public static boolean modded(ServerPlayer p) {
        return CH.isRemotePresent(p.connection.connection);
    }

    public static void hero(ServerPlayer p, Hero h) {
        if (!modded(p)) return;
        CH.send(PacketDistributor.PLAYER.with(() -> p), h);
    }

    public record Hero(int op, int slot, double x, double y, double z, float yaw, float head, float pitch, int flags, String str, long data) {
        public static final int SPAWN = 0, MOVE = 1, STATE = 2, ITEM = 3, SWING = 4, SLEEP = 5, WAKE = 6, REMOVE = 7;

        public Hero(FriendlyByteBuf b) {
            this(b.readByte(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat(), b.readFloat(),
                b.readVarInt(), b.readUtf(256), b.readLong());
        }

        public void write(FriendlyByteBuf b) {
            b.writeByte(op);
            b.writeVarInt(slot);
            b.writeDouble(x);
            b.writeDouble(y);
            b.writeDouble(z);
            b.writeFloat(yaw);
            b.writeFloat(head);
            b.writeFloat(pitch);
            b.writeVarInt(flags);
            b.writeUtf(str == null ? "" : str, 256);
            b.writeLong(data);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> vn.cavemind.client.HeroClient.on(this));
            ctx.get().setPacketHandled(true);
        }
    }

    public record HeroAct(int op, int slot) {
        public static final int HIT = 0, SEEN = 1;

        public HeroAct(FriendlyByteBuf b) {
            this(b.readByte(), b.readVarInt());
        }

        public void write(FriendlyByteBuf b) {
            b.writeByte(op);
            b.writeVarInt(slot);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) vn.cavemind.hero.Herobrine.act(p, op, slot);
            ctx.get().setPacketHandled(true);
        }
    }

    public static void fx(ServerPlayer p, int kind, float a, int ticks) {
        if (!CH.isRemotePresent(p.connection.connection)) return;
        CH.send(PacketDistributor.PLAYER.with(() -> p), new Fx(kind, a, ticks));
    }

    public record View(float fov, float aspect) {
        public View(FriendlyByteBuf b) {
            this(b.readFloat(), b.readFloat());
        }

        public void write(FriendlyByteBuf b) {
            b.writeFloat(fov);
            b.writeFloat(aspect);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer p = ctx.get().getSender();
            if (p != null) {
                Watch w = Watch.of(p);
                w.fov = Math.max(30f, Math.min(130f, fov));
                w.aspect = Math.max(0.5f, Math.min(4f, aspect));
                w.modded = true;
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public record Fx(int kind, float a, int ticks) {
        public Fx(FriendlyByteBuf b) {
            this(b.readVarInt(), b.readFloat(), b.readVarInt());
        }

        public void write(FriendlyByteBuf b) {
            b.writeVarInt(kind);
            b.writeFloat(a);
            b.writeVarInt(ticks);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> vn.cavemind.client.ClientFx.on(kind, a, ticks));
            ctx.get().setPacketHandled(true);
        }
    }

    private Net() {}
}
