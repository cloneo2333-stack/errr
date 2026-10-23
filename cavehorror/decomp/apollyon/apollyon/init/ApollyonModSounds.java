package apollyon.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ApollyonModSounds {
   public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "apollyon");
   public static final RegistryObject<SoundEvent> SPAWN_NOISE = REGISTRY.register(
      "spawn_noise", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "spawn_noise"))
   );
   public static final RegistryObject<SoundEvent> STEP_SOUND = REGISTRY.register(
      "step_sound", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "step_sound"))
   );
   public static final RegistryObject<SoundEvent> DAMAGE_NOISE = REGISTRY.register(
      "damage_noise", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "damage_noise"))
   );
   public static final RegistryObject<SoundEvent> DAMNED_DEATH = REGISTRY.register(
      "damned_death", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "damned_death"))
   );
   public static final RegistryObject<SoundEvent> PETRIFIED_BEGIN = REGISTRY.register(
      "petrified_begin", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "petrified_begin"))
   );
   public static final RegistryObject<SoundEvent> LIVING_SOUND = REGISTRY.register(
      "living_sound", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "living_sound"))
   );
   public static final RegistryObject<SoundEvent> JUMPSCARE_SOUND = REGISTRY.register(
      "jumpscare_sound", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "jumpscare_sound"))
   );
   public static final RegistryObject<SoundEvent> HALLUCINATION_SOUND = REGISTRY.register(
      "hallucination_sound", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "hallucination_sound"))
   );
   public static final RegistryObject<SoundEvent> PLAYER_DEATH = REGISTRY.register(
      "player_death", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "player_death"))
   );
   public static final RegistryObject<SoundEvent> QUIETER_SPAWN = REGISTRY.register(
      "quieter_spawn", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "quieter_spawn"))
   );
   public static final RegistryObject<SoundEvent> SHALLOW_BREATHING = REGISTRY.register(
      "shallow_breathing", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "shallow_breathing"))
   );
   public static final RegistryObject<SoundEvent> VILLAGER_DIALOGUE = REGISTRY.register(
      "villager_dialogue", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "villager_dialogue"))
   );
   public static final RegistryObject<SoundEvent> INFECTOR_HURT = REGISTRY.register(
      "infector_hurt", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "infector_hurt"))
   );
   public static final RegistryObject<SoundEvent> GUTTERAL_ROAR = REGISTRY.register(
      "gutteral_roar", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "gutteral_roar"))
   );
   public static final RegistryObject<SoundEvent> DEEP_LAUGH = REGISTRY.register(
      "deep_laugh", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "deep_laugh"))
   );
   public static final RegistryObject<SoundEvent> LIGHT_SUSPENSE = REGISTRY.register(
      "light_suspense", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "light_suspense"))
   );
   public static final RegistryObject<SoundEvent> GATEWAY_PURSUIT = REGISTRY.register(
      "gateway_pursuit", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "gateway_pursuit"))
   );
   public static final RegistryObject<SoundEvent> INTENSE_RISE = REGISTRY.register(
      "intense_rise", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "intense_rise"))
   );
   public static final RegistryObject<SoundEvent> CUTSCENE_RISE = REGISTRY.register(
      "cutscene_rise", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "cutscene_rise"))
   );
   public static final RegistryObject<SoundEvent> CHASE_MUSIC = REGISTRY.register(
      "chase_music", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "chase_music"))
   );
   public static final RegistryObject<SoundEvent> FALLEN_JUMPSCARE = REGISTRY.register(
      "fallen_jumpscare", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "fallen_jumpscare"))
   );
   public static final RegistryObject<SoundEvent> DAMNED_SNEAK = REGISTRY.register(
      "damned_sneak", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "damned_sneak"))
   );
   public static final RegistryObject<SoundEvent> SMALL_JUMPSCARE = REGISTRY.register(
      "small_jumpscare", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "small_jumpscare"))
   );
   public static final RegistryObject<SoundEvent> BREAK_ROSE = REGISTRY.register(
      "break_rose", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "break_rose"))
   );
   public static final RegistryObject<SoundEvent> PLACE_ROSE = REGISTRY.register(
      "place_rose", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "place_rose"))
   );
   public static final RegistryObject<SoundEvent> USED_ROSE = REGISTRY.register(
      "used_rose", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "used_rose"))
   );
   public static final RegistryObject<SoundEvent> STEW_CONSUMED = REGISTRY.register(
      "stew_consumed", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "stew_consumed"))
   );
   public static final RegistryObject<SoundEvent> DAMNED_FLEE = REGISTRY.register(
      "damned_flee", () -> SoundEvent.m_262824_(new ResourceLocation("apollyon", "damned_flee"))
   );
}
