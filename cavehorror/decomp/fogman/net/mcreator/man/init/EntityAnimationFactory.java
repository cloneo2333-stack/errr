package net.mcreator.man.init;

import net.mcreator.man.entity.ManaggresiveEntity;
import net.mcreator.man.entity.ManfromthefogEntity;
import net.mcreator.man.entity.ManfromthefogbackEntity;
import net.mcreator.man.entity.MftfhangEntity;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class EntityAnimationFactory {
   @SubscribeEvent
   public static void onEntityTick(LivingTickEvent event) {
      if (event != null && event.getEntity() != null) {
         if (event.getEntity() instanceof ManfromthefogEntity syncable) {
            String animation = syncable.getSyncedAnimation();
            if (!animation.equals("undefined")) {
               syncable.setAnimation("undefined");
               syncable.animationprocedure = animation;
            }
         }

         if (event.getEntity() instanceof ManaggresiveEntity syncablex) {
            String animation = syncablex.getSyncedAnimation();
            if (!animation.equals("undefined")) {
               syncablex.setAnimation("undefined");
               syncablex.animationprocedure = animation;
            }
         }

         if (event.getEntity() instanceof ManfromthefogbackEntity syncablexx) {
            String animation = syncablexx.getSyncedAnimation();
            if (!animation.equals("undefined")) {
               syncablexx.setAnimation("undefined");
               syncablexx.animationprocedure = animation;
            }
         }

         if (event.getEntity() instanceof MftfhangEntity syncablexxx) {
            String animation = syncablexxx.getSyncedAnimation();
            if (!animation.equals("undefined")) {
               syncablexxx.setAnimation("undefined");
               syncablexxx.animationprocedure = animation;
            }
         }
      }
   }
}
