package apollyon.procedures;

import apollyon.ApollyonMod;
import apollyon.network.ApollyonModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

public class TheFallenOverlaysProcedure {
   public static void execute(LevelAccessor world, Entity entity) {
      if (entity != null) {
         if (((ApollyonModVariables.PlayerVariables)entity.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null)
               .orElse(new ApollyonModVariables.PlayerVariables()))
            .fallen_overlay
            .equals("false")) {
            String _setval = "true";
            entity.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
               capability.fallen_overlay = _setval;
               capability.syncPlayerVariables(entity);
            });
            if (world instanceof Level _level) {
               if (!_level.m_5776_()) {
                  _level.m_5594_(
                     null,
                     BlockPos.m_274561_(entity.m_20185_(), entity.m_20186_(), entity.m_20189_()),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:fallen_jumpscare")),
                     SoundSource.AMBIENT,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.m_7785_(
                     entity.m_20185_(),
                     entity.m_20186_(),
                     entity.m_20189_(),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("apollyon:fallen_jumpscare")),
                     SoundSource.AMBIENT,
                     1.0F,
                     1.0F,
                     false
                  );
               }
            }

            ApollyonMod.queueServerWork(20, () -> {
               String _setvalx = "buffer";
               entity.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                  capability.fallen_overlay = _setval;
                  capability.syncPlayerVariables(entity);
               });
               ApollyonMod.queueServerWork(20, () -> {
                  String _setvalxx = "false";
                  entity.getCapability(ApollyonModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
                     capability.fallen_overlay = _setvalx;
                     capability.syncPlayerVariables(entity);
                  });
               });
            });
         }
      }
   }
}
