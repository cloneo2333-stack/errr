package vn.cavemind.day;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.phys.Vec3;
import vn.cavemind.compat.Compat;
import vn.cavemind.damned.Haunt;
import vn.cavemind.dev.Dev;
import vn.cavemind.director.Director;
import vn.cavemind.director.Dread;
import vn.cavemind.memory.Moment;
import vn.cavemind.memory.Profile;
import vn.cavemind.sense.Watch;
import vn.cavemind.world.Fx;
import vn.cavemind.world.Motion;
import vn.cavemind.world.Spots;

final class HideSeek extends Piece {
    static final double W = 0.6, H = 1.8;

    int stage, seekLeft, foundLeft, glimpses;
    long placedAt, nextPlace = -1;
    int hideDelay;
    double lastDist = 50;
    Vec3 lastSpot;
    BlockPos base;
    boolean owl, acted, gone, skipped, broke, leaned;
    double placeDist;
    String resp;
    long respAt;
    int hideTicks;

    @Override
    String id() {
        return "seek";
    }

    @Override
    String vi() {
        return "trốn tìm (nó không bao giờ cử động khi bạn đang nhìn)";
    }

    @Override
    boolean begin(ServerPlayer p, Watch w, Profile pr, long now) {
        RandomSource r = p.getRandom();
        base = p.blockPosition();
        seekLeft = 2 + r.nextInt(3);
        foundLeft = 2 + r.nextInt(2);
        owl = r.nextFloat() < 0.55f;
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(40, 64).dir(0, 180).body(W, H).tries(60)
            .when(k -> !k.sight.on && k.vis >= 0.5 && Math.abs(k.feet.getY() - p.getY()) < 14)
            .rank(k -> k.vis + (k.trunk ? 0.6 : 0)));
        if (c == null) return false;
        float yaw = facingTrunk(p.serverLevel(), c.feet);
        if (Float.isNaN(yaw)) yaw = yawTo(p.position(), c.center) + (r.nextFloat() - 0.5f) * 70f;
        if (!put(p, c.feet, yaw, "cm_count", now)) return false;
        Dev.say(p, "Nó đang úp mặt đếm, " + Dev.where(p, c.center) + ". Nó chỉ dời chỗ lúc bạn KHÔNG nhìn");
        return true;
    }

    @Override
    boolean tick(ServerPlayer p, Watch w, Profile pr, long now) {
        if (now - start > 9000 || w.underground) return false;
        Entity e = body(p);
        if (e == null && body != null) {
            body = null;
            resp = null;
            pr.bold(10);
            stage = 3;
            owl = true;
            acted = false;
            nextPlace = now + 60 + p.getRandom().nextInt(80);
            Dev.say(p, "Bạn đánh nó — nó tan đi. Trò chơi chưa kết thúc đâu");
            return true;
        }
        if (e == null) {
            if (gone) return false;
            if (nextPlace >= 0 && now >= nextPlace) {
                if (place(p, w, now)) nextPlace = -1;
                else if (now - nextPlace > 900) return false;
                else nextPlace = now + 20;
            }
            return true;
        }
        boolean was = noticed;
        boolean on = watch(p, w, e);
        double dist = Math.sqrt(e.distanceToSqr(p));
        if (noticed && !was) glimpse(p, dist);
        if (stage == 3) return ending(p, pr, e, on, dist, now);
        if (stage <= 1 && hiding(p, w, pr, e, on, now)) return false;
        if (!noticed) {
            if (now - placedAt > (stage == 0 ? 1800 : 1000) && !on) inPath(p, w, now);
            return true;
        }
        if (resp == null && on && dist < placeDist - 8 && dist < 32 && w.speed > 0.03) react(p, pr, dist, now);
        if (resp != null) {
            Boolean r = handle(p, w, e, on, dist, now);
            if (r != null) return r;
        }
        if (dist < 3.5 && on) {
            vanish(p, e, now);
            stage = 3;
            nextPlace = now + 100 + p.getRandom().nextInt(100);
            Dev.say(p, "Bạn chạm tới quá gần — nó không còn ở đó. Lượt cuối sẽ tới");
            return true;
        }
        if (stage == 1 && on && stare > 140) {
            stage = 2;
            seekLeft = 0;
            Dev.say(p, "Bạn nhìn nó quá lâu — nó đã biết bạn ở đâu");
        }
        if (away >= hideDelay) {
            Vec3 from = e.position();
            lastDist = dist;
            lastSpot = from;
            vanish(p, e, now);
            advance(p);
            if (stage == 1 && !skipped && p.getRandom().nextFloat() < 0.15f) {
                skipped = true;
                nextPlace = now + 240 + p.getRandom().nextInt(260);
                Dev.say(p, "Lần này nó không ở đâu cả. Cứ làm việc tiếp đi");
                return true;
            }
            if (place(p, w, now)) steps(p, from, body(p));
            else nextPlace = now + 20;
        }
        return true;
    }

    private boolean hiding(ServerPlayer p, Watch w, Profile pr, Entity e, boolean on, long now) {
        ServerLevel lvl = p.serverLevel();
        BlockPos eye = BlockPos.containing(p.getEyePosition());
        boolean dark = lvl.getMaxLocalRawBrightness(eye) <= 6 || !lvl.canSeeSky(eye) && lvl.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, eye) <= 6;
        boolean hid = p.isCrouching() && w.speed < 0.03 && dark && !on;
        hideTicks = hid ? hideTicks + 1 : Math.max(0, hideTicks - 3);
        int need = 260 + Math.min(4, pr.hidden) * 90;
        if (hid && hideTicks % 80 == 40) Dev.say(p, "Bạn đang trốn (" + hideTicks + "/" + need + "): thụp xuống, trong tối, không cử động, đừng nhìn nó");
        if (hideTicks < need) return false;
        BlockPos at = e.blockPosition();
        Fx.sound(p, Holder.direct(SoundEvents.CANDLE_EXTINGUISH), SoundSource.HOSTILE, e.position(), 0.5f, 0.6f);
        vanish(p, e, now);
        candle(lvl, at, false);
        pr.hidden++;
        pr.moment(new Moment(Moment.HID, Dread.day(lvl), Dread.time(lvl), at, 0, 0));
        Director.scare(p, 3);
        Dev.say(p, "Nó không tìm thấy bạn. Nó bỏ cuộc và để lại một ngọn nến đã tắt. Lần sau nó sẽ tìm lâu hơn");
        gone = true;
        return true;
    }

    private void advance(ServerPlayer p) {
        switch (stage) {
            case 0 -> stage = 1;
            case 1 -> {
                if (--seekLeft <= 0) stage = 2;
            }
            case 2 -> {
                if (--foundLeft <= 0 || lastDist <= 11) stage = 3;
            }
            default -> {}
        }
    }

    private boolean place(ServerPlayer p, Watch w, long now) {
        return switch (stage) {
            case 1 -> placeSeek(p, w, now);
            case 2 -> placeFound(p, w, now);
            default -> placeEnd(p, w, now);
        };
    }

    private boolean placeSeek(ServerPlayer p, Watch w, long now) {
        RandomSource r = p.getRandom();
        double d = Mth.clamp(lastDist * (0.65 + r.nextDouble() * 0.3) + (r.nextFloat() < 0.2f ? 12 : 0), 16, 56);
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(d - 5, d + 5).dir(0, 180).body(W, H).tries(50)
            .when(k -> !k.sight.on && k.vis >= 0.45 && Math.abs(k.feet.getY() - p.getY()) < 10)
            .rank(k -> k.vis + (k.trunk ? 0.5 : 0) + (k.cover ? 0.2 : 0)));
        if (c == null) return false;
        float toMe = yawTo(c.center, p.position());
        String anim;
        float yaw;
        float roll = r.nextFloat();
        if (c.trunk && roll < 0.6f) {
            anim = "cm_peek";
            yaw = toMe;
        } else if (roll < 0.75f) {
            anim = r.nextBoolean() ? "cm_search_a" : "cm_search_b";
            yaw = toMe + 70 + r.nextFloat() * 220;
        } else {
            anim = "cm_low";
            yaw = toMe + 40 + r.nextFloat() * 280;
        }
        if (!put(p, c.feet, yaw, anim, now)) return false;
        Dev.say(p, "Nó đang đi tìm bạn — giờ ở " + Dev.where(p, c.center));
        return true;
    }

    private boolean placeFound(ServerPlayer p, Watch w, long now) {
        RandomSource r = p.getRandom();
        double d = Math.max(7, lastDist * (0.55 + r.nextDouble() * 0.15));
        Spots.Cand c = null;
        if (lastSpot != null) {
            double rel = Mth.wrapDegrees(yawTo(p.position(), lastSpot) - p.getYRot());
            c = Spots.find(p, w, new Spots.Spec().ring(d - 2, d + 2).dir(rel, 22).body(W, H).tries(40)
                .when(k -> !k.sight.on && k.vis >= 0.5 && Math.abs(k.feet.getY() - p.getY()) < 8).rank(k -> k.vis));
        }
        if (c == null) {
            c = Spots.find(p, w, new Spots.Spec().ring(d - 3, d + 3).dir(180, 120).body(W, H).tries(40)
                .when(k -> !k.sight.on && k.vis >= 0.5 && Math.abs(k.feet.getY() - p.getY()) < 8).rank(k -> k.vis));
        }
        if (c == null) return false;
        String[] poses = {"cm_still", "cm_tall", "cm_low"};
        String anim = poses[(glimpses + r.nextInt(2)) % poses.length];
        if (!put(p, c.feet, yawTo(c.center, p.getEyePosition()), anim, now)) return false;
        Dev.say(p, "Nó ĐÃ THẤY bạn. Mỗi lần bạn quay lại nó gần hơn — " + Dev.where(p, c.center));
        return true;
    }

    private boolean placeEnd(ServerPlayer p, Watch w, long now) {
        ServerLevel lvl = p.serverLevel();
        acted = false;
        if (!owl && base != null && Math.sqrt(base.distSqr(p.blockPosition())) >= 16) {
            BlockPos feet = Spots.nearFloor(lvl, base, 2, 2);
            if (feet != null) {
                Spots.Cand c = Spots.eval(p, w, feet, W, H);
                if (!c.sight.on || c.vis == 0) {
                    if (put(p, feet, yawTo(c.center, p.getEyePosition()), "cm_still", now)) {
                        Dev.say(p, "Lượt cuối: nó đứng đúng chỗ bạn đứng lúc trò chơi bắt đầu — " + Dev.where(p, c.center));
                        return true;
                    }
                }
            }
            owl = true;
        }
        Spots.Cand c = Spots.find(p, w, new Spots.Spec().ring(5, 8).dir(180, 50).body(W, H).tries(40)
            .when(k -> !k.sight.on && k.vis >= 0.6 && Math.abs(k.feet.getY() - p.getY()) < 3).rank(k -> k.vis));
        if (c == null) return false;
        owl = true;
        if (!put(p, c.feet, yawTo(p.position(), c.center), "cm_still", now)) return false;
        Dev.say(p, "Lượt cuối: nó đứng ngay sau lưng bạn, quay lưng về phía bạn — " + Dev.where(p, c.center));
        return true;
    }

    private boolean ending(ServerPlayer p, Profile pr, Entity e, boolean on, double dist, long now) {
        ServerLevel lvl = p.serverLevel();
        if (!acted && on && stare >= (owl ? 20 : 30)) {
            acted = true;
            Motion.play(e, owl ? "cm_owl" : "cm_point", 1 << 30, now);
            Vec3 at = e.getEyePosition();
            if (owl) {
                for (int i = 0; i < 3; i++) {
                    Fx.later(lvl, 18 + i * 17, () -> Fx.sound(p, Holder.direct(SoundEvents.BAMBOO_WOOD_BREAK), SoundSource.HOSTILE, at, 0.22f, 0.5f));
                }
            } else {
                Vec3 hand = e.position().add(0, 1.3, 0);
                for (int t : new int[]{38, 44, 50}) {
                    Fx.later(lvl, t, () -> Fx.sound(p, Holder.direct(SoundEvents.BONE_BLOCK_HIT), SoundSource.HOSTILE, hand, 0.35f, 0.55f));
                }
                Fx.later(lvl, 71, () -> Fx.sound(p, Holder.direct(SoundEvents.BAMBOO_WOOD_BREAK), SoundSource.HOSTILE, at, 0.4f, 0.62f));
            }
            Director.scare(p, 10);
            Dev.say(p, owl ? "Đầu nó từ từ xoay ngược ra sau để nhìn bạn" : "Nó từ từ giơ tay chỉ vào bạn");
        }
        boolean done = noticed && away >= 3 && (acted || now - placedAt > 600) || dist < 2.5 || !noticed && now - placedAt > 1400;
        if (!done) return now - placedAt < 2400;
        BlockPos at = e.blockPosition();
        vanish(p, e, now);
        candle(lvl, at);
        pr.moment(new Moment(Moment.SEEK, Dread.day(lvl), Dread.time(lvl), at, glimpses, owl ? 1 : 0));
        Dev.say(p, noticed ? "Trò chơi kết thúc. Chỗ nó vừa đứng có một ngọn nến đang cháy" : "Nó đã đứng sau lưng bạn suốt mà bạn không quay lại. Nó để lại một ngọn nến");
        gone = true;
        return false;
    }

    private void react(ServerPlayer p, Profile pr, double dist, long now) {
        pr.bold(5);
        double b = pr.bold / 100.0;
        String[] k = {"still", "bolt", "lean", "swap", "mirror"};
        double[] wt = {0.7 * (1 - b), broke ? 0 : 0.5 + b * 0.8, 0.6, 0.5 + b * 0.5, b * 0.7};
        double sum = 0;
        for (int i = 0; i < k.length; i++) {
            if (k[i].equals(pr.lastResp)) wt[i] *= 0.3;
            sum += wt[i];
        }
        double x = p.getRandom().nextDouble() * sum;
        int pick = 0;
        for (; pick < k.length - 1; pick++) {
            x -= wt[pick];
            if (x <= 0 && wt[pick] > 0) break;
        }
        resp = k[pick];
        respAt = now;
        pr.lastResp = resp;
        if (resp.equals("swap")) {
            hideDelay = 1;
            stage = 3;
            owl = true;
            acted = false;
            resp = null;
        } else if (resp.equals("bolt")) {
            broke = true;
        }
        Dev.say(p, "Bạn sáp lại gần nó (liều " + pr.bold + ") → nó " + switch (k[pick]) {
            case "bolt" -> "PHÁ LUẬT: rạp xuống và lao vụt đi ngay trước mắt bạn";
            case "lean" -> "đứng chờ bạn tới, rồi chồm mặt về phía bạn";
            case "swap" -> "chỉ chờ bạn chớp mắt — rồi nó ở ngay sau lưng";
            case "mirror" -> "bước về phía bạn đúng nhịp bạn bước";
            default -> "vẫn đứng im như tượng";
        });
    }

    private Boolean handle(ServerPlayer p, Watch w, Entity e, boolean on, double dist, long now) {
        ServerLevel lvl = p.serverLevel();
        switch (resp) {
            case "bolt" -> {
                if (!(e instanceof Mob m)) return null;
                if (now - respAt == 0) Motion.pose(e, "cm_low", now);
                Vec3 flee = e.position().add(e.position().subtract(p.position()).normalize().scale(30));
                if (now % 6 == 0) m.getNavigation().moveTo(flee.x, flee.y, flee.z, 2.4);
                if (away >= 2 || now - respAt > 110) {
                    lastDist = Math.max(dist + 12, 30);
                    lastSpot = e.position();
                    vanish(p, e, now);
                    resp = null;
                    nextPlace = now + 60 + p.getRandom().nextInt(80);
                }
                return true;
            }
            case "lean" -> {
                if (!leaned && dist <= 6.5) {
                    leaned = true;
                    Motion.play(e, "cm_lean", 1 << 30, now);
                    Haunt.breath(p, e.getEyePosition(), 0.4f);
                }
                if (leaned && (away >= 2 || dist < 3)) {
                    vanish(p, e, now);
                    resp = null;
                    stage = 3;
                    owl = true;
                    acted = false;
                    nextPlace = now + 80 + p.getRandom().nextInt(60);
                    return true;
                }
                return leaned ? Boolean.TRUE : null;
            }
            case "mirror" -> {
                if (!(e instanceof Mob m)) return null;
                if (dist <= 6) {
                    Haunt.breath(p, p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYRot()).scale(-1.2)), 0.45f);
                    vanish(p, e, now);
                    resp = null;
                    stage = 3;
                    owl = true;
                    acted = false;
                    nextPlace = now + 70 + p.getRandom().nextInt(60);
                    return true;
                }
                if (away >= 2) {
                    lastDist = dist;
                    lastSpot = e.position();
                    vanish(p, e, now);
                    resp = null;
                    nextPlace = now + 40;
                    return true;
                }
                Vec3 to = p.position().subtract(e.position());
                double closing = w.vel.dot(new Vec3(-to.x, 0, -to.z).normalize());
                if (closing > 0.02) {
                    Vec3 v = new Vec3(to.x, 0, to.z).normalize().scale(Math.min(0.3, closing * 1.1));
                    m.setDeltaMovement(v.x, m.getDeltaMovement().y, v.z);
                } else {
                    m.setDeltaMovement(0, m.getDeltaMovement().y, 0);
                }
                return true;
            }
            default -> {
                return null;
            }
        }
    }

    private void glimpse(ServerPlayer p, double dist) {
        glimpses++;
        Director.scare(p, stage >= 2 ? 7 : 4);
        String what = switch (stage) {
            case 0 -> "Bạn thấy nó đang úp mặt đếm";
            case 1 -> "Bạn thấy nó đang tìm kiếm";
            case 2 -> "Nó đứng nhìn thẳng vào bạn";
            default -> "Nó ở ngay đây";
        };
        Dev.say(p, what + " (cách " + (int) dist + "m). Quay đi để nó đổi chỗ");
    }

    private void inPath(ServerPlayer p, Watch w, long now) {
        double d = Mth.clamp(lastDist, 18, 50);
        Spots.Cand c;
        if (w.speed > 0.05) {
            double rel = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-w.vel.x, w.vel.z)) - p.getYRot());
            c = Spots.find(p, w, new Spots.Spec().ring(d, d + 12).dir(rel, 25).body(W, H).tries(30)
                .when(k -> (!k.sight.on || k.vis == 0) && Math.abs(k.feet.getY() - p.getY()) < 10).rank(k -> k.vis));
        } else {
            double side = Math.abs(w.yawSpeed) > 2 ? Math.signum(w.yawSpeed) : p.getRandom().nextBoolean() ? 1 : -1;
            double edge = Math.toDegrees(w.hHalf()) * 1.15;
            c = Spots.find(p, w, new Spots.Spec().ring(d - 6, d + 6).dir(side * edge, 12).body(W, H).tries(30)
                .when(k -> !k.sight.on && k.vis >= 0.5 && Math.abs(k.feet.getY() - p.getY()) < 10).rank(k -> k.vis));
        }
        if (c == null) {
            placedAt = now - 600;
            return;
        }
        Entity e = body(p);
        String anim = e != null ? Motion.current(e) : "cm_still";
        float yaw = e instanceof Mob m ? m.getYRot() : yawTo(c.center, p.position());
        if (stage >= 2) yaw = yawTo(c.center, p.getEyePosition());
        put(p, c.feet, yaw, anim.isEmpty() ? "cm_still" : anim, now);
        Dev.say(p, "Bạn mãi chưa thấy nó — nó dời tới chỗ bạn sắp nhìn: " + Dev.where(p, c.center));
    }

    private boolean put(ServerPlayer p, BlockPos feet, float yaw, String anim, long now) {
        discard(p.serverLevel());
        Entity e = Haunt.spawn(p, feet, p.getEyePosition());
        if (e == null) return false;
        e.getPersistentData().putString(Compat.TAG, "damned_day");
        body = e.getUUID();
        if (e instanceof Mob m) Motion.snapYaw(m, yaw);
        Motion.pose(e, anim, now);
        placedAt = now;
        placeDist = Math.sqrt(e.distanceToSqr(p));
        resp = null;
        leaned = false;
        noticed = false;
        perceive = 0;
        away = 0;
        stare = 0;
        hideDelay = 6 + p.getRandom().nextInt(28);
        return true;
    }

    private void vanish(ServerPlayer p, Entity e, long now) {
        e.discard();
        body = null;
    }

    private void steps(ServerPlayer p, Vec3 from, Entity to) {
        if (to == null) return;
        Vec3 dest = to.position();
        if (Math.min(from.distanceTo(p.position()), dest.distanceTo(p.position())) > 28) return;
        ServerLevel lvl = p.serverLevel();
        int n = 3 + p.getRandom().nextInt(3);
        for (int i = 1; i <= n; i++) {
            Vec3 at = from.lerp(dest, i / (double) (n + 1));
            Fx.later(lvl, i * (4 + p.getRandom().nextInt(3)), () -> Fx.sound(p, Compat.sound("apollyon:step_sound", "minecraft:block.grass.step"),
                SoundSource.HOSTILE, at, 0.32f, 0.8f + p.getRandom().nextFloat() * 0.1f));
        }
    }

    private static void candle(ServerLevel lvl, BlockPos at) {
        candle(lvl, at, true);
    }

    private static void candle(ServerLevel lvl, BlockPos at, boolean lit) {
        for (BlockPos q : new BlockPos[]{at, at.above(), at.below()}) {
            if (!lvl.getBlockState(q).canBeReplaced() || !lvl.getFluidState(q).isEmpty()) continue;
            if (!lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), Direction.UP)) continue;
            lvl.setBlock(q, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, lit), 3);
            return;
        }
    }

    private static float facingTrunk(ServerLevel lvl, BlockPos feet) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (lvl.getBlockState(feet.above().relative(d)).is(BlockTags.LOGS)) return d.toYRot();
        }
        return Float.NaN;
    }

    static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * (180f / Math.PI)) - 90f;
    }
}
