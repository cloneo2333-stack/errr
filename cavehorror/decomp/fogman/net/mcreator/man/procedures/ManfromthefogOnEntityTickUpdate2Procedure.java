package net.mcreator.man.procedures;

import java.util.Comparator;
import net.mcreator.man.configuration.ManconfigConfiguration;
import net.mcreator.man.entity.ManfromthefogbackEntity;
import net.mcreator.man.network.ManModVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class ManfromthefogOnEntityTickUpdate2Procedure {
   public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
      if (entity != null) {
         if (!world.m_6443_(
                  Player.class,
                  AABB.m_165882_(
                     new Vec3(x, y, z),
                     (Double)ManconfigConfiguration.VANISH_DISTANCE.get(),
                     (Double)ManconfigConfiguration.VANISH_DISTANCE.get(),
                     (Double)ManconfigConfiguration.VANISH_DISTANCE.get()
                  ),
                  e -> true
               )
               .isEmpty()
            && !(new Object() {
                  public boolean checkGamemode(Entity _ent) {
                     if (_ent instanceof ServerPlayer _serverPlayer) {
                        return _serverPlayer.f_8941_.m_9290_() == GameType.CREATIVE;
                     } else {
                        return _ent.m_9236_().m_5776_() && _ent instanceof Player _player
                           ? Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()) != null
                              && Minecraft.m_91087_().m_91403_().m_104949_(_player.m_36316_().getId()).m_105325_() == GameType.CREATIVE
                           : false;
                     }
                  }
               })
               .checkGamemode(
                  world.m_6443_(
                        Player.class,
                        AABB.m_165882_(
                           new Vec3(x, y, z),
                           (Double)ManconfigConfiguration.VANISH_DISTANCE.get(),
                           (Double)ManconfigConfiguration.VANISH_DISTANCE.get(),
                           (Double)ManconfigConfiguration.VANISH_DISTANCE.get()
                        ),
                        e -> true
                     )
                     .stream()
                     .sorted((new Object() {
                        Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                           return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                        }
                     }).compareDistOf(x, y, z))
                     .findFirst()
                     .orElse(null)
               )
            && !(entity instanceof ManfromthefogbackEntity)) {
            if (!entity.m_9236_().m_5776_()) {
               entity.m_146870_();
            }

            if (world instanceof Level _level) {
               if (!_level.m_5776_()) {
                  _level.m_5594_(
                     null,
                     BlockPos.m_274561_(x, y, z),
                     (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:vanish")),
                     SoundSource.HOSTILE,
                     1.0F,
                     1.0F
                  );
               } else {
                  _level.m_7785_(
                     x, y, z, (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:vanish")), SoundSource.HOSTILE, 1.0F, 1.0F, false
                  );
               }
            }

            ManModVariables.MapVariables.get(world).ring_timer = 0.0;
            ManModVariables.MapVariables.get(world).syncData(world);
            if ((Boolean)ManconfigConfiguration.LIGHTNINGS.get() && world instanceof ServerLevel _levelx) {
               LightningBolt entityToSpawn = (LightningBolt)EntityType.f_20465_.m_20615_(_levelx);
               entityToSpawn.m_20219_(Vec3.m_82539_(BlockPos.m_274561_(x, y, z)));
               entityToSpawn.m_20874_(true);
               _levelx.m_7967_(entityToSpawn);
            }
         }
      }
   }
}
