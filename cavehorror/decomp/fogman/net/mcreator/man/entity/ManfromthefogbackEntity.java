package net.mcreator.man.entity;

import net.mcreator.man.init.ManModEntities;
import net.mcreator.man.procedures.ManfromthefogOnEntityTickUpdate2Procedure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages.SpawnEntity;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController.State;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ManfromthefogbackEntity extends Monster implements GeoEntity {
   public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.m_135353_(ManfromthefogbackEntity.class, EntityDataSerializers.f_135035_);
   public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.m_135353_(ManfromthefogbackEntity.class, EntityDataSerializers.f_135030_);
   public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.m_135353_(ManfromthefogbackEntity.class, EntityDataSerializers.f_135030_);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private boolean swinging;
   private boolean lastloop;
   private long lastSwing;
   public String animationprocedure = "empty";
   String prevAnim = "empty";

   public ManfromthefogbackEntity(SpawnEntity packet, Level world) {
      this((EntityType<ManfromthefogbackEntity>)ManModEntities.MANFROMTHEFOGBACK.get(), world);
   }

   public ManfromthefogbackEntity(EntityType<ManfromthefogbackEntity> type, Level world) {
      super(type, world);
      this.f_21364_ = 0;
      this.m_21557_(false);
      this.m_274367_(0.6F);
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(SHOOT, false);
      this.f_19804_.m_135372_(ANIMATION, "undefined");
      this.f_19804_.m_135372_(TEXTURE, "stalker");
   }

   public void setTexture(String texture) {
      this.f_19804_.m_135381_(TEXTURE, texture);
   }

   public String getTexture() {
      return (String)this.f_19804_.m_135370_(TEXTURE);
   }

   public Packet<ClientGamePacketListener> m_5654_() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   protected void m_8099_() {
      super.m_8099_();
      this.f_21345_.m_25352_(1, new LookAtPlayerGoal(this, Player.class, 200.0F) {
         public boolean m_8045_() {
            double x = ManfromthefogbackEntity.this.m_20185_();
            double y = ManfromthefogbackEntity.this.m_20186_();
            double z = ManfromthefogbackEntity.this.m_20189_();
            Entity entity = ManfromthefogbackEntity.this;
            Level world = ManfromthefogbackEntity.this.m_9236_();
            return super.m_8045_();
         }
      });
      this.f_21345_.m_25352_(2, new LookAtPlayerGoal(this, Player.class, 200.0F) {
         public boolean m_8045_() {
            double x = ManfromthefogbackEntity.this.m_20185_();
            double y = ManfromthefogbackEntity.this.m_20186_();
            double z = ManfromthefogbackEntity.this.m_20189_();
            Entity entity = ManfromthefogbackEntity.this;
            Level world = ManfromthefogbackEntity.this.m_9236_();
            return super.m_8045_();
         }
      });
      this.f_21345_.m_25352_(3, new LookAtPlayerGoal(this, Player.class, 200.0F) {
         public boolean m_8045_() {
            double x = ManfromthefogbackEntity.this.m_20185_();
            double y = ManfromthefogbackEntity.this.m_20186_();
            double z = ManfromthefogbackEntity.this.m_20189_();
            Entity entity = ManfromthefogbackEntity.this;
            Level world = ManfromthefogbackEntity.this.m_9236_();
            return super.m_8045_();
         }
      });
      this.f_21345_.m_25352_(4, new LookAtPlayerGoal(this, Player.class, 200.0F) {
         public boolean m_8045_() {
            double x = ManfromthefogbackEntity.this.m_20185_();
            double y = ManfromthefogbackEntity.this.m_20186_();
            double z = ManfromthefogbackEntity.this.m_20189_();
            Entity entity = ManfromthefogbackEntity.this;
            Level world = ManfromthefogbackEntity.this.m_9236_();
            return super.m_8045_();
         }
      });
   }

   public MobType m_6336_() {
      return MobType.f_21640_;
   }

   public SoundEvent m_7515_() {
      return (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("man:lurk"));
   }

   public SoundEvent m_7975_(DamageSource ds) {
      return (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
   }

   public SoundEvent m_5592_() {
      return (SoundEvent)ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
   }

   public boolean m_6469_(DamageSource source, float amount) {
      return source.m_276093_(DamageTypes.f_268671_) ? false : super.m_6469_(source, amount);
   }

   public void m_7380_(CompoundTag compound) {
      super.m_7380_(compound);
      compound.m_128359_("Texture", this.getTexture());
   }

   public void m_7378_(CompoundTag compound) {
      super.m_7378_(compound);
      if (compound.m_128441_("Texture")) {
         this.setTexture(compound.m_128461_("Texture"));
      }
   }

   public void m_6075_() {
      super.m_6075_();
      ManfromthefogOnEntityTickUpdate2Procedure.execute(this.m_9236_(), this.m_20185_(), this.m_20186_(), this.m_20189_(), this);
      this.m_6210_();
   }

   public EntityDimensions m_6972_(Pose p_33597_) {
      return super.m_6972_(p_33597_).m_20388_(1.0F);
   }

   public static void init() {
   }

   public static Builder createAttributes() {
      Builder builder = Mob.m_21552_();
      builder = builder.m_22268_(Attributes.f_22279_, 0.3);
      builder = builder.m_22268_(Attributes.f_22276_, 10.0);
      builder = builder.m_22268_(Attributes.f_22284_, 0.0);
      builder = builder.m_22268_(Attributes.f_22281_, 7.0);
      return builder.m_22268_(Attributes.f_22277_, 16.0);
   }

   private PlayState movementPredicate(AnimationState event) {
      if (this.animationprocedure.equals("empty")) {
         return this.m_5912_() && event.isMoving()
            ? event.setAndContinue(RawAnimation.begin().thenLoop("attack"))
            : event.setAndContinue(RawAnimation.begin().thenLoop("stare"));
      } else {
         return PlayState.STOP;
      }
   }

   private PlayState procedurePredicate(AnimationState event) {
      if (!this.animationprocedure.equals("empty") && event.getController().getAnimationState() == State.STOPPED
         || !this.animationprocedure.equals(this.prevAnim) && !this.animationprocedure.equals("empty")) {
         if (!this.animationprocedure.equals(this.prevAnim)) {
            event.getController().forceAnimationReset();
         }

         event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
         if (event.getController().getAnimationState() == State.STOPPED) {
            this.animationprocedure = "empty";
            event.getController().forceAnimationReset();
         }
      } else if (this.animationprocedure.equals("empty")) {
         this.prevAnim = "empty";
         return PlayState.STOP;
      }

      this.prevAnim = this.animationprocedure;
      return PlayState.CONTINUE;
   }

   protected void m_6153_() {
      this.f_20919_++;
      if (this.f_20919_ == 20) {
         this.m_142687_(RemovalReason.KILLED);
         this.m_21226_();
      }
   }

   public String getSyncedAnimation() {
      return (String)this.f_19804_.m_135370_(ANIMATION);
   }

   public void setAnimation(String animation) {
      this.f_19804_.m_135381_(ANIMATION, animation);
   }

   public void registerControllers(ControllerRegistrar data) {
      data.add(new AnimationController[]{new AnimationController(this, "movement", 4, this::movementPredicate)});
      data.add(new AnimationController[]{new AnimationController(this, "procedure", 4, this::procedurePredicate)});
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
