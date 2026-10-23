package vn.cavemind.chapter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

public final class Gifts {
    static final String[][] WEAPONS = {
        {},
        {"dixtas_armory:iron_katana", "dixtas_armory:iron_rapier", "dixtas_armory:iron_glaive", "dixtas_armory:iron_twinblade", "vtaw_mw:iron_longsword", "born_in_chaos_v1:spider_bite_sword"},
        {"dixtas_armory:silver_katana", "dixtas_armory:silver_longsword", "dixtas_armory:silver_halberd", "born_in_chaos_v1:skullbreaker_hammer", "born_in_chaos_v1:soul_cutlass", "vtaw_mw:iron_greataxe"},
        {"dixtas_armory:diamond_katana", "dixtas_armory:diamond_glaive", "dixtas_armory:diamond_twinblade", "born_in_chaos_v1:spiritual_sword", "born_in_chaos_v1:frostbitten_blade", "vtaw_mw:diamond_greatsword"},
        {"dixtas_armory:necromium_katana", "dixtas_armory:necromium_longsword", "born_in_chaos_v1:great_reaper_axe", "arphex:mantis_machete", "arphex:abyssal_dagger", "fathomless:sanguine_dagger"},
        {"dixtas_armory:netherite_katana", "dixtas_armory:netherite_glaive", "born_in_chaos_v1:nightmare_scythe", "fathomless:chrysalised_greatblade", "vtaw_mw:netherite_greatsword", "dixtas_armory:netherite_twinblade"},
        {"dixtas_armory:adamantite_katana", "dixtas_armory:adamantite_greatsword", "dixtas_armory:adamantite_halberd", "arphex:visionary_spear", "arphex:spear_of_paralysis", "fathomless:chrysalised_spear"}
    };
    static final Item[] FALLBACK = {Items.IRON_SWORD, Items.IRON_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.NETHERITE_SWORD};
    static final Object[][][] ENCH = {
        {},
        {{"sharpness", 2}, {"unbreaking", 2}},
        {{"sharpness", 3}, {"unbreaking", 2}, {"looting", 1}},
        {{"sharpness", 3}, {"unbreaking", 3}, {"looting", 1}, {"fire_aspect", 1}},
        {{"sharpness", 4}, {"unbreaking", 3}, {"looting", 2}},
        {{"sharpness", 4}, {"unbreaking", 3}, {"looting", 2}, {"fire_aspect", 2}},
        {{"sharpness", 5}, {"unbreaking", 3}, {"looting", 3}, {"mending", 1}}
    };
    static final int[] COLOR = {0, 0x9fc5e8, 0x9fc5e8, 0x7fe0c0, 0xc792ea, 0xc792ea, 0xffcf5a};

    public static ItemStack rare(ServerPlayer p, int tier, String child) {
        tier = Math.max(1, Math.min(6, tier));
        RandomSource r = p.getRandom();
        List<Item> pool = new ArrayList<>();
        for (String id : WEAPONS[tier]) {
            ResourceLocation rl = new ResourceLocation(id);
            if (ForgeRegistries.ITEMS.containsKey(rl)) pool.add(ForgeRegistries.ITEMS.getValue(rl));
        }
        Item it = pool.isEmpty() ? FALLBACK[tier] : pool.get(r.nextInt(pool.size()));
        ItemStack st = new ItemStack(it);
        for (Object[] e : ENCH[tier]) {
            Enchantment en = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft", (String) e[0]));
            if (en == null || !en.canEnchant(st)) continue;
            st.enchant(en, (Integer) e[1]);
        }
        int color = COLOR[tier];
        st.setHoverName(Component.translatable(it.getDescriptionId()).append(Component.literal(" của " + child)).withStyle(s -> s.withItalic(false).withColor(color)));
        CompoundTag disp = st.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        for (String line : new String[]{"Quà hiếm sau một ván trốn tìm.", "Đứa trẻ thứ tám giữ nó rất lâu.", "Bậc " + tier + " trên 6"}) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line).withStyle(s -> s.withItalic(true).withColor(0x8a8a8a)))));
        }
        disp.put("Lore", lore);
        st.getOrCreateTag().putInt("cavemind_gift", tier);
        return st;
    }

    public static ItemStack small(ServerPlayer p, int tier) {
        RandomSource r = p.getRandom();
        ItemStack[] pool = {
            new ItemStack(Items.GOLDEN_CARROT, 4 + tier),
            new ItemStack(Items.EXPERIENCE_BOTTLE, 3 + tier),
            new ItemStack(Items.CANDLE, 4),
            new ItemStack(Items.ENDER_PEARL, 2),
            new ItemStack(Items.NAME_TAG),
            new ItemStack(Items.ARROW, 16 + tier * 4),
            new ItemStack(tier >= 4 ? Items.DIAMOND : Items.GOLD_INGOT, tier >= 4 ? 2 : 3),
            new ItemStack(Items.GLOW_BERRIES, 8)
        };
        ItemStack st = pool[r.nextInt(pool.length)].copy();
        CompoundTag disp = st.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("Nó để lại sau ván trốn tìm.").withStyle(s -> s.withItalic(true).withColor(0x8a8a8a)))));
        disp.put("Lore", lore);
        return st;
    }

    public static void give(ServerPlayer p, ItemStack st) {
        Component name = st.getHoverName().copy();
        if (!p.getInventory().add(st)) p.drop(st, false);
        p.displayClientMessage(Component.literal("Nhận được: ").withStyle(ChatFormatting.GRAY).append(name), false);
    }

    private Gifts() {}
}
