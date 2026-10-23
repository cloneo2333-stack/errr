package vn.cavemind.world;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

public final class Assets {
    private static final Map<String, Block> CACHE = new HashMap<>();

    public static Block block(String id) {
        return CACHE.computeIfAbsent(id, k -> {
            ResourceLocation rl = ResourceLocation.tryParse(k);
            if (rl == null || !ForgeRegistries.BLOCKS.containsKey(rl)) return null;
            return ForgeRegistries.BLOCKS.getValue(rl);
        });
    }

    public static BlockState get(String id, BlockState fallback) {
        Block b = block(id);
        if (b == null) return fallback;
        BlockState st = b.defaultBlockState();
        for (String off : new String[]{"lit", "powered", "open", "tucked"}) st = flag(st, off, false);
        return st;
    }

    public static BlockState pick(BlockState fallback, String... ids) {
        for (String id : ids) {
            Block b = block(id);
            if (b != null) return get(id, fallback);
        }
        return fallback;
    }

    public static BlockState flag(BlockState st, String name, boolean v) {
        for (Property<?> p : st.getProperties()) {
            if (p.getName().equals(name) && p instanceof BooleanProperty bp) return st.setValue(bp, v);
        }
        return st;
    }

    public static BlockState face(BlockState st, Direction d) {
        for (Property<?> p : st.getProperties()) {
            if (p instanceof DirectionProperty dp && (p.getName().equals("facing") || p.getName().equals("horizontal_facing")) && dp.getPossibleValues().contains(d)) {
                return st.setValue(dp, d);
            }
        }
        return st;
    }

    public static BlockState door(String id, BlockState fallback) {
        Block b = block(id);
        if (b instanceof DoorBlock && b.defaultBlockState().hasProperty(BlockStateProperties.OPEN)) return b.defaultBlockState();
        return fallback;
    }

    public static boolean has(String id) {
        return block(id) != null;
    }

    private Assets() {}
}
