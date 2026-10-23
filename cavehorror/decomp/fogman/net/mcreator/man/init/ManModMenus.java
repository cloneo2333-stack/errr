package net.mcreator.man.init;

import net.mcreator.man.world.inventory.CassetteRecorderGuiMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ManModMenus {
   public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "man");
   public static final RegistryObject<MenuType<CassetteRecorderGuiMenu>> CASSETTE_RECORDER_GUI = REGISTRY.register(
      "cassette_recorder_gui", () -> IForgeMenuType.create(CassetteRecorderGuiMenu::new)
   );
}
