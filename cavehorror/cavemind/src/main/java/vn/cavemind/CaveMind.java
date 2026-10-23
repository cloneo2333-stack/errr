package vn.cavemind;

import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import vn.cavemind.compat.Compat;
import vn.cavemind.net.Net;

@Mod(CaveMind.MODID)
public class CaveMind {
    public static final String MODID = "cavemind";
    public static final Logger LOG = LogUtils.getLogger();

    public CaveMind() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Cfg.SPEC);
        modBus.addListener(this::setup);
        modBus.addListener(this::packs);
        modBus.addListener(this::reloaded);
        modBus.addListener(this::tabs);
        vn.cavemind.gear.Gear.init();
        vn.cavemind.chapter.Keys.ITEMS.register(modBus);
        vn.cavemind.gear.Gear.BLOCKS.register(modBus);
        MinecraftForge.EVENT_BUS.register(Events.class);
        if (Boolean.getBoolean("cavemind.selftest")) MinecraftForge.EVENT_BUS.register(vn.cavemind.cmd.SelfTest.class);
    }

    private void packs(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            if (!ModList.get().isLoaded("watching") || !Cfg.HERO.get() || !Cfg.HERO_REPLACE.get()) return;
            Path data = ModList.get().getModFileById(MODID).getFile().findResource("ftf_override");
            Pack dp = Pack.readMetaAndCreate("cavemind_ftf", Component.literal("Cave Mind: Herobrine"), true,
                id -> new PathPackResources(id, data, true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
            if (dp != null) event.addRepositorySource(c -> c.accept(dp));
            return;
        }
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        Path root = ModList.get().getModFileById(MODID).getFile().findResource("overrides");
        Pack pack = Pack.readMetaAndCreate("cavemind_overrides", Component.literal("Cave Mind"), true,
            id -> new PathPackResources(id, root, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack != null) event.addRepositorySource(c -> c.accept(pack));
    }

    private void reloaded(net.minecraftforge.fml.event.config.ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != Cfg.SPEC) return;
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.execute(() -> Events.retune(server));
    }

    private void tabs(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != net.minecraft.world.item.CreativeModeTabs.INGREDIENTS) return;
        for (var item : vn.cavemind.chapter.Keys.ITEMS.getEntries()) event.accept(item);
    }

    private void setup(FMLCommonSetupEvent event) {
        Net.init();
        event.enqueueWork(Compat::takeOver);
    }

    public static final java.util.ArrayDeque<String> RECENT = new java.util.ArrayDeque<>();

    public static void debug(String msg, Object... args) {
        String line = org.slf4j.helpers.MessageFormatter.arrayFormat(msg, args).getMessage();
        synchronized (RECENT) {
            RECENT.addLast(java.time.LocalTime.now().withNano(0) + " " + line);
            while (RECENT.size() > 80) RECENT.removeFirst();
        }
        if (Cfg.DEBUG.get()) LOG.info("[CaveMind] " + line);
    }
}
