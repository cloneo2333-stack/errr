package net.mcreator.man.configuration;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class ManconfigConfiguration {
   public static final Builder BUILDER = new Builder();
   public static final ForgeConfigSpec SPEC = BUILDER.build();
   public static final ConfigValue<Boolean> ENABLE_SPAWNING = BUILDER.comment("enable spawning of the man").define("enable_spawning", true);
   public static final ConfigValue<Double> MIN_SPAWN_RATE = BUILDER.comment("minimum duration of spawning in ticks").define("min_spawn_rate", 5000.0);
   public static final ConfigValue<Double> MAX_SPAWN_RATE = BUILDER.comment("maximum duration of spawning in ticks").define("max_spawn_rate", 20000.0);
   public static final ConfigValue<Boolean> JUMPSCARE = BUILDER.comment("if true, it shows a jumpscare image when killed").define("jumpscare", true);
   public static final ConfigValue<Double> MIN_CHASE_DURATION = BUILDER.comment("minimum duration of getting chased in ticks")
      .define("min_chase_duration", 400.0);
   public static final ConfigValue<Double> MAX_CHASE_DURATION = BUILDER.comment("maximum duration of getting chased in ticks")
      .define("max_chase_duration", 1000.0);
   public static final ConfigValue<Boolean> CLIMBING = BUILDER.comment("if true, it can climb vertical blocks").define("climbing", true);
   public static final ConfigValue<Double> VANISH_DISTANCE = BUILDER.comment("minimum radius in blocks before it despawns").define("vanish_distance", 30.0);
   public static final ConfigValue<Boolean> SPAWN_AT_DAY = BUILDER.comment("if true, it can also spawn during the day").define("spawn_at_day", true);
   public static final ConfigValue<Boolean> DARKNESS_EFFECT = BUILDER.comment("if true, the player gets the darkness effect when starts getting chased")
      .define("darkness_effect", true);
   public static final ConfigValue<Double> DARKNESS_EFFECT_DURATION = BUILDER.comment("the duration in ticks of the darkness effect")
      .define("darkness_effect_duration", 150.0);
   public static final ConfigValue<Boolean> BREAK_BLOCKS = BUILDER.comment("if true, it can break doors and glass/glass panes while it chases the player")
      .define("break_blocks", true);
   public static final ConfigValue<Boolean> LIGHTNINGS = BUILDER.comment("if true, it will summon fake lightnings when despawning or spawning")
      .define("lightnings", true);
   public static final ConfigValue<Boolean> SPAWN_SOUNDS = BUILDER.comment("if true, it will play one of five spawn sounds when spawning or getting closer")
      .define("spawn_sounds", true);
   public static final ConfigValue<Double> STAGE1_SPAWN_RATE = BUILDER.comment("the duration in ticks until it will get closer when not looked at")
      .define("stage1_spawn_rate", 600.0);
   public static final ConfigValue<Double> STAGE2_SPAWN_RATE = BUILDER.comment(
         "the duration in ticks until it will start chasing the player when not looked at"
      )
      .define("stage2_spawn_rate", 900.0);
   public static final ConfigValue<Boolean> OLD_STALK_MECHANIC = BUILDER.comment("if true, the man will use the old chase mechanic")
      .define("old_stalk_mechanic", false);
   public static final ConfigValue<Boolean> LEGACY_MAN = BUILDER.comment(
         "the structures as well as the dimension will be disabled and the man spawns normally in your world. Create a new world when disabled"
      )
      .define("legacy_man", false);
   public static final ConfigValue<Boolean> SEARCH_MECHANIC = BUILDER.comment(
         "the man will search the nearest player on lost sight and when close enough, destroy neccessary blocks to get the player"
      )
      .define("search_mechanic", true);
   public static final ConfigValue<Double> BLOCK_HARDNESS = BUILDER.comment(
         "The max hardness of a block which the man will be able to break, set to -1.0 to disable"
      )
      .define("block_hardness", 2.0);
   public static final ConfigValue<Double> BLOCK_HARDNESS_SEARCH = BUILDER.comment(
         "The max hardness of a block which the man will be able to break when searching for the player"
      )
      .define("block_hardness_search", 5.0);
   public static final ConfigValue<Double> BLOCK_BREAK_TIMER = BUILDER.comment(
         "The time in ticks for how long the man needs to break a block (higher value = lower speed)"
      )
      .define("block_break_timer", 10.0);

   static {
      BUILDER.push("Server and Client");
      BUILDER.pop();
      BUILDER.push("Client only");
      BUILDER.pop();
   }
}
