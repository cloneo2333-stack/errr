package net.mcreator.man.entity.model;

import net.mcreator.man.entity.ManaggresiveEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ManaggresiveModel extends GeoModel<ManaggresiveEntity> {
   public ResourceLocation getAnimationResource(ManaggresiveEntity entity) {
      return new ResourceLocation("man", "animations/the_man_from_the_fog.animation.json");
   }

   public ResourceLocation getModelResource(ManaggresiveEntity entity) {
      return new ResourceLocation("man", "geo/the_man_from_the_fog.geo.json");
   }

   public ResourceLocation getTextureResource(ManaggresiveEntity entity) {
      return new ResourceLocation("man", "textures/entities/" + entity.getTexture() + ".png");
   }
}
