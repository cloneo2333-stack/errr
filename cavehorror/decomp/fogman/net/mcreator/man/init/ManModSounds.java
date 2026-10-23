package net.mcreator.man.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ManModSounds {
   public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "man");
   public static final RegistryObject<SoundEvent> SPAWN_1 = REGISTRY.register("spawn_1", () -> SoundEvent.m_262824_(new ResourceLocation("man", "spawn_1")));
   public static final RegistryObject<SoundEvent> SPAWN_2 = REGISTRY.register("spawn_2", () -> SoundEvent.m_262824_(new ResourceLocation("man", "spawn_2")));
   public static final RegistryObject<SoundEvent> SPAWN_3 = REGISTRY.register("spawn_3", () -> SoundEvent.m_262824_(new ResourceLocation("man", "spawn_3")));
   public static final RegistryObject<SoundEvent> SCREAM_LOOP = REGISTRY.register(
      "scream_loop", () -> SoundEvent.m_262824_(new ResourceLocation("man", "scream_loop"))
   );
   public static final RegistryObject<SoundEvent> VANISH = REGISTRY.register("vanish", () -> SoundEvent.m_262824_(new ResourceLocation("man", "vanish")));
   public static final RegistryObject<SoundEvent> LOOK = REGISTRY.register("look", () -> SoundEvent.m_262824_(new ResourceLocation("man", "look")));
   public static final RegistryObject<SoundEvent> JUMPSCARE = REGISTRY.register(
      "jumpscare", () -> SoundEvent.m_262824_(new ResourceLocation("man", "jumpscare"))
   );
   public static final RegistryObject<SoundEvent> LOOP = REGISTRY.register("loop", () -> SoundEvent.m_262824_(new ResourceLocation("man", "loop")));
   public static final RegistryObject<SoundEvent> SPAWN_4 = REGISTRY.register("spawn_4", () -> SoundEvent.m_262824_(new ResourceLocation("man", "spawn_4")));
   public static final RegistryObject<SoundEvent> SPAWN_5 = REGISTRY.register("spawn_5", () -> SoundEvent.m_262824_(new ResourceLocation("man", "spawn_5")));
   public static final RegistryObject<SoundEvent> FOREST_AMBIENT = REGISTRY.register(
      "forest_ambient", () -> SoundEvent.m_262824_(new ResourceLocation("man", "forest_ambient"))
   );
   public static final RegistryObject<SoundEvent> WHITE_NOISE_PASSBY = REGISTRY.register(
      "white_noise_passby", () -> SoundEvent.m_262824_(new ResourceLocation("man", "white_noise_passby"))
   );
   public static final RegistryObject<SoundEvent> LONG_RISER = REGISTRY.register(
      "long_riser", () -> SoundEvent.m_262824_(new ResourceLocation("man", "long_riser"))
   );
   public static final RegistryObject<SoundEvent> RITUAL_ENTER = REGISTRY.register(
      "ritual_enter", () -> SoundEvent.m_262824_(new ResourceLocation("man", "ritual_enter"))
   );
   public static final RegistryObject<SoundEvent> LONG_WHISPER = REGISTRY.register(
      "long_whisper", () -> SoundEvent.m_262824_(new ResourceLocation("man", "long_whisper"))
   );
   public static final RegistryObject<SoundEvent> SHORT_WHISPER_1 = REGISTRY.register(
      "short_whisper_1", () -> SoundEvent.m_262824_(new ResourceLocation("man", "short_whisper_1"))
   );
   public static final RegistryObject<SoundEvent> SHORT_WHISPER_2 = REGISTRY.register(
      "short_whisper_2", () -> SoundEvent.m_262824_(new ResourceLocation("man", "short_whisper_2"))
   );
   public static final RegistryObject<SoundEvent> SAFETY_INSTRUCTIONS = REGISTRY.register(
      "safety_instructions", () -> SoundEvent.m_262824_(new ResourceLocation("man", "safety_instructions"))
   );
   public static final RegistryObject<SoundEvent> RITUAL = REGISTRY.register("ritual", () -> SoundEvent.m_262824_(new ResourceLocation("man", "ritual")));
}
