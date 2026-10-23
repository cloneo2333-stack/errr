package vn.cavemind.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record Moment(String kind, long day, int time, BlockPos pos, int a, int b) {
    public static final String UNSEEN = "unseen";
    public static final String SEEN = "seen";
    public static final String LISTENED = "listened";
    public static final String TURNED = "turned";
    public static final String DIED = "died";
    public static final String SLEPT = "slept";
    public static final String RAN = "ran";
    public static final String HURT_IT = "hurt";
    public static final String ESCAPED = "escaped";
    public static final String TOOK = "took";
    public static final String FOLLOWED = "followed";
    public static final String HOME = "home";
    public static final String NOON = "noon";
    public static final String ECHOED = "echoed";
    public static final String RITUAL = "ritual";
    public static final String TWIN = "twin";
    public static final String SEEK = "seek";
    public static final String PRINTS = "prints";
    public static final String TORCHES = "torches";
    public static final String INSIDE = "inside";
    public static final String DOOR = "door";
    public static final String SITIN = "sitin";
    public static final String WINDOW = "window";
    public static final String TRAIL = "trail";
    public static final String NEST = "nest";
    public static final String SPOIL = "spoil";
    public static final String HARVEST = "harvest";
    public static final String PEEP = "peep";
    public static final String SEAL = "seal";
    public static final String WAKE = "wake";
    public static final String ANIMAL = "animal";
    public static final String LURED = "lured";
    public static final String FRENZY = "frenzy";
    public static final String HERO = "hero";
    public static final String HERO_SIGN = "hero_sign";
    public static final String HERO_TUNNEL = "hero_tunnel";
    public static final String HERO_BUILD = "hero_build";
    public static final String HERO_LEAFLESS = "hero_leafless";
    public static final String BELL = "bell";
    public static final String HID = "hid";
    public static final String LORE = "lore";
    public static final String CAUGHT = "caught";
    public static final String MAZE = "maze";

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("k", kind);
        t.putLong("d", day);
        t.putInt("t", time);
        t.putLong("p", pos.asLong());
        t.putInt("a", a);
        t.putInt("b", b);
        return t;
    }

    public static Moment load(CompoundTag t) {
        return new Moment(t.getString("k"), t.getLong("d"), t.getInt("t"), BlockPos.of(t.getLong("p")), t.getInt("a"), t.getInt("b"));
    }
}
