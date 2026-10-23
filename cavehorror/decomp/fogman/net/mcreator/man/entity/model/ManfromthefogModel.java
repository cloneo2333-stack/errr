package net.mcreator.man.entity.model;

import net.mcreator.man.entity.ManfromthefogEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ManfromthefogModel extends GeoModel<ManfromthefogEntity> {
   public ResourceLocation getAnimationResource(ManfromthefogEntity entity) {
      return new ResourceLocation("man", "animations/the_man_from_the_fog.animation.json");
   }

   public ResourceLocation getModelResource(ManfromthefogEntity entity) {
      return new ResourceLocation("man", "geo/the_man_from_the_fog.geo.json");
   }

   public ResourceLocation getTextureResource(ManfromthefogEntity entity) {
      return new ResourceLocation("man", "textures/entities/" + entity.getTexture() + ".png");
   }
}
