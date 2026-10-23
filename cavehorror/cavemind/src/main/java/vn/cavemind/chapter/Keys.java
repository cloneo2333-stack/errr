package vn.cavemind.chapter;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import vn.cavemind.CaveMind;

public final class Keys {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CaveMind.MODID);

    public static final RegistryObject<Item> LINH = ITEMS.register("con_quay_linh", () -> new Toy(2));
    public static final RegistryObject<Item> TUAN = ITEMS.register("bi_ve_tuan", () -> new Toy(3));
    public static final RegistryObject<Item> MAI = ITEMS.register("dong_ho_mai", () -> new Toy(4));
    public static final RegistryObject<Item> BAO = ITEMS.register("dieu_giay_bao", () -> new Toy(5));
    public static final RegistryObject<Item> NAM = ITEMS.register("coi_go_nam", () -> new Toy(6));
    public static final RegistryObject<Item> PIN = ITEMS.register("pin_may_quay", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SO = ITEMS.register("so_chuong", Book.BookItem::new);
    public static final RegistryObject<Item> PAGE = ITEMS.register("trang_nhat_ky", () -> new vn.cavemind.lore.Pages.PageItem());

    public static Item of(int gate) {
        return switch (gate) {
            case 2 -> LINH.get();
            case 3 -> TUAN.get();
            case 4 -> MAI.get();
            case 5 -> BAO.get();
            case 6 -> NAM.get();
            default -> null;
        };
    }

    public static int gateOf(ItemStack st) {
        return st.getItem() instanceof Toy t ? t.gate : 0;
    }

    public static final class Toy extends Item {
        final int gate;

        Toy(int gate) {
            super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
            this.gate = gate;
        }

        @Override
        public void appendHoverText(ItemStack st, Level lvl, List<Component> lines, TooltipFlag flag) {
            String key = getDescriptionId() + ".desc";
            for (int i = 1; i <= 3; i++) lines.add(Component.translatable(key + i).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            lines.add(Component.translatable("cavemind.toy.gate", gate).withStyle(ChatFormatting.DARK_PURPLE));
        }

        @Override
        public int getEntityLifespan(ItemStack st, Level lvl) {
            return 20 * 60 * 30;
        }
    }

    private Keys() {}
}
