package apollyon.init;

import apollyon.entity.TheDamnedEntity;
import apollyon.entity.TheFallenEntity;
import apollyon.entity.TheInfectorEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@EventBusSubscriber(
   bus = Bus.MOD
)
public class ApollyonModEntities {
   public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "apollyon");
   public static final RegistryObject<EntityType<TheDamnedEntity>> THE_DAMNED = register(
      "the_damned",
      Builder.m_20704_(TheDamnedEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .setCustomClientFactory(TheDamnedEntity::new)
         .m_20719_()
         .m_20699_(0.6F, 1.8F)
   );
   public static final RegistryObject<EntityType<TheInfectorEntity>> THE_INFECTOR = register(
      "the_infector",
      Builder.m_20704_(TheInfectorEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .setCustomClientFactory(TheInfectorEntity::new)
         .m_20719_()
         .m_20699_(0.6F, 1.8F)
   );
   public static final RegistryObject<EntityType<TheFallenEntity>> THE_FALLEN = register(
      "the_fallen",
      Builder.m_20704_(TheFallenEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .setCustomClientFactory(TheFallenEntity::new)
         .m_20719_()
         .m_20699_(0.6F, 4.7F)
   );

   private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryname, Builder<T> entityTypeBuilder) {
      return REGISTRY.register(registryname, () -> entityTypeBuilder.m_20712_(registryname));
   }

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
      event.enqueueWork(() -> {
         TheDamnedEntity.init();
         TheInfectorEntity.init();
         TheFallenEntity.init();
      });
   }

   @SubscribeEvent
   public static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put((EntityType)THE_DAMNED.get(), TheDamnedEntity.createAttributes().m_22265_());
      event.put((EntityType)THE_INFECTOR.get(), TheInfectorEntity.createAttributes().m_22265_());
      event.put((EntityType)THE_FALLEN.get(), TheFallenEntity.createAttributes().m_22265_());
   }
}
