package vn.cavemind.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import vn.cavemind.CaveMind;

// Sounds synthesized by tools/gen_sounds.py.
public final class Sounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CaveMind.MODID);

    public static final String HUM = "cavemind:maze.hum", FOCUS = "cavemind:maze.focus", SOB = "cavemind:maze.sob", COUGH = "cavemind:maze.cough",
        SCRAPE = "cavemind:maze.scrape", TAPE = "cavemind:maze.tape", TOP = "cavemind:maze.top", HARMONICA = "cavemind:maze.harmonica", TWANG = "cavemind:maze.twang";

    static {
        for (String id : new String[]{HUM, FOCUS, SOB, COUGH, SCRAPE, TAPE, TOP, HARMONICA, TWANG}) {
            String path = id.substring(id.indexOf(':') + 1);
            SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CaveMind.MODID, path)));
        }
    }

    private Sounds() {}
}
