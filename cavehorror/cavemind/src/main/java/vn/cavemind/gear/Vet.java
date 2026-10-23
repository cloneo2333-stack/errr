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

// Painted-on grime for the maze: vet_san lies flat on the floor, vet_tuong hangs on the wall behind it (like Scrawl).
// Textures come from tools/gen_decals.py; the kind numbers must match its lists.
public final class Vet extends HorizontalDirectionalBlock {
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 8);
    public static final int ARC = 0, CHALK = 1, BOARD = 2, MUD = 3, HANDS = 4, WAX = 5, WET = 6, DRAG = 7, PUDDLE = 8;
    public static final int STAIN_TOP = 0, STAIN_MID = 1, STAIN_BOT = 2, HAND = 3, CLAW = 4, MOLD = 5, CRACK = 6, PEEL = 7, BOARDS = 8;

    Vet() {
        super(BlockBehaviour.Properties.of().strength(-1f, 3600000f).sound(SoundType.WOOL).noOcclusion().noCollission().noLootTable());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, KIND);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter lvl, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    // facing: the top edge of the texture points this way
    public static BlockState floor(Direction facing, int kind) {
        return Gear.VET_SAN.get().defaultBlockState().setValue(FACING, facing).setValue(KIND, kind);
    }

    // facing: away from the wall the decal is painted on
    public static BlockState wall(Direction facing, int kind) {
        return Gear.VET_TUONG.get().defaultBlockState().setValue(FACING, facing).setValue(KIND, kind);
    }
}
