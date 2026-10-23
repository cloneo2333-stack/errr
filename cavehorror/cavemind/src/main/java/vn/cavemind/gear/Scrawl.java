package vn.cavemind.gear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Scrawl extends HorizontalDirectionalBlock {
    public static final IntegerProperty TEXT = IntegerProperty.create("text", 0, 31);
    public static final int RULES = 0, NAMES = 6, EIGHTH = 13, BOARD = 14, TALLY = 15, CORRIDOR = 16;

    Scrawl() {
        super(BlockBehaviour.Properties.of().strength(-1f, 3600000f).sound(SoundType.WOOL).noOcclusion().noCollission().noLootTable());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TEXT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, TEXT);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter lvl, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    public static BlockState of(Direction facing, int text) {
        return Gear.SCRAWL.get().defaultBlockState().setValue(FACING, facing).setValue(TEXT, text);
    }
}
