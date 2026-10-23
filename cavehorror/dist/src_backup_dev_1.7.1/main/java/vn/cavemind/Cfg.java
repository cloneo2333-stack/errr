package vn.cavemind;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class Cfg {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.BooleanValue EYE;
    public static final ForgeConfigSpec.BooleanValue DEV;
    public static final ForgeConfigSpec.DoubleValue INTENSITY;
    public static final ForgeConfigSpec.BooleanValue MERCY;
    public static final ForgeConfigSpec.BooleanValue MAN_SCHEMES;
    public static final ForgeConfigSpec.BooleanValue MAN_STEAL;
    public static final ForgeConfigSpec.BooleanValue MAN_WAKE;

    public static final ForgeConfigSpec.BooleanValue DIRECTOR;
    public static final ForgeConfigSpec.BooleanValue GATE;
    public static final ForgeConfigSpec.IntValue MAX_HEADLINERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEADLINERS;
    public static final ForgeConfigSpec.IntValue RELAX_MIN;
    public static final ForgeConfigSpec.IntValue RELAX_MAX;
    public static final ForgeConfigSpec.BooleanValue CAVE_DREAD;
    public static final ForgeConfigSpec.BooleanValue TORCH_THEFT;
    public static final ForgeConfigSpec.BooleanValue FOG;
    public static final ForgeConfigSpec.BooleanValue SILENCE;

    public static final ForgeConfigSpec.BooleanValue MAN;
    public static final ForgeConfigSpec.IntValue MAN_FIRST_MIN;
    public static final ForgeConfigSpec.IntValue MAN_GAP_MIN;
    public static final ForgeConfigSpec.IntValue MAN_GAP_MAX;
    public static final ForgeConfigSpec.BooleanValue MAN_HUNT;
    public static final ForgeConfigSpec.IntValue MAN_HUNT_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MAN_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue MAN_DOORS;

    public static final ForgeConfigSpec.BooleanValue DAMNED;
    public static final ForgeConfigSpec.BooleanValue DAYLIGHT;
    public static final ForgeConfigSpec.BooleanValue QUIET_FALLEN;
    public static final ForgeConfigSpec.IntValue DAMNED_START_DAY;
    public static final ForgeConfigSpec.DoubleValue DAMNED_NIGHT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue DAMNED_SHRINE;
    public static final ForgeConfigSpec.BooleanValue DAMNED_SLEEP;
    public static final ForgeConfigSpec.BooleanValue DAMNED_TRACES;

    public static final ForgeConfigSpec.BooleanValue HERO;
    public static final ForgeConfigSpec.BooleanValue HERO_REPLACE;
    public static final ForgeConfigSpec.IntValue HERO_START_DAY;
    public static final ForgeConfigSpec.DoubleValue HERO_GAP_MIN;
    public static final ForgeConfigSpec.DoubleValue HERO_GAP_MAX;
    public static final ForgeConfigSpec.BooleanValue HERO_EDITS;
    public static final ForgeConfigSpec.ConfigValue<String> HERO_SKIN;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        DEBUG = b.comment("Log every decision the minds make (latest.log, prefix [CaveMind])").define("debug", false);
        EYE = b.comment("Record everything the minds do (positions, poses, what you could see, decisions, screenshots) to cavemind_eye/ for tools/eye.py. Always on in dev mode")
            .define("eye", false);
        DEV = b.comment("Developer mode: announces every decision in chat, status bar, glowing outlines, fast timers, hunts deal no damage, creative counts as playing")
            .define("dev_mode", false);
        INTENSITY = b.comment("How often things happen. 0.5 = half as often and a smaller daily scare budget, 2 = twice as often").defineInRange("intensity", 1.0, 0.25, 3.0);
        MERCY = b.comment("Anti-frustration: quiet first minutes after joining, nothing while you are in a menu, fighting, riding, AFK or in a raid, a calm stretch after you die, a daily scare budget")
            .define("mercy", true);
        b.pop();

        b.push("director");
        DIRECTOR = b.comment("Pace the horror per player: build-up, peak, relief").define("enabled", true);
        GATE = b.comment("Stop other mods' stalkers from piling on while a scene is running or during relief").define("headliner_gate", true);
        MAX_HEADLINERS = b.comment("How many stalker-class entities may hunt the same player at once").defineInRange("max_headliners", 1, 1, 4);
        HEADLINERS = b.comment("Entity ids (or modid:*) that count as stalker-class headliners")
            .defineList("headliners", List.of(
                "cave_dweller:cave_dweller",
                "the_knocker:knocker", "the_knocker:knockerstalk", "the_knocker:knockerstalklooked",
                "anomaly:anomaly_chase", "anomaly:anomaly_ghost", "anomaly:anomaly_diamond",
                "the_obsessed:idk_stalker_surprise", "the_obsessed:idk_stalker_saviour", "the_obsessed:idk_stalker_mimic",
                "the_obsessed:idk_stalker_unhappy", "the_obsessed:idk_stalker_opportunistic", "the_obsessed:idk_spider_jealous",
                "apollyon:the_fallen", "apollyon:the_infector"
            ), o -> o instanceof String);
        RELAX_MIN = b.comment("Minutes of relief after a peak (min)").defineInRange("relax_min", 3, 0, 60);
        RELAX_MAX = b.comment("Minutes of relief after a peak (max)").defineInRange("relax_max", 7, 0, 120);
        CAVE_DREAD = b.comment("Cave dread events: digging behind the walls, echo steps, breathing in the dark").define("cave_dread", true);
        TORCH_THEFT = b.comment("Torches the player placed far behind them can go missing").define("torch_theft", true);
        FOG = b.comment("Fog thickens when the Man is near (client)").define("fog", true);
        SILENCE = b.comment("Ambient sound and music cut out before something happens (client)").define("silence", true);
        b.pop();

        b.push("man_from_the_fog");
        MAN = b.comment("Replace the Man From The Fog's spawn/look/vanish logic with the Cave Mind stalker").define("enabled", true);
        MAN_FIRST_MIN = b.comment("Minutes of play before the Man first arrives").defineInRange("first_arrival_minutes", 9, 0, 600);
        MAN_GAP_MIN = b.comment("Minutes between visits (min)").defineInRange("visit_gap_min", 10, 1, 600);
        MAN_GAP_MAX = b.comment("Minutes between visits (max)").defineInRange("visit_gap_max", 26, 1, 900);
        MAN_HUNT = b.comment("The Man may hunt (rare, telegraphed, escapable by hiding)").define("hunt", true);
        MAN_HUNT_LEVEL = b.comment("Familiarity level (0-10) before he will hunt unprovoked").defineInRange("hunt_level", 5, 0, 10);
        MAN_DAMAGE = b.comment("Damage per hit while hunting").defineInRange("hunt_damage", 8.0, 1.0, 100.0);
        MAN_DOORS = b.comment("He may knock and open doors at night").define("doors", true);
        MAN_SCHEMES = b.comment("He works on things you find later: a worn trail, his nest, a peephole, a sealed tunnel").define("schemes", true);
        MAN_STEAL = b.comment("He may rot part of your food stores, reset a line of crops and take one farm animal (never named, tamed or the last few)").define("steal", true);
        MAN_WAKE = b.comment("He may knock you awake (never two nights running, never while other players are asleep)").define("wake", true);
        b.pop();

        b.push("the_damned");
        DAMNED = b.comment("Replace The Damned with the non-lethal haunting").define("enabled", true);
        DAMNED_START_DAY = b.comment("In-game day the haunting may begin").defineInRange("start_day", 2, 0, 1000);
        DAMNED_NIGHT_CHANCE = b.comment("Chance a night carries a haunting scene").defineInRange("night_chance", 0.7, 0.0, 1.0);
        DAMNED_SHRINE = b.comment("It builds a shrine and writes about you").define("shrine", true);
        DAMNED_SLEEP = b.comment("It visits while you sleep").define("sleep_visits", true);
        DAMNED_TRACES = b.comment("Candles go out, frames turn, torches go missing at night").define("traces", true);
        b.pop();

        b.push("herobrine");
        HERO = b.comment("Herobrine (From The Fog) as a rare, varied, player-like presence that remembers every move you make").define("enabled", true);
        HERO_REPLACE = b.comment("Silence From The Fog's own scheduled events (sightings, burning base, torch breaking, nightmares, fake crash) so only Cave Mind drives him (needs world reload)")
            .define("replace_from_the_fog", true);
        HERO_START_DAY = b.comment("In-game day he may first appear").defineInRange("start_day", 3, 0, 1000);
        HERO_GAP_MIN = b.comment("In-game days between his appearances (min)").defineInRange("gap_days_min", 1.0, 0.1, 30.0);
        HERO_GAP_MAX = b.comment("In-game days between his appearances (max)").defineInRange("gap_days_max", 2.5, 0.1, 60.0);
        HERO_EDITS = b.comment("He may leave things in the world: signs that know your habits, tunnels continued past where you stopped, a leafless tree, copies of what you built")
            .define("world_edits", true);
        HERO_SKIN = b.comment("Skin texture for Herobrine (any From The Fog skin under watching:textures/item/herobrine/...)")
            .define("skin", "watching:textures/item/herobrine/classic/classic.png");
        b.pop();

        b.push("daylight");
        DAYLIGHT = b.comment("Daytime set-pieces: the figure in the field, the slow walker, animal rituals, a house that was touched, echoes of yourself. Never lethal, nothing thrown at the screen")
            .define("enabled", true);
        QUIET_FALLEN = b.comment("The Fallen sinks silently when looked at instead of flashing a full-screen jumpscare image (needs restart)")
            .define("quiet_fallen", true);
        b.pop();
        SPEC = b.build();
    }

    private Cfg() {}
}
