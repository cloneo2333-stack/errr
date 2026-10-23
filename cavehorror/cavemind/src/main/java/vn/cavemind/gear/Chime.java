package vn.cavemind.gear;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import vn.cavemind.world.Fx;

public final class Chime extends Block {
    static final VoxelShape SHAPE = Block.box(3, 1, 3, 13, 16, 13);
    public static final int USES = 3;

    Chime() {
        super(BlockBehaviour.Properties.of().strength(0.3f).sound(SoundType.BONE_BLOCK).noOcclusion().noCollission());
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter lvl, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void onPlace(BlockState st, Level lvl, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(st, lvl, pos, old, moving);
        if (lvl instanceof ServerLevel sl && !old.is(this) && sl.dimension() == Level.OVERWORLD) Data.get(sl.getServer()).add(pos);
    }

    @Override
    public void onRemove(BlockState st, Level lvl, BlockPos pos, BlockState now, boolean moving) {
        if (lvl instanceof ServerLevel sl && !now.is(this) && sl.dimension() == Level.OVERWORLD) Data.get(sl.getServer()).remove(pos);
        super.onRemove(st, lvl, pos, now, moving);
    }

    static final class Data extends SavedData {
        final Map<Long, Integer> worn = new HashMap<>();
        final Map<Long, Long> rang = new HashMap<>();

        static Data get(MinecraftServer s) {
            return s.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "cavemind_chimes");
        }

        static Data load(CompoundTag t) {
            Data d = new Data();
            for (Tag e : t.getList("c", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) e;
                d.worn.put(c.getLong("p"), c.getInt("w"));
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            ListTag l = new ListTag();
            worn.forEach((k, v) -> {
                CompoundTag c = new CompoundTag();
                c.putLong("p", k);
                c.putInt("w", v);
                l.add(c);
            });
            t.put("c", l);
            return t;
        }

        void add(BlockPos p) {
            worn.putIfAbsent(p.asLong(), 0);
            setDirty();
        }

        void remove(BlockPos p) {
            if (worn.remove(p.asLong()) != null) setDirty();
        }
    }

    public static int count(MinecraftServer s) {
        return Data.get(s).worn.size();
    }

    static void ring(ServerLevel lvl, BlockPos q, float vol) {
        Vec3 c = Vec3.atCenterOf(q);
        var r = lvl.getRandom();
        for (int i = 0; i < 4; i++) {
            int k = i;
            Fx.later(lvl, i * 4 + r.nextInt(3), () -> Fx.soundAll(lvl, k % 2 == 0 ? "minecraft:block.amethyst_block.chime" : "minecraft:block.bone_block.hit", SoundSource.BLOCKS, c, vol, 0.55f + r.nextFloat() * 0.35f));
        }
    }

    public static void tick(MinecraftServer server, long now) {
        if (now % 20 != 7) return;
        Data d = Data.get(server);
        if (d.worn.isEmpty()) return;
        Vec3 man = Barometer.man(server);
        if (man == null) return;
        ServerLevel ow = server.overworld();
        for (long k : new java.util.ArrayList<>(d.worn.keySet())) {
            BlockPos q = BlockPos.of(k);
            if (Vec3.atCenterOf(q).distanceTo(man) > 45 || !ow.isLoaded(q)) continue;
            if (!ow.getBlockState(q).is(Gear.CHIME_BLOCK.get())) {
                d.remove(q);
                continue;
            }
            if (now - d.rang.getOrDefault(k, -10000L) < 160) continue;
            d.rang.put(k, now);
            ring(ow, q, 0.9f);
            for (ServerPlayer p : ow.players()) {
                if (p.distanceToSqr(Vec3.atCenterOf(q)) < 40 * 40) p.displayClientMessage(Component.literal("Chuông gió xương tự leng keng. Có thứ gì đó đang ở quanh đây.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }
    }

    public static boolean guard(ServerLevel lvl, BlockPos at, ServerPlayer p) {
        if (lvl.dimension() != Level.OVERWORLD) return false;
        Data d = Data.get(lvl.getServer());
        for (long k : new java.util.ArrayList<>(d.worn.keySet())) {
            BlockPos q = BlockPos.of(k);
            if (q.distSqr(at) > 16 * 16 || !lvl.isLoaded(q)) continue;
            if (!lvl.getBlockState(q).is(Gear.CHIME_BLOCK.get())) {
                d.remove(q);
                continue;
            }
            int w = d.worn.getOrDefault(k, 0) + 1;
            ring(lvl, q, 1.0f);
            if (w >= USES) {
                lvl.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                Block.popResource(lvl, q, new ItemStack(Items.STRING));
                Block.popResource(lvl, q, new ItemStack(Gear.BONE.get()));
                d.remove(q);
                if (p != null) p.displayClientMessage(Component.literal("Chuông gió xương leng keng dữ dội rồi đứt dây. Nó đã đuổi hắn đi lần cuối.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
            } else {
                d.worn.put(k, w);
                d.setDirty();
                if (p != null) p.displayClientMessage(Component.literal("Chuông gió xương leng keng. Có thứ gì đó vừa bỏ đi. (dây còn chịu được " + (USES - w) + " lần)").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            }
            return true;
        }
        return false;
    }
}
