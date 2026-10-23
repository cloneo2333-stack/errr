package vn.cavemind;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class Cfg {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.DoubleValue INTENSITY;
    public static final ForgeConfigSpec.BooleanValue MERCY;
    public static final ForgeConfigSpec.IntValue DEATH_CALM;
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_FIRE;
    public static final ForgeConfigSpec.BooleanValue GLIMPSES;
    public static final ForgeConfigSpec.BooleanValue LIFE;
    public static final ForgeConfigSpec.BooleanValue LIFE_INSIDE;
    public static final ForgeConfigSpec.BooleanValue MAN_SCHEMES;
    public static final ForgeConfigSpec.BooleanValue MAN_STEAL;
    public static final ForgeConfigSpec.BooleanValue MAN_WAKE;

    public static final ForgeConfigSpec.BooleanValue DIRECTOR;
    public static final ForgeConfigSpec.IntValue PEAK_MIN;
    public static final ForgeConfigSpec.IntValue PEAK_MAX;
    public static final ForgeConfigSpec.IntValue PEAK_HOLD_MIN;
    public static final ForgeConfigSpec.IntValue PEAK_HOLD_MAX;
    public static final ForgeConfigSpec.DoubleValue HINT_MIN;
    public static final ForgeConfigSpec.DoubleValue HINT_MAX;
    public static final ForgeConfigSpec.DoubleValue DRY_CHANCE;
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
    public static final ForgeConfigSpec.DoubleValue MAN_FREQ;
    public static final ForgeConfigSpec.BooleanValue MAN_DAYTIME;
    public static final ForgeConfigSpec.BooleanValue MAN_SHY;
    public static final ForgeConfigSpec.IntValue MAN_FIRST_MIN;
    public static final ForgeConfigSpec.DoubleValue MAN_REST_MIN;
    public static final ForgeConfigSpec.DoubleValue MAN_REST_MAX;
    public static final ForgeConfigSpec.BooleanValue MAN_HUNT;
    public static final ForgeConfigSpec.IntValue MAN_HUNT_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MAN_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue MAN_DOORS;
    public static final ForgeConfigSpec.BooleanValue MAN_BAR;
    public static final ForgeConfigSpec.IntValue MAN_BAR_MINUTES;

    public static final ForgeConfigSpec.BooleanValue DAMNED;
    public static final ForgeConfigSpec.BooleanValue DAYLIGHT;
    public static final ForgeConfigSpec.BooleanValue QUIET_FALLEN;
    public static final ForgeConfigSpec.IntValue DAMNED_START_DAY;
    public static final ForgeConfigSpec.DoubleValue DAMNED_NIGHT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue DAMNED_SHRINE;
    public static final ForgeConfigSpec.BooleanValue DAMNED_SLEEP;
    public static final ForgeConfigSpec.BooleanValue DAMNED_TRACES;
    public static final ForgeConfigSpec.BooleanValue MAZE;
    public static final ForgeConfigSpec.BooleanValue MAZE_CAM;

    public static final ForgeConfigSpec.BooleanValue SEEK;
    public static final ForgeConfigSpec.IntValue SEEK_DAY;
    public static final ForgeConfigSpec.DoubleValue SEEK_FREQ;

    public static final ForgeConfigSpec.BooleanValue HERO;
    public static final ForgeConfigSpec.BooleanValue HERO_REPLACE;
    public static final ForgeConfigSpec.DoubleValue HERO_FREQ;
    public static final ForgeConfigSpec.IntValue HERO_START_DAY;
    public static final ForgeConfigSpec.IntValue HERO_TIER1;
    public static final ForgeConfigSpec.IntValue HERO_TIER2;
    public static final ForgeConfigSpec.DoubleValue HERO_GAP_MIN;
    public static final ForgeConfigSpec.DoubleValue HERO_GAP_MAX;
    public static final ForgeConfigSpec.BooleanValue HERO_EDITS;
    public static final ForgeConfigSpec.ConfigValue<String> HERO_SKIN;
    public static final ForgeConfigSpec.BooleanValue HERO_FAKE_LEAVE;

    public static final ForgeConfigSpec.BooleanValue CHAPTERS;
    public static final ForgeConfigSpec.BooleanValue CHAPTER_SPAWNS;
    public static final ForgeConfigSpec.DoubleValue RARE_CHANCE;

    private static String sec;

    private static void push(ForgeConfigSpec.Builder b, String name, String comment) {
        sec = name;
        b.comment(comment).translation("cavemind.config." + name).push(name);
    }

    private static ForgeConfigSpec.Builder k(ForgeConfigSpec.Builder b, String key, String comment) {
        return b.comment(comment).translation("cavemind.config." + sec + "." + key);
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        push(b, "general", "Overall pace and anti-frustration");
        DEBUG = k(b, "debug", "Log every decision the minds make (latest.log, prefix [CaveMind])").define("debug", false);
        INTENSITY = k(b, "intensity", "How often things happen. 0.5 = half as often and a smaller daily scare budget, 2 = twice as often")
            .defineInRange("intensity", 1.0, 0.25, 3.0);
        MERCY = k(b, "mercy", "Anti-frustration: quiet first minutes after joining, nothing while you are in a menu, fighting, riding, AFK or in a raid, a calm stretch after you die, a daily scare budget")
            .define("mercy", true);
        DEATH_CALM = k(b, "death_calm_minutes", "Minutes of calm after a death (mercy)").defineInRange("death_calm_minutes", 0, 0, 20);
        FRIENDLY_FIRE = k(b, "pvp_counts", "Deaths and fights between players (and /kill) count for mercy. Off = friends killing each other does not pause the horror")
            .define("pvp_counts", false);
        GLIMPSES = k(b, "glimpses", "The creatures may let themselves be seen from afar when they choose to: a figure in the window, at its lair, sitting in your dark house")
            .define("glimpses", true);
        b.pop();

        push(b, "director", "Tension Director: each player has a 0-100 tension meter that goes build-up, peak, fade, release. It never places a creature, it only allows, forbids and hints");
        DIRECTOR = k(b, "enabled", "Pace the horror per player with the tension meter").define("enabled", true);
        PEAK_MIN = k(b, "peak_min", "Tension that starts a peak (lowest, picked at random each cycle)").defineInRange("peak_min", 55, 10, 100);
        PEAK_MAX = k(b, "peak_max", "Tension that starts a peak (highest)").defineInRange("peak_max", 85, 10, 100);
        PEAK_HOLD_MIN = k(b, "peak_hold_min", "Seconds a peak may last (min). Only during a peak may a creature attack or punish").defineInRange("peak_hold_min", 20, 5, 300);
        PEAK_HOLD_MAX = k(b, "peak_hold_max", "Seconds a peak may last (max)").defineInRange("peak_hold_max", 45, 5, 600);
        HINT_MIN = k(b, "hint_minutes_min", "Minutes of build-up with nothing noticing you before the Director hints an area to the creatures (min)").defineInRange("hint_minutes_min", 6.0, 0.5, 120.0);
        HINT_MAX = k(b, "hint_minutes_max", "Same, max").defineInRange("hint_minutes_max", 12.0, 0.5, 240.0);
        DRY_CHANCE = k(b, "dry_chance", "Chance a build-up gets no hint at all: only what the creatures find by themselves").defineInRange("dry_chance", 0.2, 0.0, 1.0);
        GATE = k(b, "headliner_gate", "Stop other mods' stalkers from piling on while a scene is running or during relief").define("headliner_gate", true);
        MAX_HEADLINERS = k(b, "max_headliners", "How many stalker-class entities may hunt the same player at once").defineInRange("max_headliners", 1, 1, 4);
        HEADLINERS = k(b, "headliners", "Entity ids (or modid:*) that count as stalker-class headliners")
            .defineList("headliners", List.of(
                "cave_dweller:cave_dweller",
                "the_knocker:knocker", "the_knocker:knockerstalk", "the_knocker:knockerstalklooked",
                "anomaly:anomaly_chase", "anomaly:anomaly_ghost", "anomaly:anomaly_diamond",
                "the_obsessed:idk_stalker_surprise", "the_obsessed:idk_stalker_saviour", "the_obsessed:idk_stalker_mimic",
                "the_obsessed:idk_stalker_unhappy", "the_obsessed:idk_stalker_opportunistic", "the_obsessed:idk_spider_jealous",
                "apollyon:the_fallen", "apollyon:the_infector"
            ), o -> o instanceof String);
        RELAX_MIN = k(b, "relax_min", "Minutes of release (true quiet) after a peak (min)").defineInRange("relax_min", 3, 0, 60);
        RELAX_MAX = k(b, "relax_max", "Minutes of release after a peak (max)").defineInRange("relax_max", 8, 0, 120);
        CAVE_DREAD = k(b, "cave_dread", "Cave dread events: digging behind the walls, echo steps, breathing in the dark").define("cave_dread", true);
        TORCH_THEFT = k(b, "torch_theft", "Torches the player placed far behind them can go missing").define("torch_theft", true);
        FOG = k(b, "fog", "Fog thickens when the Man is near (client)").define("fog", true);
        SILENCE = k(b, "silence", "Ambient sound and music cut out before something happens (client)").define("silence", true);
        b.pop();

        push(b, "man_from_the_fog", "The Man From The Fog");
        MAN = k(b, "enabled", "Replace the Man From The Fog's spawn/look/vanish logic with the Cave Mind stalker").define("enabled", true);
        MAN_FREQ = k(b, "frequency", "How fast he grows hungry and how short he rests, on top of intensity. 2 = twice as eager").defineInRange("frequency", 1.0, 0.25, 4.0);
        MAN_DAYTIME = k(b, "daytime", "He may also walk in under clear daylight when he is very hungry (underground he comes any time)").define("daytime", true);
        LIFE = k(b, "lives_in_world", "He lives in the world: a lair, a daily routine, hunger for attention, caution, curiosity. He studies your base night after night and leaves signs even while you sleep")
            .define("lives_in_world", true);
        LIFE_INSIDE = k(b, "enters_base", "Late in the study he gets inside when nobody is home (or while you sleep) and may be waiting in a dark corner when you come back").define("enters_base", true);
        MAN_SHY = k(b, "shy_of_groups", "He breaks off creeping tactics when another player is within 22 blocks").define("shy_of_groups", true);
        MAN_FIRST_MIN = k(b, "first_arrival_minutes", "Minutes a new player plays before the Man may walk in on them").defineInRange("first_arrival_minutes", 6, 0, 600);
        MAN_REST_MIN = k(b, "rest_min", "Minutes he walks away and rests after a visit (min). Then hunger, your scent and your noise bring him back").defineInRange("rest_min", 4.0, 0.5, 120.0);
        MAN_REST_MAX = k(b, "rest_max", "Minutes he rests after a visit (max)").defineInRange("rest_max", 10.0, 0.5, 240.0);
        MAN_HUNT = k(b, "hunt", "The Man may hunt (rare, telegraphed, escapable by hiding)").define("hunt", true);
        MAN_HUNT_LEVEL = k(b, "hunt_level", "Familiarity level (0-10) before he will hunt unprovoked").defineInRange("hunt_level", 5, 0, 10);
        MAN_DAMAGE = k(b, "hunt_damage", "Damage per hit while hunting").defineInRange("hunt_damage", 8.0, 1.0, 100.0);
        MAN_DOORS = k(b, "doors", "He may knock and open doors at night").define("doors", true);
        MAN_SCHEMES = k(b, "schemes", "He works on things you find later: a worn trail, his nest, a peephole, a sealed tunnel").define("schemes", true);
        MAN_STEAL = k(b, "steal", "He may rot part of your food stores, reset a line of crops and take one farm animal (never named, tamed or the last few)").define("steal", true);
        MAN_WAKE = k(b, "wake", "He may knock you awake (never two nights running, never while other players are asleep)").define("wake", true);
        MAN_BAR = k(b, "progress_bar", "Show his progress on the HUD and make every quarter of it end in something he does: traces at your home, a visit, a test in the dark, then a night hunt you cannot sleep through").define("progress_bar", true);
        MAN_BAR_MINUTES = k(b, "progress_minutes", "Minutes of awake play that fill his bar with no other help (seeing him, sleeping through nights and staying underground fill it faster)").defineInRange("progress_minutes", 36, 5, 600);
        b.pop();

        push(b, "the_damned", "The Damned");
        DAMNED = k(b, "enabled", "Replace The Damned with the non-lethal haunting").define("enabled", true);
        DAMNED_START_DAY = k(b, "start_day", "In-game day the haunting may begin").defineInRange("start_day", 1, 0, 1000);
        DAMNED_NIGHT_CHANCE = k(b, "night_chance", "Chance a night carries a haunting scene").defineInRange("night_chance", 0.7, 0.0, 1.0);
        DAMNED_SHRINE = k(b, "shrine", "It builds a shrine and writes about you").define("shrine", true);
        DAMNED_SLEEP = k(b, "sleep_visits", "It visits while you sleep").define("sleep_visits", true);
        DAMNED_TRACES = k(b, "traces", "Candles go out, frames turn, torches go missing at night").define("traces", true);
        MAZE = k(b, "maze", "The Forty Eighth Hour: when it catches you it drags you into its maze, and the playground well opens it if you ring the school bell with a lit candle on the lid. Your items are never lost there")
            .define("maze", true);
        MAZE_CAM = k(b, "camcorder", "Inside the maze you see through an old camcorder: night vision that eats batteries, grain, heartbeat and breathing, no minimap").define("camcorder", true);
        b.pop();

        push(b, "hide_and_seek", "The Damned's game: it grows lonely, finds you by itself, counts to forty eight at a tree and searches. Play by the rules and you are safe, ignore it and it punishes you");
        SEEK = k(b, "enabled", "The Damned invites you to play hide and seek (day or night)").define("enabled", true);
        SEEK_DAY = k(b, "start_day", "In-game day from which it may invite you").defineInRange("start_day", 1, 0, 100);
        SEEK_FREQ = k(b, "frequency", "How fast it grows lonely. 0.5 = half as often, 2 = twice as often").defineInRange("frequency", 1.0, 0.0, 5.0);
        b.pop();

        push(b, "herobrine", "Herobrine");
        HERO = k(b, "enabled", "Herobrine (From The Fog) as a rare, varied, player-like presence that remembers every move you make").define("enabled", true);
        HERO_REPLACE = k(b, "replace_from_the_fog", "Silence From The Fog's own scheduled events (sightings, burning base, torch breaking, nightmares, fake crash) so only Cave Mind drives him (needs world reload)")
            .define("replace_from_the_fog", true);
        HERO_FREQ = k(b, "frequency", "How fast the mine remembers you: memory grows with every block you dig or place and he echoes it back. 2 = twice as often").defineInRange("frequency", 1.0, 0.25, 4.0);
        HERO_START_DAY = k(b, "start_day", "In-game day he may first appear").defineInRange("start_day", 1, 0, 1000);
        HERO_TIER1 = k(b, "bigger_shows_day", "Days survived before the walker, the builder and the replay of your own path").defineInRange("bigger_shows_day", 3, 0, 100);
        HERO_TIER2 = k(b, "closest_shows_day", "Days survived before someone asleep in your bed and the figure at your shoulder").defineInRange("closest_shows_day", 6, 0, 100);
        HERO_GAP_MIN = k(b, "gap_days_min", "In-game days between his appearances (min)").defineInRange("gap_days_min", 1.0, 0.1, 30.0);
        HERO_GAP_MAX = k(b, "gap_days_max", "In-game days between his appearances (max)").defineInRange("gap_days_max", 2.5, 0.1, 60.0);
        HERO_EDITS = k(b, "world_edits", "He may leave things in the world: signs that know your habits, tunnels continued past where you stopped, a leafless tree, copies of what you built")
            .define("world_edits", true);
        HERO_SKIN = k(b, "skin", "Skin texture for Herobrine (any From The Fog skin under watching:textures/item/herobrine/...)")
            .define("skin", "watching:textures/item/herobrine/classic/classic.png");
        HERO_FAKE_LEAVE = k(b, "fake_leave", "Rarely, a line saying a far away friend left the game, seen only by you (after you found the abandoned mine)").define("fake_leave", false);
        b.pop();

        push(b, "daylight", "Daytime set-pieces");
        DAYLIGHT = k(b, "enabled", "Daytime set-pieces: the figure in the field, the slow walker, animal rituals, a house that was touched, echoes of yourself. Never lethal, nothing thrown at the screen")
            .define("enabled", true);
        QUIET_FALLEN = k(b, "quiet_fallen", "The Fallen sinks silently when looked at instead of flashing a full-screen jumpscare image (needs restart)")
            .define("quiet_fallen", true);
        b.pop();

        push(b, "chapters", "Quest chapters 2-6 open only after a hide and seek you start yourself, holding the lost child's toy for that chapter");
        CHAPTERS = k(b, "enabled", "Chapter gates: invitations, meeting points, toys, active hide and seek, rare gifts").define("enabled", true);
        CHAPTER_SPAWNS = k(b, "spawn_tiers", "New monster families start spawning only after the chapter that brings them is unlocked by a player nearby")
            .define("spawn_tiers", true);
        RARE_CHANCE = k(b, "rare_chance", "Chance a hide and seek you start yourself (outside a chapter gate) ends with a rare weapon instead of a small gift")
            .defineInRange("rare_chance", 0.35, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private Cfg() {}
}
