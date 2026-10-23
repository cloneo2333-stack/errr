package vn.cavemind.maze.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public final class Brain {
    public enum Mode { COUNT, SEARCH, SWEEP, OPEN, AMBUSH, CHASE, LAST, CUT, STUN }

    public static double SEARCH_SPEED = 0.16, CHASE_SPEED = 0.262, LAST_SPEED = 0.21, VENT_SPEED = 0.22, LUNGE_SPEED = 0.36;
    public static double DIFFUSE = 0.18, OPEN_BASE = 0.45, OPEN_K = 6.0, PRIOR = 0.012, CURIOUS = 0.35, CLOSET_PRIOR = 5.0;
    public static double LOSE_TICKS = 12;

    final Round rd;
    final Grid g;
    final Random r;
    public double x, z, yaw, y;
    public Mode mode = Mode.COUNT;
    public long modeAt;
    public final double[] belief = new double[Grid.N * Grid.N];
    final double[] prior = new double[Grid.N * Grid.N];
    final long[] looked = new long[Grid.N * Grid.N];
    public int at;
    final ArrayDeque<int[]> route = new ArrayDeque<>();
    public int goal = -1;
    final ArrayDeque<Integer> tabu = new ArrayDeque<>();
    Round.Runner focus;
    long listenUntil, lungeUntil, lungeReady;
    boolean listening, heardInside;
    public int listens, lunges, waits;
    double lastX, lastZ, lastFx, lastFz;
    long lastSeen = -10000, holdUntil, stunUntil;
    int vent = -1;
    double ventT;
    boolean ventFwd, diving;
    double sweepYaw;
    int openIdx = -1;
    long openAt;
    boolean opening;
    double openX, openZ;
    public long idleSince, moved, progressAt;
    public int rescues;
    double px, pz;
    public int opens, chases, vents, sweeps;
    final List<Round.Noise> heard = new ArrayList<>();
    public String why = "đang đếm";
    double notice, gap1, gap2;
    long spotUntil, eagerUntil;
    public static int SPOT_TICKS = 8;
    int cutCell = -1;
    public int cuts;

    Brain(Round rd, Random r) {
        this.rd = rd;
        this.g = rd.g;
        this.r = r;
        x = g.damnedStart[0];
        z = g.damnedStart[1];
        at = Grid.cellAt(x, z);
        int well = Grid.cell(g.rooms[Grid.WELL][0] + 1, g.rooms[Grid.WELL][1] + 1);
        double sum = 0, ps = 0;
        for (int c = 0; c < belief.length; c++) {
            double w = g.room[c] == Grid.WELL ? 0.1 : 1.0;
            if (g.room[c] < 0 && g.edges(c).size() == 1) w *= 2.0;
            for (Grid.Closet cl : g.closets) if (cl.cell == c) w *= CLOSET_PRIOR;
            w *= 1.0 + Math.max(0, 3 - g.light[Grid.ci(c) * Grid.P + 2][Grid.cj(c) * Grid.P + 2]) * 0.15;
            prior[c] = w;
            ps += w;
            belief[c] = w;
            sum += w;
        }
        for (int c = 0; c < belief.length; c++) {
            belief[c] /= sum;
            prior[c] /= ps;
        }
        for (Grid.Closet cl : g.closets) hiding[cl.cell] = true;
        for (Grid.Nook nk : g.nooks) hiding[nk.cell] = true;
        yaw = 0;
        px = x;
        pz = z;
    }

    final boolean[] hiding = new boolean[Grid.N * Grid.N];

    double boredom() {
        long since = rd.t - Math.max(lastSeen, Round.COUNT);
        return Math.max(0, Math.min(0.5, since / 4000.0));
    }

    public double fov() {
        return mode == Mode.CHASE ? 160 : 140;
    }

    void set(Mode m, String why) {
        if (m != Mode.OPEN) listening = false;
        mode = m;
        modeAt = rd.t;
        this.why = why;
    }

    void tick() {
        heard.addAll(rd.fresh);
        if (rd.t % 2 == 0) senses();
        if (rd.t % 10 == 0) think();
        move();
        double mv = Math.hypot(x - px, z - pz);
        if (mv > 0.01 || mode == Mode.COUNT || mode == Mode.SWEEP || mode == Mode.OPEN || mode == Mode.AMBUSH || mode == Mode.STUN) idleSince = rd.t;
        moved += mv > 0.01 ? 1 : 0;
        if (mv > 0.01 || route.isEmpty() && vent < 0) progressAt = rd.t;
        px = x;
        pz = z;
    }

    void senses() {
        if (mode == Mode.COUNT || mode == Mode.STUN) return;
        Round.Runner best = null;
        double bd = 1e9;
        for (Round.Runner rn : rd.runners) {
            if (rn.out || rn.caught) continue;
            boolean see = rd.visibleToDamned(rn) || rd.looksAtDamned(rn);
            if (see && rd.hidden(rn) && mode != Mode.CHASE && r.nextDouble() > 0.14) see = false;
            if (!see) continue;
            double d = Math.hypot(rn.x - x, rn.z - z);
            if (d < bd) {
                bd = d;
                best = rn;
            }
        }
        if (best != null && mode != Mode.CHASE) {
            double d = Math.hypot(best.x - x, best.z - z);
            notice += d < 2.5 || rd.looksAtDamned(best) ? 3 : best.speed > 0.1 ? 1.5 : 1;
            if (notice < (mode == Mode.LAST ? 2 : 3)) best = null;
        } else if (best == null) {
            notice = Math.max(0, notice - 0.5);
        }
        if (best != null) {
            notice = 3;
            focus = best;
            lastX = best.x;
            lastZ = best.z;
            if (best.speed > 0.03) {
                lastFx = best.fx();
                lastFz = best.fz();
            }
            lastSeen = rd.t;
            collapse(Grid.cellAt(best.x, best.z));
            if (mode == Mode.CUT && Math.hypot(best.x - x, best.z - z) > 6) return;
            if (mode != Mode.CHASE) {
                chases++;
                if (mode != Mode.LAST) spotUntil = rd.t + SPOT_TICKS;
                yaw = yawTo(x, z, best.x, best.z);
                rd.events.add(new Round.Event("spot", x, z, best.id));
                set(Mode.CHASE, "thấy bạn");
                route.clear();
                vent = -1;
                if (y < 0) y = 0;
            }
        }
    }

    void watchdog() {
        boolean wants = !route.isEmpty() || vent >= 0 || mode == Mode.OPEN && !opening;
        if (!wants || rd.t - progressAt < 60) return;
        rescues++;
        if (vent >= 0) {
            Grid.Vent v = g.vents.get(vent);
            x = (ventFwd ? v.bx : v.ax) + 0.5;
            z = (ventFwd ? v.bz : v.az) + 0.5;
            at = ventFwd ? v.b : v.a;
        }
        vent = -1;
        diving = false;
        y = 0;
        if (!g.fits(x, z)) {
            at = Grid.cellAt(x, z);
            x = Grid.cx(at);
            z = Grid.cz(at);
        }
        at = Grid.cellAt(x, z);
        route.clear();
        openIdx = -1;
        opening = false;
        rd.closetOpen = -1;
        progressAt = rd.t;
        set(Mode.SEARCH, "gỡ kẹt");
        pickGoal();
    }

    boolean approach(double tx, double tz, double speed) {
        if (g.walkClear(x, z, tx, tz)) return toward(tx, tz, speed);
        double cx = Grid.cx(at), cz = Grid.cz(at);
        if (Math.hypot(cx - x, cz - z) > 0.05 && g.walkClear(x, z, cx, cz)) {
            toward(cx, cz, speed);
            return false;
        }
        return toward(tx, tz, speed);
    }

    void think() {
        watchdog();
        diffuse(DIFFUSE);
        if (mode != Mode.COUNT) for (int c = 0; c < belief.length; c++) belief[c] = belief[c] * (1 - PRIOR) + PRIOR * prior[c];
        for (Round.Noise n : heard) hear(n);
        heard.clear();
        if (mode != Mode.COUNT) negative();
        normalize();
        switch (mode) {
            case COUNT -> {
                if (rd.t - modeAt >= Round.COUNT) set(Mode.SEARCH, "đếm xong, đi tìm");
            }
            case SEARCH -> {
                if (route.isEmpty() && vent < 0) {
                    if (goal >= 0 && at == goal) {
                        sweeps++;
                        set(Mode.SWEEP, "tới ô nghi ngờ, quét góc");
                        sweepYaw = yaw;
                        holdUntil = rd.t + 24 + r.nextInt(18);
                    } else pickGoal();
                } else if (rd.t - modeAt > 600) {
                    route.clear();
                    pickGoal();
                    modeAt = rd.t;
                }
            }
            case SWEEP -> {
                if (rd.t >= holdUntil) {
                    if (!tryOpen()) afterSweep();
                }
            }
            case OPEN -> {
                if (!opening) {
                    if (Math.hypot(openX - x, openZ - z) < 0.05) {
                        Grid.Closet c = g.closets.get(openIdx);
                        yaw = yawTo(x, z, c.x + 0.5, c.z + 0.5);
                        if (!listening) {
                            listening = true;
                            heardInside = false;
                            listens++;
                            listenUntil = rd.t + 30 + r.nextInt(30);
                            why = "áp tai vào cửa tủ";
                            rd.events.add(new Round.Event("listen", c.x + 0.5, c.z + 0.5, openIdx));
                        } else if (rd.t >= listenUntil) {
                            listening = false;
                            boolean occupied = false;
                            for (Round.Runner rn : rd.runners) if (!rn.out && !rn.caught && rn.closet == openIdx) occupied = true;
                            double p = heardInside ? 1.0 : Math.min(0.9, OPEN_BASE + belief[c.cell] * OPEN_K + (occupied ? 0.1 : 0));
                            if (r.nextDouble() < p) {
                                opening = true;
                                openAt = rd.t;
                                rd.events.add(new Round.Event("open", c.x + 0.5, c.z + 0.5, openIdx));
                            } else {
                                rd.events.add(new Round.Event("pass", c.x + 0.5, c.z + 0.5, openIdx));
                                int idx = openIdx;
                                openIdx = -1;
                                belief[c.cell] *= 0.6;
                                if (occupied && r.nextDouble() < 0.55) {
                                    waits++;
                                    set(Mode.AMBUSH, "giả vờ bỏ đi, đứng chờ ngoài tủ");
                                    holdUntil = rd.t + 120 + r.nextInt(140);
                                    route.clear();
                                    rd.events.add(new Round.Event("wait", c.x + 0.5, c.z + 0.5, idx));
                                } else afterSweep();
                            }
                        }
                    } else if (rd.t - modeAt > 140) {
                        openIdx = -1;
                        listening = false;
                        afterSweep();
                    }
                } else if (rd.t - openAt >= 22) {
                    rd.closetOpen = -1;
                    int idx = openIdx;
                    openIdx = -1;
                    opening = false;
                    if (idx >= 0) {
                        Grid.Closet c = g.closets.get(idx);
                        belief[c.cell] *= 0.25;
                        rd.events.add(new Round.Event("shut", c.x + 0.5, c.z + 0.5, idx));
                        if (r.nextDouble() < 0.2) {
                            waits++;
                            set(Mode.AMBUSH, "đóng tủ rồi đứng im trong bóng tối");
                            holdUntil = rd.t + 100 + r.nextInt(120);
                            route.clear();
                            break;
                        }
                    }
                    afterSweep();
                } else if (rd.t - openAt >= 6) {
                    rd.closetOpen = openIdx;
                }
            }
            case AMBUSH -> {
                if (rd.t >= holdUntil || peak() > 0.3 && !why.startsWith("giả vờ") && !why.startsWith("đóng tủ")) set(Mode.SEARCH, "rời chỗ phục kích");
            }
            case CHASE -> {
                double gap = focus == null ? 0 : Math.hypot(focus.x - x, focus.z - z);
                boolean widening = gap > 7 && gap > gap1 + 0.2 && gap1 > gap2 + 0.2;
                gap2 = gap1;
                gap1 = gap;
                if (widening && focus != null && rd.t - lastSeen <= LOSE_TICKS && cutTo(focus)) {
                    gap1 = gap2 = 0;
                } else if (rd.t - lastSeen > LOSE_TICKS) {
                    set(Mode.LAST, "mất tầm nhìn, chạy tới chỗ thấy cuối");
                    route.clear();
                    seedAhead();
                    planTo(Grid.cellAt(lastX + lastFx * Grid.P * 2, lastZ + lastFz * Grid.P * 2), false);
                } else if (rd.t % 10 == 0) {
                    planTo(Grid.cellAt(focus.x, focus.z), false);
                }
            }
            case CUT -> {
                if (route.isEmpty() && vent < 0) {
                    set(Mode.AMBUSH, "chờ ở chỗ bạn sắp tới");
                    holdUntil = rd.t + 120 + r.nextInt(120);
                } else if (rd.t - modeAt > 500) {
                    route.clear();
                    set(Mode.SEARCH, "chặn đầu hụt");
                }
            }
            case LAST -> {
                if (route.isEmpty() && vent < 0) {
                    sweeps++;
                    set(Mode.SWEEP, "lục quanh chỗ mất dấu");
                    holdUntil = rd.t + 30 + r.nextInt(20);
                } else if (rd.t - modeAt > 300) {
                    route.clear();
                    set(Mode.SEARCH, "bỏ đuổi");
                }
            }
            case STUN -> {
                if (rd.t >= stunUntil) set(focus != null && rd.t - lastSeen < 40 ? Mode.CHASE : Mode.SEARCH, "hết choáng");
            }
        }
    }

    boolean cutTo(Round.Runner rn) {
        int best = -1;
        double bd = 1e9;
        int rc = Grid.cellAt(rn.x, rn.z);
        if (rn.toys >= 7 || rd.remainingToys() == 0) best = Grid.cellAt(g.chest[0], g.chest[1]);
        else {
            for (int i = 0; i < 7; i++) {
                if (rd.toyTaken[i]) continue;
                int tc = Grid.cellAt(g.toys[i][0], g.toys[i][1]);
                double d = g.pathCells(rc, tc);
                double ahead = (Grid.cx(tc) - rn.x) * rn.fx() + (Grid.cz(tc) - rn.z) * rn.fz();
                if (ahead < 0) d += 4;
                if (d < bd) {
                    bd = d;
                    best = tc;
                }
            }
        }
        if (best < 0 || best == at) return false;
        double[] mine = dijkstra(at, false);
        if (mine[best] >= g.pathCells(rc, best) + 2) return false;
        cuts++;
        cutCell = best;
        set(Mode.CUT, "bạn chạy nhanh hơn, nó đi đường tắt chặn đầu");
        planTo(best, true);
        return true;
    }

    void afterSweep() {
        if (rd.remainingToys() > 0 && peak() < 0.07 && r.nextDouble() < 0.35) {
            int junction = ambushCell();
            if (junction >= 0) {
                set(Mode.AMBUSH, "niềm tin quá loãng, đi phục kích");
                route.clear();
                planTo(junction, false);
                holdUntil = rd.t + 200 + r.nextInt(200);
                return;
            }
        }
        set(Mode.SEARCH, "chọn chỗ mới");
        pickGoal();
    }

    boolean tryOpen() {
        for (int i = 0; i < g.closets.size(); i++) {
            Grid.Closet c = g.closets.get(i);
            if (c.cell != at) continue;
            double p = OPEN_BASE + belief[c.cell] * OPEN_K + boredom();
            if (r.nextDouble() > Math.min(0.95, p)) continue;
            openIdx = i;
            opening = false;
            opens++;
            set(Mode.OPEN, "mở tủ");
            openX = c.x + 0.5 + Grid.DX[c.face] * 1.0;
            openZ = c.z + 0.5 + Grid.DZ[c.face] * 1.0;
            return true;
        }
        return false;
    }

    int ambushCell() {
        int toy = -1;
        double bd = 1e9;
        double mx = 0, mz = 0;
        for (int c = 0; c < belief.length; c++) {
            mx += belief[c] * Grid.cx(c);
            mz += belief[c] * Grid.cz(c);
        }
        for (int i = 0; i < 7; i++) {
            if (rd.toyTaken[i]) continue;
            double d = Math.hypot(g.toys[i][0] - mx, g.toys[i][1] - mz);
            if (d < bd) {
                bd = d;
                toy = i;
            }
        }
        if (toy < 0) return -1;
        int tc = Grid.cellAt(g.toys[toy][0], g.toys[toy][1]), mc = Grid.cellAt(mx, mz);
        int best = -1;
        double bs = -1e9;
        for (int c = 0; c < belief.length; c++) {
            if (g.edges(c).size() < 3 || g.dist[c][tc] == Integer.MAX_VALUE) continue;
            double s = -g.dist[c][tc] - Math.abs(g.dist[c][mc] - g.dist[tc][mc]) * 0.5 + r.nextDouble();
            if (s > bs) {
                bs = s;
                best = c;
            }
        }
        return best;
    }

    void hear(Round.Noise n) {
        double d = Math.hypot(n.x() - x, n.z() - z);
        if (listening && openIdx >= 0) {
            Grid.Closet c = g.closets.get(openIdx);
            if (Math.hypot(n.x() - c.x - 0.5, n.z() - c.z - 0.5) < 1.2) heardInside = true;
        }
        if (mode == Mode.AMBUSH && (why.startsWith("giả vờ") || why.startsWith("đóng tủ")) && d < 4) {
            for (Grid.Closet c : g.closets) {
                if (Math.hypot(n.x() - c.x - 0.5, n.z() - c.z - 0.5) >= 1.2 || Math.hypot(c.x + 0.5 - x, c.z + 0.5 - z) > 3) continue;
                int idx = g.closets.indexOf(c);
                openIdx = idx;
                opening = true;
                openAt = rd.t;
                listening = false;
                yaw = yawTo(x, z, c.x + 0.5, c.z + 0.5);
                set(Mode.OPEN, "nghe tiếng trong tủ, giật cửa");
                rd.events.add(new Round.Event("open", c.x + 0.5, c.z + 0.5, idx));
                return;
            }
        }
        if (d > n.r() * (y < 0 ? 0.7 : 1.0)) return;
        int c = Grid.cellAt(n.x(), n.z());
        double sigma = 0.5 + d / 16.0;
        double w = 0.25 + 0.6 * (1 - d / n.r());
        double sum = 0;
        double[] add = new double[belief.length];
        int ci = Grid.ci(c), cj = Grid.cj(c);
        for (int k = 0; k < belief.length; k++) {
            double dd = Math.pow(Grid.ci(k) - ci, 2) + Math.pow(Grid.cj(k) - cj, 2);
            add[k] = Math.exp(-dd / (2 * sigma * sigma));
            sum += add[k];
        }
        for (int k = 0; k < belief.length; k++) belief[k] = belief[k] * (1 - w) + w * add[k] / sum;
        if (w > 0.45) eagerUntil = rd.t + 60;
        if (mode == Mode.AMBUSH && w > 0.5) set(Mode.SEARCH, "nghe thấy gì đó");
        if (mode == Mode.SEARCH && w > 0.45 && goal >= 0 && belief[c] > belief[goal] * 1.5) {
            route.clear();
            pickGoal();
        }
        if (mode == Mode.SWEEP && w > 0.6) {
            set(Mode.SEARCH, "nghe tiếng, bỏ quét");
            pickGoal();
        }
    }

    void diffuse(double k) {
        double[] nb = new double[belief.length];
        for (int c = 0; c < belief.length; c++) {
            List<int[]> es = g.edges(c);
            if (es.isEmpty()) {
                nb[c] += belief[c];
                continue;
            }
            nb[c] += belief[c] * (1 - k);
            double share = belief[c] * k / es.size();
            for (int[] e : es) nb[e[0]] += share;
        }
        System.arraycopy(nb, 0, belief, 0, belief.length);
    }

    void negative() {
        if (y < -0.5) return;
        double range = 9;
        for (int c = 0; c < belief.length; c++) {
            double cx = Grid.cx(c), cz = Grid.cz(c);
            double d = Math.hypot(cx - x, cz - z);
            if (d > range) continue;
            if (d > 2.5) {
                double a = Math.atan2(cz - z, cx - x);
                double f = Math.atan2(Math.cos(Math.toRadians(yaw)), -Math.sin(Math.toRadians(yaw)));
                double diff = Math.abs(Math.atan2(Math.sin(a - f), Math.cos(a - f)));
                if (diff > Math.toRadians(fov() / 2)) continue;
            }
            if (!g.clear(x, z, cx, cz, rd.closetOpen)) continue;
            boolean closet = false;
            for (Grid.Closet cl : g.closets) if (cl.cell == c) closet = true;
            for (Grid.Nook nk : g.nooks) if (nk.cell == c) closet = true;
            belief[c] *= closet ? 0.45 : 0.05;
            looked[c] = rd.t;
        }
    }

    void collapse(int c) {
        Arrays.fill(belief, 0.0005);
        belief[c] = 1;
        normalize();
    }

    void seedAhead() {
        int c = Grid.cellAt(lastX + lastFx * Grid.P * 2, lastZ + lastFz * Grid.P * 2);
        double[] add = new double[belief.length];
        int ci = Grid.ci(c), cj = Grid.cj(c);
        double sum = 0;
        for (int k = 0; k < belief.length; k++) {
            double dd = Math.pow(Grid.ci(k) - ci, 2) + Math.pow(Grid.cj(k) - cj, 2);
            add[k] = Math.exp(-dd / (2 * 2.25));
            sum += add[k];
        }
        for (int k = 0; k < belief.length; k++) belief[k] = belief[k] * 0.3 + 0.7 * add[k] / sum;
    }

    void normalize() {
        double s = 0;
        for (double b : belief) s += b;
        if (s <= 0) {
            Arrays.fill(belief, 1.0 / belief.length);
            return;
        }
        for (int i = 0; i < belief.length; i++) belief[i] /= s;
    }

    public double peak() {
        double m = 0;
        for (double b : belief) m = Math.max(m, b);
        return m;
    }

    double loudest() {
        return 0;
    }

    void pickGoal() {
        if (vent >= 0) return;
        double[] cost = dijkstra(at, false);
        int[] best = {-1, -1, -1};
        double[] bs = {-1, -1, -1};
        for (int c = 0; c < belief.length; c++) {
            if (c == at || cost[c] == Double.MAX_VALUE) continue;
            if (tabu.contains(c) && belief[c] < 0.2) continue;
            double stale = Math.min(1.0, (rd.t - looked[c]) / 3000.0);
            double s = (belief[c] + CURIOUS * prior[c] * stale) / (1 + cost[c] / 5.0);
            if (hiding[c]) s *= 1 + 3 * boredom() * stale;
            for (int k = 0; k < 3; k++) {
                if (s > bs[k]) {
                    for (int m = 2; m > k; m--) {
                        bs[m] = bs[m - 1];
                        best[m] = best[m - 1];
                    }
                    bs[k] = s;
                    best[k] = c;
                    break;
                }
            }
        }
        double sum = 0;
        for (int k = 0; k < 3; k++) if (best[k] >= 0) sum += bs[k] * bs[k];
        int pick = best[0];
        if (sum > 0) {
            double u = r.nextDouble() * sum;
            for (int k = 0; k < 3; k++) {
                if (best[k] < 0) continue;
                u -= bs[k] * bs[k];
                if (u <= 0) {
                    pick = best[k];
                    break;
                }
            }
        }
        if (pick < 0) pick = r.nextInt(belief.length);
        goal = pick;
        planTo(goal, peak() > 0.25);
    }

    void planTo(int to, boolean flank) {
        if (vent >= 0) return;
        route.clear();
        if (to == at) return;
        double[] cost = new double[belief.length];
        int[] prev = new int[belief.length], via = new int[belief.length];
        Arrays.fill(cost, Double.MAX_VALUE);
        Arrays.fill(prev, -1);
        cost[at] = 0;
        PriorityQueue<double[]> q = new PriorityQueue<>((a, b) -> Double.compare(a[1], b[1]));
        q.add(new double[]{at, 0});
        while (!q.isEmpty()) {
            double[] cur = q.poll();
            int c = (int) cur[0];
            if (cur[1] > cost[c]) continue;
            if (c == to) break;
            for (int[] e : g.edges(c)) {
                int n = e[0];
                double len = e[1] >= 0 ? g.vents.get(e[1]).len / Grid.P * 0.8 + 0.6 : 1.0;
                if (flank) len += flankPenalty(n);
                double nc = cost[c] + len;
                if (nc < cost[n]) {
                    cost[n] = nc;
                    prev[n] = c;
                    via[n] = e[1];
                    q.add(new double[]{n, nc});
                }
            }
        }
        if (prev[to] < 0) return;
        ArrayDeque<int[]> path = new ArrayDeque<>();
        for (int c = to; c != at && c >= 0; c = prev[c]) path.addFirst(new int[]{c, via[c]});
        route.addAll(path);
    }

    double flankPenalty(int n) {
        double pen = 0;
        for (Round.Runner rn : rd.runners) {
            if (rn.out || rn.caught) continue;
            double dx = Grid.cx(n) - rn.x, dz = Grid.cz(n) - rn.z;
            double d = Math.hypot(dx, dz);
            if (d > 6 * Grid.P || d < 1) continue;
            double dot = (dx * rn.fx() + dz * rn.fz()) / d;
            if (dot > 0.6) pen += 2.5;
        }
        return pen;
    }

    double[] dijkstra(int from, boolean flank) {
        double[] cost = new double[belief.length];
        Arrays.fill(cost, Double.MAX_VALUE);
        cost[from] = 0;
        PriorityQueue<double[]> q = new PriorityQueue<>((a, b) -> Double.compare(a[1], b[1]));
        q.add(new double[]{from, 0});
        while (!q.isEmpty()) {
            double[] cur = q.poll();
            int c = (int) cur[0];
            if (cur[1] > cost[c]) continue;
            for (int[] e : g.edges(c)) {
                double len = e[1] >= 0 ? g.vents.get(e[1]).len / Grid.P * 0.8 + 0.6 : 1.0;
                double nc = cost[c] + len;
                if (nc < cost[e[0]]) {
                    cost[e[0]] = nc;
                    q.add(new double[]{e[0], nc});
                }
            }
        }
        return cost;
    }

    void move() {
        switch (mode) {
            case COUNT, OPEN, AMBUSH -> {
                if (mode == Mode.OPEN && !opening && openIdx >= 0) approach(openX, openZ, SEARCH_SPEED); else if (mode == Mode.AMBUSH && (!route.isEmpty() || vent >= 0)) step(SEARCH_SPEED);
                else if (mode == Mode.AMBUSH) yaw += Math.sin(rd.t * 0.03) * 1.5;
            }
            case STUN -> {}
            case SWEEP -> {
                yaw = (float) (sweepYaw + Math.sin((rd.t - modeAt) * 0.12) * 120);
            }
            case CHASE -> {
                if (rd.t < spotUntil) {
                    if (focus != null) yaw = yawTo(x, z, focus.x, focus.z);
                } else if (focus != null && vent < 0 && rd.t - lastSeen <= 2 && Math.hypot(focus.x - x, focus.z - z) < Grid.P * 1.6
                    && (g.walkClear(x, z, focus.x, focus.z) || Grid.cellAt(focus.x, focus.z) == at || route.isEmpty())) {
                    double d = Math.hypot(focus.x - x, focus.z - z);
                    if (d < 4.5 && d > 1.4 && rd.t >= lungeReady && g.walkClear(x, z, focus.x, focus.z)) {
                        lungeUntil = rd.t + 9;
                        lungeReady = rd.t + 110;
                        lunges++;
                        rd.events.add(new Round.Event("lunge", x, z, focus.id));
                    }
                    toward(focus.x, focus.z, rd.t < lungeUntil ? LUNGE_SPEED : CHASE_SPEED);
                    at = Grid.cellAt(x, z);
                } else step(CHASE_SPEED);
            }
            case LAST -> step(LAST_SPEED);
            case CUT -> step(CHASE_SPEED);
            default -> step(rd.t < eagerUntil ? LAST_SPEED : SEARCH_SPEED);
        }
    }

    void step(double speed) {
        if (vent >= 0) {
            Grid.Vent v = g.vents.get(vent);
            double sx = ventFwd ? v.ax + 0.5 : v.bx + 0.5, sz = ventFwd ? v.az + 0.5 : v.bz + 0.5;
            double ex = ventFwd ? v.bx + 0.5 : v.ax + 0.5, ez = ventFwd ? v.bz + 0.5 : v.az + 0.5;
            if (!diving) {
                if (approach(sx, sz, speed)) {
                    diving = true;
                    ventT = 0;
                    rd.events.add(new Round.Event("grate", sx, sz, 1));
                }
                return;
            }
            ventT += VENT_SPEED / Math.max(1, v.len);
            y = -1.4;
            double t = Math.min(1, ventT);
            x = sx + (ex - sx) * t;
            z = sz + (ez - sz) * t;
            yaw = yawTo(sx, sz, ex, ez);
            if (t >= 1) {
                y = 0;
                diving = false;
                vent = -1;
                at = ventFwd ? v.b : v.a;
                vents++;
                rd.events.add(new Round.Event("grate", ex, ez, 0));
                tabu(at);
                int[] head = route.peek();
                if (head != null && head[1] == done(v)) route.poll();
            }
            return;
        }
        int[] nx = route.peek();
        if (nx == null) return;
        if (nx[1] >= 0) {
            Grid.Vent v = g.vents.get(nx[1]);
            vent = nx[1];
            ventFwd = v.a == at;
            diving = false;
            return;
        }
        double tx = Grid.cx(nx[0]), tz = Grid.cz(nx[0]);
        if (approach(tx, tz, speed)) {
            at = nx[0];
            tabu(at);
            route.poll();
        }
    }

    int done(Grid.Vent v) {
        return g.vents.indexOf(v);
    }

    void tabu(int c) {
        tabu.remove(c);
        tabu.addLast(c);
        while (tabu.size() > 12) tabu.removeFirst();
    }

    boolean toward(double tx, double tz, double speed) {
        double dx = tx - x, dz = tz - z;
        double d = Math.hypot(dx, dz);
        if (d < 1e-6) return true;
        yaw = yawTo(x, z, tx, tz);
        if (d <= speed && g.fits(tx, tz)) {
            x = tx;
            z = tz;
            return true;
        }
        double mx = dx / d * Math.min(speed, d), mz = dz / d * Math.min(speed, d);
        if (g.fits(x + mx, z + mz)) {
            x += mx;
            z += mz;
        } else if (Math.abs(mx) > 1e-4 && g.fits(x + Math.signum(mx) * Math.min(speed, Math.abs(dx)), z)) {
            x += Math.signum(mx) * Math.min(speed, Math.abs(dx));
        } else if (Math.abs(mz) > 1e-4 && g.fits(x, z + Math.signum(mz) * Math.min(speed, Math.abs(dz)))) {
            z += Math.signum(mz) * Math.min(speed, Math.abs(dz));
        } else {
            return d < 0.6;
        }
        return false;
    }

    static double yawTo(double x0, double z0, double x1, double z1) {
        return Math.toDegrees(Math.atan2(z1 - z0, x1 - x0)) - 90;
    }

    public int goalCloset() {
        return mode == Mode.OPEN ? openIdx : -1;
    }

    public int focusId() {
        return focus == null ? -1 : focus.id;
    }

    public boolean listening() {
        return mode == Mode.OPEN && listening;
    }

    public void stun(long ticks) {
        listening = false;
        stunUntil = rd.t + ticks;
        set(Mode.STUN, "bị đánh, rít lên");
    }
}
