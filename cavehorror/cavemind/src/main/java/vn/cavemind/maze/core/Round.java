package vn.cavemind.maze.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Round {
    public enum Mode { PLAY, PUNISH }

    public enum End { NONE, CAUGHT, WIN_TOYS, WIN_TIME, AFK, LEFT }

    public static final int TIME = 12000, COUNT = 48 * 20;
    public static final int N_STEP = 0, N_SPRINT = 1, N_CROUCH = 2, N_JUMP = 3, N_DOOR = 4, N_TOY = 5, N_VOICE = 6, N_BELL = 7, N_HIT = 8;

    public static final class Runner {
        public final int id;
        public double x, z, yaw;
        public boolean crouch, sprint, light, caught, out;
        public int closet = -1, nook = -1;
        public boolean doorClosed = true;
        public double speed;
        public int toys;
        public int still;
        public int stare;
        public double stamina = 1;
        public boolean exhausted, holding;
        int breathAt;

        public Runner(int id) {
            this.id = id;
        }

        public double fx() {
            return -Math.sin(Math.toRadians(yaw));
        }

        public double fz() {
            return Math.cos(Math.toRadians(yaw));
        }
    }

    public record Noise(double x, double z, double r, int kind, int who, long t) {}

    public record Event(String kind, double x, double z, int a) {}

    public final Grid g;
    public final Mode mode;
    public final List<Runner> runners = new ArrayList<>();
    public final Brain brain;
    public final boolean[] toyTaken = new boolean[7];
    public final int[] toyBy = new int[7];
    public int delivered;
    public long t;
    public End end = End.NONE;
    public int caughtId = -1;
    final List<Noise> fresh = new ArrayList<>();
    public final List<Event> events = new ArrayList<>();
    public int closetOpen = -1;

    public Round(long seed, Mode mode) {
        this.g = new Grid(seed);
        this.mode = mode;
        this.brain = new Brain(this, new Random(seed * 31 + 7));
        java.util.Arrays.fill(toyBy, -1);
    }

    public Runner add(int id) {
        Runner r = new Runner(id);
        r.x = g.spawn[0];
        r.z = g.spawn[1];
        runners.add(r);
        return r;
    }

    public Runner runner(int id) {
        for (Runner r : runners) if (r.id == id) return r;
        return null;
    }

    public void noise(double x, double z, double r, int kind, int who) {
        fresh.add(new Noise(x, z, r, kind, who, t));
    }

    public boolean takeToy(Runner rn, int i) {
        if (toyTaken[i]) return false;
        toyTaken[i] = true;
        toyBy[i] = rn.id;
        rn.toys++;
        noise(rn.x, rn.z, 10, N_TOY, rn.id);
        events.add(new Event("toy", g.toys[i][0], g.toys[i][1], i));
        return true;
    }

    public boolean deliver(int count) {
        delivered = Math.max(delivered, count);
        return delivered >= 7;
    }

    public void bell(Runner rn) {
        noise(g.bell[0], g.bell[1], 48, N_BELL, rn == null ? -1 : rn.id);
        if (delivered >= 7 && end == End.NONE) end = End.WIN_TOYS;
    }

    public int remainingToys() {
        int n = 0;
        for (boolean b : toyTaken) if (!b) n++;
        return n;
    }

    public void tick() {
        if (end != End.NONE) return;
        t++;
        gazeTick();
        for (Runner r : runners) if (!r.out && !r.caught) breath(r);
        brain.tick();
        fresh.clear();
        for (Runner r : runners) {
            if (r.out || r.caught) continue;
            if (catches(r)) {
                r.caught = true;
                caughtId = r.id;
                end = End.CAUGHT;
                events.add(new Event("caught", r.x, r.z, r.id));
                return;
            }
        }
        boolean anyIn = false;
        for (Runner r : runners) if (!r.out && !r.caught) anyIn = true;
        if (!anyIn) {
            end = End.LEFT;
            return;
        }
        if (t >= TIME) end = End.WIN_TIME;
    }

    boolean catches(Runner r) {
        if (brain.y < -0.5 || brain.mode == Brain.Mode.COUNT || brain.mode == Brain.Mode.STUN) return false;
        double d = Math.hypot(r.x - brain.x, r.z - brain.z);
        double lim = r.closet >= 0 && closetOpen == r.closet ? 1.35 : 1.1;
        if (d > lim) return false;
        if (r.closet >= 0 && r.doorClosed && closetOpen != r.closet) return false;
        if (hidden(r) && !(brain.mode == Brain.Mode.CHASE && brain.focus == r)) return false;
        return g.clear(brain.x, brain.z, r.x, r.z, closetOpen);
    }

    public boolean visibleToDamned(Runner r) {
        if (r.out || r.caught || brain.y < -0.5) return false;
        if (r.closet >= 0 && r.doorClosed && closetOpen != r.closet) return false;
        double d = Math.hypot(r.x - brain.x, r.z - brain.z);
        double range = sightOf(r);
        if (d > range) return false;
        if (hidden(r)) {
            double a = Math.atan2(r.z - brain.z, r.x - brain.x);
            double f = Math.atan2(Math.cos(Math.toRadians(brain.yaw)), -Math.sin(Math.toRadians(brain.yaw)));
            if (Math.abs(Math.atan2(Math.sin(a - f), Math.cos(a - f))) > Math.toRadians(35)) return false;
        } else if (d > 2.5) {
            double a = Math.atan2(r.z - brain.z, r.x - brain.x);
            double f = Math.atan2(Math.cos(Math.toRadians(brain.yaw)), -Math.sin(Math.toRadians(brain.yaw)));
            double diff = Math.abs(Math.atan2(Math.sin(a - f), Math.cos(a - f)));
            if (diff > Math.toRadians(brain.fov() / 2)) return false;
        }
        return g.clear(brain.x, brain.z, r.x, r.z, closetOpen);
    }

    public boolean tucked(Runner r) {
        return r.closet >= 0 && r.doorClosed && closetOpen != r.closet || r.nook >= 0;
    }

    void breath(Runner r) {
        boolean near = brain.y >= 0 && brain.mode != Brain.Mode.COUNT && Math.hypot(r.x - brain.x, r.z - brain.z) < 4.5;
        r.holding = false;
        if (r.sprint && r.speed > 0.1 && !r.exhausted) {
            r.stamina -= 1.0 / 130;
            if (r.stamina <= 0) {
                r.stamina = 0;
                r.exhausted = true;
                noise(r.x, r.z, 10, N_VOICE, r.id);
                events.add(new Event("gasp", r.x, r.z, r.id));
            }
            return;
        }
        if (tucked(r) && near) {
            if (r.crouch) {
                r.holding = true;
                r.stamina -= 1.0 / 80;
                if (r.stamina <= 0) {
                    r.stamina = 0.25;
                    r.exhausted = true;
                    noise(r.x, r.z, 8, N_VOICE, r.id);
                    events.add(new Event("gasp", r.x, r.z, r.id));
                }
            } else if (t - r.breathAt >= 30) {
                r.breathAt = (int) t;
                noise(r.x, r.z, 2.5, N_VOICE, r.id);
            }
            return;
        }
        r.stamina = Math.min(1, r.stamina + (r.crouch || r.speed < 0.02 ? 1.0 / 160 : 1.0 / 220));
        if (r.exhausted && r.stamina >= 0.35) r.exhausted = false;
    }

    public boolean hidden(Runner r) {
        return r.nook >= 0 && r.crouch && r.speed < 0.04 && !r.light;
    }

    public double sightOf(Runner r) {
        int l = g.lightAt(r.x, r.z);
        double s = l <= 1 ? 7 : l <= 3 ? 9 : 12;
        if (r.light) s = Math.max(16, s * 2);
        else if (hidden(r)) s = 1.6;
        else if (r.crouch && r.speed < 0.02) s *= 0.55;
        else if (r.crouch) s *= 0.8;
        return s;
    }

    public boolean gazes(Runner r) {
        if (r.out || r.caught || brain.y < -0.5) return false;
        double dx = brain.x - r.x, dz = brain.z - r.z;
        double d = Math.hypot(dx, dz);
        double see = g.lightAt(brain.x, brain.z) >= 4 || r.light ? 18 : 10;
        if (d > see || d < 0.5) return false;
        double dot = (dx * r.fx() + dz * r.fz()) / d;
        if (dot < Math.cos(Math.toRadians(11))) return false;
        if (r.closet >= 0 && r.doorClosed) return false;
        return g.clear(r.x, r.z, brain.x, brain.z, closetOpen);
    }

    public boolean looksAtDamned(Runner r) {
        return r.stare >= 15;
    }

    void gazeTick() {
        for (Runner r : runners) r.stare = gazes(r) ? r.stare + 1 : 0;
    }
}
