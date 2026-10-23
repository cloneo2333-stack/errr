package vn.cavemind.gear;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import vn.cavemind.CaveMind;
import vn.cavemind.chapter.Keys;

public final class Gear {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CaveMind.MODID);

    public static final RegistryObject<Block> CHIME_BLOCK = BLOCKS.register("chuong_gio_xuong", Chime::new);
    public static final RegistryObject<Block> SCRAWL = BLOCKS.register("chu_tuong", Scrawl::new);
    public static final RegistryObject<Item> CHIME = Keys.ITEMS.register("chuong_gio_xuong", () -> new BlockItem(CHIME_BLOCK.get(), new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)) {
        @Override
        public void appendHoverText(ItemStack st, Level lvl, List<Component> lines, TooltipFlag flag) {
            Thing.lines(getDescriptionId(), 3, lines);
        }
    });
    public static final RegistryObject<Item> ECHO = Keys.ITEMS.register("manh_da_vong_am", () -> new Thing(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), 2));
    public static final RegistryObject<Item> BONE = Keys.ITEMS.register("manh_xuong_suong", () -> new Thing(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), 2));
    public static final RegistryObject<Item> BARO = Keys.ITEMS.register("phong_vu_bieu_suong", Barometer::new);
    public static final RegistryObject<Item> LISTEN = Keys.ITEMS.register("may_thu_am", Listener::new);
    public static final RegistryObject<Item> DECOY = Keys.ITEMS.register("bi_day_cot", Decoy::new);
    public static final RegistryObject<Item> FINDER = Keys.ITEMS.register("lo_dom_dom", Finder::new);

    public static void init() {}

    public static class Thing extends Item {
        final int lines;

        public Thing(Item.Properties props, int lines) {
            super(props);
            this.lines = lines;
        }

        static void lines(String key, int n, List<Component> out) {
            for (int i = 1; i <= n; i++) out.add(Component.translatable(key + ".desc" + i).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }

        @Override
        public void appendHoverText(ItemStack st, Level lvl, List<Component> out, TooltipFlag flag) {
            lines(getDescriptionId(), lines, out);
        }
    }

    private Gear() {}
}
