package net.mcreator.man.block;

import io.netty.buffer.Unpooled;
import net.mcreator.man.block.entity.CassetteRecorderBlockEntity;
import net.mcreator.man.procedures.CassetterecorderBlockDestroyedByPlayerProcedure;
import net.mcreator.man.world.inventory.CassetteRecorderGuiMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class CassetteRecorderBlock extends Block implements EntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;

   public CassetteRecorderBlock() {
      super(
         Properties.m_284310_()
            .m_280658_(NoteBlockInstrument.BASEDRUM)
            .m_60918_(SoundType.f_56742_)
            .m_60913_(1.0F, 10.0F)
            .m_60955_()
            .m_60924_((bs, br, bp) -> false)
      );
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   public boolean m_7420_(BlockState state, BlockGetter reader, BlockPos pos) {
      return true;
   }

   public int m_7753_(BlockState state, BlockGetter worldIn, BlockPos pos) {
      return 0;
   }

   public VoxelShape m_5909_(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return Shapes.m_83040_();
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return switch ((Direction)state.m_61143_(FACING)) {
         case NORTH -> Shapes.m_83124_(
         m_49796_(2.0, 0.0, 2.0, 14.0, 3.0, 21.0),
         new VoxelShape[]{
            m_49796_(2.0, 0.0, -1.5, 14.0, 1.0, 2.0),
            m_49796_(2.0, 1.0, -1.5, 4.0, 3.0, 2.0),
            m_49796_(12.0, 1.0, -1.5, 14.0, 3.0, 2.0),
            m_49796_(10.0, 1.0, -1.0, 12.0, 2.5, 2.0),
            m_49796_(8.0, 1.0, -1.0, 10.0, 2.5, 2.0),
            m_49796_(6.0, 1.0, -1.0, 8.0, 2.5, 2.0),
            m_49796_(4.0, 1.0, -1.0, 6.0, 2.5, 2.0)
         }
      );
         case EAST -> Shapes.m_83124_(
         m_49796_(-5.0, 0.0, 2.0, 14.0, 3.0, 14.0),
         new VoxelShape[]{
            m_49796_(14.0, 0.0, 2.0, 17.5, 1.0, 14.0),
            m_49796_(14.0, 1.0, 2.0, 17.5, 3.0, 4.0),
            m_49796_(14.0, 1.0, 12.0, 17.5, 3.0, 14.0),
            m_49796_(14.0, 1.0, 10.0, 17.0, 2.5, 12.0),
            m_49796_(14.0, 1.0, 8.0, 17.0, 2.5, 10.0),
            m_49796_(14.0, 1.0, 6.0, 17.0, 2.5, 8.0),
            m_49796_(14.0, 1.0, 4.0, 17.0, 2.5, 6.0)
         }
      );
         case WEST -> Shapes.m_83124_(
         m_49796_(2.0, 0.0, 2.0, 21.0, 3.0, 14.0),
         new VoxelShape[]{
            m_49796_(-1.5, 0.0, 2.0, 2.0, 1.0, 14.0),
            m_49796_(-1.5, 1.0, 12.0, 2.0, 3.0, 14.0),
            m_49796_(-1.5, 1.0, 2.0, 2.0, 3.0, 4.0),
            m_49796_(-1.0, 1.0, 4.0, 2.0, 2.5, 6.0),
            m_49796_(-1.0, 1.0, 6.0, 2.0, 2.5, 8.0),
            m_49796_(-1.0, 1.0, 8.0, 2.0, 2.5, 10.0),
            m_49796_(-1.0, 1.0, 10.0, 2.0, 2.5, 12.0)
         }
      );
         default -> Shapes.m_83124_(
         m_49796_(2.0, 0.0, -5.0, 14.0, 3.0, 14.0),
         new VoxelShape[]{
            m_49796_(2.0, 0.0, 14.0, 14.0, 1.0, 17.5),
            m_49796_(12.0, 1.0, 14.0, 14.0, 3.0, 17.5),
            m_49796_(2.0, 1.0, 14.0, 4.0, 3.0, 17.5),
            m_49796_(4.0, 1.0, 14.0, 6.0, 2.5, 17.0),
            m_49796_(6.0, 1.0, 14.0, 8.0, 2.5, 17.0),
            m_49796_(8.0, 1.0, 14.0, 10.0, 2.5, 17.0),
            m_49796_(10.0, 1.0, 14.0, 12.0, 2.5, 17.0)
         }
      );
      };
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING});
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_().m_122424_());
   }

   public BlockState m_6843_(BlockState state, Rotation rot) {
      return (BlockState)state.m_61124_(FACING, rot.m_55954_((Direction)state.m_61143_(FACING)));
   }

   public BlockState m_6943_(BlockState state, Mirror mirrorIn) {
      return state.m_60717_(mirrorIn.m_54846_((Direction)state.m_61143_(FACING)));
   }

   public void m_6256_(BlockState blockstate, Level world, BlockPos pos, Player entity) {
      super.m_6256_(blockstate, world, pos, entity);
      CassetterecorderBlockDestroyedByPlayerProcedure.execute(world, (double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_());
   }

   public InteractionResult m_6227_(BlockState blockstate, Level world, final BlockPos pos, Player entity, InteractionHand hand, BlockHitResult hit) {
      super.m_6227_(blockstate, world, pos, entity, hand, hit);
      if (entity instanceof ServerPlayer player) {
         NetworkHooks.openScreen(player, new MenuProvider() {
            public Component m_5446_() {
               return Component.m_237113_("Cassette Recorder");
            }

            public AbstractContainerMenu m_7208_(int id, Inventory inventory, Player player) {
               return new CassetteRecorderGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).m_130064_(pos));
            }
         }, pos);
      }

      return InteractionResult.SUCCESS;
   }

   public MenuProvider m_7246_(BlockState state, Level worldIn, BlockPos pos) {
      return worldIn.m_7702_(pos) instanceof MenuProvider menuProvider ? menuProvider : null;
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new CassetteRecorderBlockEntity(pos, state);
   }

   public boolean m_8133_(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
      super.m_8133_(state, world, pos, eventID, eventParam);
      BlockEntity blockEntity = world.m_7702_(pos);
      return blockEntity == null ? false : blockEntity.m_7531_(eventID, eventParam);
   }

   public void m_6810_(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.m_60734_() != newState.m_60734_()) {
         if (world.m_7702_(pos) instanceof CassetteRecorderBlockEntity be) {
            Containers.m_19002_(world, pos, be);
            world.m_46717_(pos, this);
         }

         super.m_6810_(state, world, pos, newState, isMoving);
      }
   }

   public boolean m_7278_(BlockState state) {
      return true;
   }

   public int m_6782_(BlockState blockState, Level world, BlockPos pos) {
      return world.m_7702_(pos) instanceof CassetteRecorderBlockEntity be ? AbstractContainerMenu.m_38938_(be) : 0;
   }
}
