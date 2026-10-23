package apollyon.procedures;

import apollyon.entity.TheFallenEntity;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class TheFallenJumpscaresProcedure {
   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END) {
         execute(event, event.player.m_9236_(), event.player);
      }
   }

   public static void execute(LevelAccessor world, Entity entity) {
      execute(null, world, entity);
   }

   private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
      if (entity != null) {
         double raytrace_distance = 0.0;
         String found_entity_name = "";
         boolean entity_found = false;
         raytrace_distance = 0.0;
         entity_found = false;

         for (int index0 = 0; index0 < 15; index0++) {
            if (!world.m_6443_(
                     TheFallenEntity.class,
                     AABB.m_165882_(
                        new Vec3(
                           (double)entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                    Block.COLLIDER,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123341_(),
                           (double)entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                    Block.COLLIDER,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123342_(),
                           (double)entity.m_9236_()
                              .m_45547_(
                                 new ClipContext(
                                    entity.m_20299_(1.0F),
                                    entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                    Block.COLLIDER,
                                    Fluid.NONE,
                                    entity
                                 )
                              )
                              .m_82425_()
                              .m_123343_()
                        ),
                        1.0,
                        1.0,
                        1.0
                     ),
                     e -> true
                  )
                  .isEmpty()
               && world.m_6443_(
                        LivingEntity.class,
                        AABB.m_165882_(
                           new Vec3(
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           ),
                           1.0,
                           1.0,
                           1.0
                        ),
                        e -> true
                     )
                     .stream()
                     .sorted(
                        (new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           })
                           .compareDistOf(
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                     )
                     .findFirst()
                     .orElse(null)
                  != entity) {
               entity_found = true;
               if (world.m_6443_(
                        LivingEntity.class,
                        AABB.m_165882_(
                           new Vec3(
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           ),
                           1.0,
                           1.0,
                           1.0
                        ),
                        e -> true
                     )
                     .stream()
                     .sorted(
                        (new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           })
                           .compareDistOf(
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                     )
                     .findFirst()
                     .orElse(null)
                  != null) {
                  found_entity_name = world.m_6443_(
                        TheFallenEntity.class,
                        AABB.m_165882_(
                           new Vec3(
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           ),
                           1.0,
                           1.0,
                           1.0
                        ),
                        e -> true
                     )
                     .stream()
                     .sorted(
                        (new Object() {
                              Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                 return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                              }
                           })
                           .compareDistOf(
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123341_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123342_(),
                              (double)entity.m_9236_()
                                 .m_45547_(
                                    new ClipContext(
                                       entity.m_20299_(1.0F),
                                       entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                       Block.COLLIDER,
                                       Fluid.NONE,
                                       entity
                                    )
                                 )
                                 .m_82425_()
                                 .m_123343_()
                           )
                     )
                     .findFirst()
                     .orElse(null)
                     .m_5446_()
                     .getString();
                  break;
               }
            } else {
               entity_found = false;
               raytrace_distance++;
            }
         }

         if (entity_found
            && (
               world.m_6443_(
                           LivingEntity.class,
                           AABB.m_165882_(
                              new Vec3(
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123341_(),
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123342_(),
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123343_()
                              ),
                              1.0,
                              1.0,
                              1.0
                           ),
                           e -> true
                        )
                        .stream()
                        .sorted(
                           (new Object() {
                                 Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                    return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                                 }
                              })
                              .compareDistOf(
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123341_(),
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123342_(),
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123343_()
                              )
                        )
                        .findFirst()
                        .orElse(null)
                     != null
                  || !(
                     world.m_6443_(
                           TheFallenEntity.class,
                           AABB.m_165882_(
                              new Vec3(
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123341_(),
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123342_(),
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123343_()
                              ),
                              1.0,
                              1.0,
                              1.0
                           ),
                           e -> true
                        )
                        .stream()
                        .sorted(
                           (new Object() {
                                 Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                                    return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                                 }
                              })
                              .compareDistOf(
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123341_(),
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123342_(),
                                 (double)entity.m_9236_()
                                    .m_45547_(
                                       new ClipContext(
                                          entity.m_20299_(1.0F),
                                          entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                          Block.COLLIDER,
                                          Fluid.NONE,
                                          entity
                                       )
                                    )
                                    .m_82425_()
                                    .m_123343_()
                              )
                        )
                        .findFirst()
                        .orElse(null) instanceof TheFallenEntity
                  )
            )) {
            TheFallenOverlaysProcedure.execute(world, entity);
            if (entity instanceof ServerPlayer _player) {
               Advancement _adv = _player.f_8924_.m_129889_().m_136041_(new ResourceLocation("apollyon:thats_not_an_enderman"));
               AdvancementProgress _ap = _player.m_8960_().m_135996_(_adv);
               if (!_ap.m_8193_()) {
                  for (String criteria : _ap.m_8219_()) {
                     _player.m_8960_().m_135988_(_adv, criteria);
                  }
               }
            }

            world.m_6443_(
                  TheFallenEntity.class,
                  AABB.m_165882_(
                     new Vec3(
                        (double)entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F),
                                 entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                 Block.COLLIDER,
                                 Fluid.NONE,
                                 entity
                              )
                           )
                           .m_82425_()
                           .m_123341_(),
                        (double)entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F),
                                 entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                 Block.COLLIDER,
                                 Fluid.NONE,
                                 entity
                              )
                           )
                           .m_82425_()
                           .m_123342_(),
                        (double)entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F),
                                 entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                 Block.COLLIDER,
                                 Fluid.NONE,
                                 entity
                              )
                           )
                           .m_82425_()
                           .m_123343_()
                     ),
                     1.0,
                     1.0,
                     1.0
                  ),
                  e -> true
               )
               .stream()
               .sorted(
                  (new Object() {
                        Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
                           return Comparator.comparingDouble(_entcnd -> _entcnd.m_20275_(_x, _y, _z));
                        }
                     })
                     .compareDistOf(
                        (double)entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F),
                                 entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                 Block.COLLIDER,
                                 Fluid.NONE,
                                 entity
                              )
                           )
                           .m_82425_()
                           .m_123341_(),
                        (double)entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F),
                                 entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                 Block.COLLIDER,
                                 Fluid.NONE,
                                 entity
                              )
                           )
                           .m_82425_()
                           .m_123342_(),
                        (double)entity.m_9236_()
                           .m_45547_(
                              new ClipContext(
                                 entity.m_20299_(1.0F),
                                 entity.m_20299_(1.0F).m_82549_(entity.m_20252_(1.0F).m_82490_(raytrace_distance)),
                                 Block.COLLIDER,
                                 Fluid.NONE,
                                 entity
                              )
                           )
                           .m_82425_()
                           .m_123343_()
                     )
               )
               .findFirst()
               .orElse(null)
               .m_6074_();
         }
      }
   }
}
