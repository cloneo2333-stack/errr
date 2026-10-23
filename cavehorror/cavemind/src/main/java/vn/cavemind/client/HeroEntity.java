package vn.cavemind.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class HeroEntity extends RemotePlayer {
    private static final PlayerTeam TEAM = new PlayerTeam(new Scoreboard(), "cavemind_hero");

    static {
        TEAM.setNameTagVisibility(Team.Visibility.NEVER);
        TEAM.setCollisionRule(Team.CollisionRule.NEVER);
    }

    public final int slot;
    ResourceLocation skin;
    String model = "default";
    boolean eyes, shoulder;

    public HeroEntity(ClientLevel level, GameProfile profile, int slot) {
        super(level, profile);
        this.slot = slot;
        this.noPhysics = true;
    }

    @Override
    public ResourceLocation getSkinTextureLocation() {
        return skin != null ? skin : super.getSkinTextureLocation();
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public ResourceLocation getCloakTextureLocation() {
        return null;
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart part) {
        return part != PlayerModelPart.CAPE;
    }

    @Override
    public PlayerTeam getTeam() {
        return TEAM;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
