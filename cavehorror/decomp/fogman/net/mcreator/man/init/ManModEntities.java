package net.mcreator.man.init;

import net.mcreator.man.entity.ManaggresiveEntity;
import net.mcreator.man.entity.ManfromthefogEntity;
import net.mcreator.man.entity.ManfromthefogbackEntity;
import net.mcreator.man.entity.MftfhangEntity;
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
public class ManModEntities {
   public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "man");
   public static final RegistryObject<EntityType<ManfromthefogEntity>> MANFROMTHEFOG = register(
      "manfromthefog",
      Builder.m_20704_(ManfromthefogEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .setCustomClientFactory(ManfromthefogEntity::new)
         .m_20699_(0.6F, 3.0F)
   );
   public static final RegistryObject<EntityType<ManaggresiveEntity>> MANAGGRESIVE = register(
      "managgresive",
      Builder.m_20704_(ManaggresiveEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(200)
         .setUpdateInterval(3)
         .setCustomClientFactory(ManaggresiveEntity::new)
         .m_20719_()
         .m_20699_(0.5F, 0.9F)
   );
   public static final RegistryObject<EntityType<ManfromthefogbackEntity>> MANFROMTHEFOGBACK = register(
      "manfromthefogback",
      Builder.m_20704_(ManfromthefogbackEntity::new, MobCategory.MONSTER)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .setCustomClientFactory(ManfromthefogbackEntity::new)
         .m_20699_(0.6F, 3.0F)
   );
   public static final RegistryObject<EntityType<MftfhangEntity>> MFTFHANG = register(
      "mftfhang",
      Builder.m_20704_(MftfhangEntity::new, MobCategory.AMBIENT)
         .setShouldReceiveVelocityUpdates(true)
         .setTrackingRange(64)
         .setUpdateInterval(3)
         .setCustomClientFactory(MftfhangEntity::new)
         .m_20699_(0.6F, 1.0F)
   );

   private static <T extends Entity> RegistryObject<EntityType<T>> register(String registryname, Builder<T> entityTypeBuilder) {
      return REGISTRY.register(registryname, () -> entityTypeBuilder.m_20712_(registryname));
   }

   @SubscribeEvent
   public static void init(FMLCommonSetupEvent event) {
      event.enqueueWork(() -> {
         ManfromthefogEntity.init();
         ManaggresiveEntity.init();
         ManfromthefogbackEntity.init();
         MftfhangEntity.init();
      });
   }

   @SubscribeEvent
   public static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put((EntityType)MANFROMTHEFOG.get(), ManfromthefogEntity.createAttributes().m_22265_());
      event.put((EntityType)MANAGGRESIVE.get(), ManaggresiveEntity.createAttributes().m_22265_());
      event.put((EntityType)MANFROMTHEFOGBACK.get(), ManfromthefogbackEntity.createAttributes().m_22265_());
      event.put((EntityType)MFTFHANG.get(), MftfhangEntity.createAttributes().m_22265_());
   }
}
