package vn.cavemind.maze.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class Grid {
    public static final int N = 17, P = 4, S = N * P + 1;
    public static final byte WALL = 0, FLOOR = 1, CLOSET = 2, FENCE = 3, PILLAR = 4, NOOK = 5;
    public static final int WELL = 0, CLASS = 1, DORM = 2, STORE = 3, BATH = 4;
    public static final int WARDROBE = 0, LOCKER = 1, STALL = 2, CUPBOARD = 3;
    static final int[] DX = {1, 0, -1, 0}, DZ = {0, 1, 0, -1};

    public final byte[][] tile = new byte[S][S];
    public final int[][] light = new int[S][S];
    public final boolean[][] grate = new boolean[S][S];
    public final boolean[][] lamp = new boolean[S][S];
    public final boolean[][] open = new boolean[N * N][4];
    public final int[] room = new int[N * N];
    public final int[][] rooms = {{7, 7}, {1, 1}, {13, 1}, {1, 13}, {13, 13}};
    public final List<Vent> vents = new ArrayList<>();
    public final List<Closet> closets = new ArrayList<>();
    public final List<Nook> nooks = new ArrayList<>();
    public final double[][] toys = new double[7][2];
    public final int[] toyCloset = new int[7];
    public double[] chest, bell, spawn, damnedStart;
    public final long seed;
    final int[][] dist;

    public static final class Vent {
        public final int a, b;
        public final int ax, az, bx, bz;
        public final double len;

        Vent(int a, int b, int ax, int az, int bx, int bz) {
            this.a = a;
            this.b = b;
            this.ax = ax;
            this.az = az;
            this.bx = bx;
            this.bz = bz;
            this.len = Math.hypot(bx - ax, bz - az);
        }
    }

    public static final class Closet {
        public final int x, z, cell, face;
        public int kind;

        Closet(int x, int z, int cell, int face) {
            this.x = x;
            this.z = z;
            this.cell = cell;
            this.face = face;
        }
    }

    public static final class Nook {
        public final int x, z, cell, face;

        Nook(int x, int z, int cell, int face) {
            this.x = x;
            this.z = z;
            this.cell = cell;
            this.face = face;
        }
    }

    public Grid(long seed) {
        this.seed = seed;
        Random r = new Random(seed);
        for (byte[] row : tile) Arrays.fill(row, WALL);
        Arrays.fill(room, -1);
        for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) carveCell(i, j);
        carveTree(r);
        for (int k = 0; k < rooms.length; k++) carveRoom(k, rooms[k][0], rooms[k][1], r);
        braid(r, 0.35);
        dist = allPairs();
        fences(r);
        placeVents(r);
        placeClosets(r);
        placeNooks(r);
        placeToys(r);
        int wx = rooms[WELL][0] * P, wz = rooms[WELL][1] * P;
        chest = new double[]{wx + 6.5, wz + 5.5};
        bell = new double[]{wx + 8.5, wz + 3.5};
        spawn = new double[]{wx + 6.5, wz + 8.5};
        damnedStart = new double[]{wx + 6.5, wz + 3.5};
        lights();
    }

    public static int cell(int i, int j) {
        return i + j * N;
    }

    public static int ci(int c) {
        return c % N;
    }

    public static int cj(int c) {
        return c / N;
    }

    public static double cx(int c) {
        return ci(c) * P + 2.5;
    }

    public static double cz(int c) {
        return cj(c) * P + 2.5;
    }

    public static int cellAt(double x, double z) {
        int i = (int) Math.floor((x - 0.5) / P), j = (int) Math.floor((z - 0.5) / P);
        i = Math.max(0, Math.min(N - 1, i));
        j = Math.max(0, Math.min(N - 1, j));
        return cell(i, j);
    }

    void carveCell(int i, int j) {
        for (int x = i * P + 1; x <= i * P + 3; x++) for (int z = j * P + 1; z <= j * P + 3; z++) tile[x][z] = FLOOR;
    }

    void link(int c, int d) {
        int i = ci(c), j = cj(c), k;
        int ni = ci(d), nj = cj(d);
        if (ni == i + 1) k = 0;
        else if (nj == j + 1) k = 1;
        else if (ni == i - 1) k = 2;
        else k = 3;
        open[c][k] = true;
        open[d][(k + 2) % 4] = true;
        if (k == 0) for (int z = j * P + 1; z <= j * P + 3; z++) tile[(i + 1) * P][z] = FLOOR;
        else if (k == 2) for (int z = j * P + 1; z <= j * P + 3; z++) tile[i * P][z] = FLOOR;
        else if (k == 1) for (int x = i * P + 1; x <= i * P + 3; x++) tile[x][(j + 1) * P] = FLOOR;
        else for (int x = i * P + 1; x <= i * P + 3; x++) tile[x][j * P] = FLOOR;
    }

    int neighbor(int c, int k) {
        int ni = ci(c) + DX[k], nj = cj(c) + DZ[k];
        if (ni < 0 || nj < 0 || ni >= N || nj >= N) return -1;
        return cell(ni, nj);
    }

    void carveTree(Random r) {
        boolean[] seen = new boolean[N * N];
        ArrayDeque<Integer> st = new ArrayDeque<>();
        int start = r.nextInt(N * N);
        st.push(start);
        seen[start] = true;
        int[] order = {0, 1, 2, 3};
        while (!st.isEmpty()) {
            int c = st.peek();
            shuffle(order, r);
            boolean moved = false;
            for (int k : order) {
                int d = neighbor(c, k);
                if (d < 0 || seen[d]) continue;
                link(c, d);
                seen[d] = true;
                st.push(d);
                moved = true;
                break;
            }
            if (!moved) st.pop();
        }
    }

    void carveRoom(int id, int i0, int j0, Random r) {
        for (int i = i0; i < i0 + 3; i++) {
            for (int j = j0; j < j0 + 3; j++) {
                room[cell(i, j)] = id;
                if (i < i0 + 2) link(cell(i, j), cell(i + 1, j));
                if (j < j0 + 2) link(cell(i, j), cell(i, j + 1));
            }
        }
        for (int x = i0 * P + 1; x <= (i0 + 3) * P - 1; x++) for (int z = j0 * P + 1; z <= (j0 + 3) * P - 1; z++) tile[x][z] = FLOOR;
        List<int[]> rim = new ArrayList<>();
        int doors = 0;
        for (int i = i0; i < i0 + 3; i++) {
            for (int j = j0; j < j0 + 3; j++) {
                int c = cell(i, j);
                for (int k = 0; k < 4; k++) {
                    int d = neighbor(c, k);
                    if (d < 0 || room[d] == id) continue;
                    if (open[c][k]) doors++;
                    else rim.add(new int[]{c, d});
                }
            }
        }
        java.util.Collections.shuffle(rim, r);
        int want = id == WELL ? 4 : 2;
        for (int[] e : rim) {
            if (doors >= want) break;
            link(e[0], e[1]);
            doors++;
        }
    }

    int degree(int c) {
        int n = 0;
        for (int k = 0; k < 4; k++) if (open[c][k]) n++;
        return n;
    }

    void braid(Random r, double frac) {
        for (int c = 0; c < N * N; c++) {
            if (room[c] >= 0 || degree(c) != 1 || r.nextDouble() > frac) continue;
            List<Integer> opts = new ArrayList<>();
            for (int k = 0; k < 4; k++) {
                int d = neighbor(c, k);
                if (d >= 0 && !open[c][k]) opts.add(d);
            }
            if (!opts.isEmpty()) link(c, opts.get(r.nextInt(opts.size())));
        }
    }

    int[][] allPairs() {
        int[][] out = new int[N * N][];
        for (int c = 0; c < N * N; c++) out[c] = bfs(c);
        return out;
    }

    int[] bfs(int s) {
        int[] d = new int[N * N];
        Arrays.fill(d, Integer.MAX_VALUE);
        d[s] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(s);
        while (!q.isEmpty()) {
            int c = q.poll();
            for (int k = 0; k < 4; k++) {
                if (!open[c][k]) continue;
                int n = neighbor(c, k);
                if (d[n] == Integer.MAX_VALUE) {
                    d[n] = d[c] + 1;
                    q.add(n);
                }
            }
        }
        return d;
    }

    public int pathCells(int a, int b) {
        return dist[a][b];
    }

    void fences(Random r) {
        for (int c = 0; c < N * N; c++) {
            for (int k = 0; k < 2; k++) {
                int d = neighbor(c, k);
                if (d < 0 || open[c][k] || room[c] >= 0 && room[c] == room[d] || r.nextDouble() > 0.13) continue;
                int i = ci(c), j = cj(c);
                if (k == 0) tile[(i + 1) * P][j * P + 2] = FENCE;
                else tile[i * P + 2][(j + 1) * P] = FENCE;
            }
        }
    }

    void placeVents(Random r) {
        int made = 0;
        for (int tries = 0; tries < 6000 && made < 8; tries++) {
            int a = r.nextInt(N * N), b = r.nextInt(N * N);
            if (a == b || room[a] == WELL || room[b] == WELL) continue;
            double e = Math.hypot(ci(a) - ci(b), cj(a) - cj(b));
            if (e < 2.5 || e > 7.5 || dist[a][b] < 8 || dist[a][b] < e * 2.2) continue;
            boolean used = false;
            for (Vent v : vents) if (v.a == a || v.b == a || v.a == b || v.b == b) used = true;
            if (used) continue;
            int[] pa = corner(a, r), pb = corner(b, r);
            if (pa == null || pb == null) continue;
            grate[pa[0]][pa[1]] = true;
            grate[pb[0]][pb[1]] = true;
            vents.add(new Vent(a, b, pa[0], pa[1], pb[0], pb[1]));
            made++;
        }
    }

    int[] corner(int c, Random r) {
        int i = ci(c), j = cj(c);
        int[][] cs = {{i * P + 1, j * P + 1}, {i * P + 3, j * P + 1}, {i * P + 1, j * P + 3}, {i * P + 3, j * P + 3}};
        for (int t = 0; t < 4; t++) {
            int[] q = cs[(t + r.nextInt(4)) % 4];
            if (tile[q[0]][q[1]] == FLOOR && !grate[q[0]][q[1]]) return q;
        }
        return null;
    }

    void placeClosets(Random r) {
        int[] dorm = rooms[DORM];
        int bx = dorm[0] * P, bz = dorm[1] * P;
        int[][] spots = {{bx + 2, bz + 1, 1}, {bx + 6, bz + 1, 1}, {bx + 10, bz + 1, 1}, {bx + 2, bz + 11, 3}, {bx + 6, bz + 11, 3}, {bx + 10, bz + 11, 3}};
        int made = 0;
        for (int[] q : spots) {
            if (made >= 4) break;
            if (r.nextDouble() < 0.25) continue;
            if (closetAt(q[0], q[1], q[2], WARDROBE)) made++;
        }
        int cx = rooms[CLASS][0] * P, cz = rooms[CLASS][1] * P;
        made = 0;
        for (int[] q : new int[][]{{cx + 1, cz + 3, 0}, {cx + 1, cz + 7, 0}, {cx + 11, cz + 3, 2}, {cx + 11, cz + 9, 2}}) {
            if (made >= 3) break;
            if (closetAt(q[0], q[1], q[2], LOCKER)) made++;
        }
        int hx = rooms[BATH][0] * P, hz = rooms[BATH][1] * P;
        made = 0;
        for (int[] q : new int[][]{{hx + 3, hz + 1, 1}, {hx + 6, hz + 1, 1}, {hx + 9, hz + 1, 1}, {hx + 3, hz + 11, 3}, {hx + 9, hz + 11, 3}}) {
            if (made >= 3) break;
            if (closetAt(q[0], q[1], q[2], STALL)) made++;
        }
        List<Integer> ends = new ArrayList<>();
        for (int c = 0; c < N * N; c++) if (room[c] < 0 && degree(c) == 1) ends.add(c);
        java.util.Collections.shuffle(ends, r);
        int extra = 0;
        for (int c : ends) {
            if (extra >= 8) break;
            for (int k = 0; k < 4; k++) {
                if (open[c][k]) continue;
                int i = ci(c), j = cj(c);
                int x = i * P + 2, z = j * P + 2;
                int face = (k + 2) % 4;
                x += DX[k];
                z += DZ[k];
                if (grate[x][z] || tile[x][z] != FLOOR) continue;
                int px = DX[(k + 1) % 4], pz = DZ[(k + 1) % 4];
                if (grate[x + px][z + pz] || grate[x - px][z - pz]) continue;
                if (closetAt(x, z, face, r.nextDouble() < 0.5 ? WARDROBE : CUPBOARD)) {
                    extra++;
                    break;
                }
            }
        }
    }

    void placeNooks(Random r) {
        for (int id = 1; id < rooms.length; id++) {
            int bx = rooms[id][0] * P, bz = rooms[id][1] * P;
            int[][] corners = {{bx + 1, bz + 1, 1, 1}, {bx + 11, bz + 1, -1, 1}, {bx + 1, bz + 11, 1, -1}, {bx + 11, bz + 11, -1, -1}};
            int made = 0;
            for (int[] q : corners) {
                if (made >= 2 || r.nextDouble() < 0.3) continue;
                if (nookCorner(q[0], q[1], q[2], q[3], r)) made++;
            }
            List<int[]> wall = new ArrayList<>();
            for (int t = bx + 3; t <= bx + 9; t += 2) {
                wall.add(new int[]{t, bz + 1, 1});
                wall.add(new int[]{t, bz + 11, 3});
                wall.add(new int[]{bx + 1, t, 0});
                wall.add(new int[]{bx + 11, t, 2});
            }
            java.util.Collections.shuffle(wall, r);
            for (int[] q : wall) {
                if (made >= 2) break;
                if (nookWall(q[0], q[1], q[2])) made++;
            }
        }
        List<Integer> ends = new ArrayList<>();
        for (int c = 0; c < N * N; c++) if (room[c] < 0 && degree(c) == 1) ends.add(c);
        java.util.Collections.shuffle(ends, r);
        int made = 0;
        for (int c : ends) {
            if (made >= 6) break;
            boolean taken = false;
            for (Closet cl : closets) if (cl.cell == c) taken = true;
            if (taken) continue;
            int k = -1;
            for (int d = 0; d < 4; d++) if (open[c][d]) k = d;
            if (k < 0) continue;
            int x = ci(c) * P + 2 - DX[k], z = cj(c) * P + 2 - DZ[k];
            int px = DX[(k + 1) % 4], pz = DZ[(k + 1) % 4];
            if (tile[x][z] != FLOOR || grate[x][z] || grate[x + px][z + pz] || grate[x - px][z - pz]) continue;
            if (tile[x - DX[k]][z - DZ[k]] != WALL) continue;
            tile[x + px][z + pz] = PILLAR;
            tile[x - px][z - pz] = PILLAR;
            tile[x][z] = NOOK;
            nooks.add(new Nook(x, z, c, k));
            made++;
        }
    }

    boolean nookWall(int x, int z, int face) {
        int k = (face + 2) % 4;
        int px = DX[(k + 1) % 4], pz = DZ[(k + 1) % 4];
        if (tile[x][z] != FLOOR || grate[x][z] || tile[x + DX[k]][z + DZ[k]] != WALL) return false;
        if (tile[x + px][z + pz] != FLOOR || tile[x - px][z - pz] != FLOOR || grate[x + px][z + pz] || grate[x - px][z - pz]) return false;
        int fx = x + DX[face], fz = z + DZ[face];
        if (tile[fx][fz] != FLOOR || grate[fx][fz]) return false;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            byte t = tile[x + dx][z + dz];
            if (t == CLOSET || t == NOOK || t == PILLAR) return false;
        }
        tile[x + px][z + pz] = PILLAR;
        tile[x - px][z - pz] = PILLAR;
        tile[x][z] = NOOK;
        nooks.add(new Nook(x, z, cellAt(x + 0.5, z + 0.5), face));
        return true;
    }

    boolean nookCorner(int x, int z, int sx, int sz, Random r) {
        if (tile[x][z] != FLOOR || grate[x][z]) return false;
        if (tile[x - sx][z] != WALL || tile[x][z - sz] != WALL) return false;
        boolean alongX = r.nextBoolean();
        int px = alongX ? x + sx : x, pz = alongX ? z : z + sz;
        int fx = alongX ? x : x + sx, fz = alongX ? z + sz : z;
        if (tile[px][pz] != FLOOR || grate[px][pz] || tile[fx][fz] != FLOOR || grate[fx][fz]) return false;
        for (Closet c : closets) if (Math.abs(c.x - px) + Math.abs(c.z - pz) <= 1 || Math.abs(c.x - fx) + Math.abs(c.z - fz) <= 1) return false;
        int face = fx > x ? 0 : fz > z ? 1 : fx < x ? 2 : 3;
        tile[px][pz] = PILLAR;
        tile[x][z] = NOOK;
        nooks.add(new Nook(x, z, cellAt(x + 0.5, z + 0.5), face));
        return true;
    }

    boolean closetAt(int x, int z, int face, int kind) {
        int k = (face + 2) % 4;
        int px = DX[(k + 1) % 4], pz = DZ[(k + 1) % 4];
        if (tile[x][z] != FLOOR || tile[x + DX[k]][z + DZ[k]] == FLOOR) return false;
        if (grate[x][z] || grate[x + px][z + pz] || grate[x - px][z - pz]) return false;
        if (tile[x + px][z + pz] != FLOOR || tile[x - px][z - pz] != FLOOR) return false;
        int fx = x + DX[face], fz = z + DZ[face];
        if (tile[fx][fz] != FLOOR || grate[fx][fz]) return false;
        tile[x][z] = CLOSET;
        tile[x + px][z + pz] = PILLAR;
        tile[x - px][z - pz] = PILLAR;
        Closet c = new Closet(x, z, cellAt(x + 0.5, z + 0.5), face);
        c.kind = kind;
        closets.add(c);
        return true;
    }

    void placeToys(Random r) {
        Arrays.fill(toyCloset, -1);
        int well = cell(rooms[WELL][0] + 1, rooms[WELL][1] + 1);
        toys[0] = roomSpot(CLASS, r);
        toys[2] = roomSpot(DORM, r);
        toys[3] = roomSpot(STORE, r);
        List<Integer> ends = new ArrayList<>();
        for (int c = 0; c < N * N; c++) if (room[c] < 0 && degree(c) == 1) ends.add(c);
        ends.sort((a, b) -> Integer.compare(dist[well][b], dist[well][a]));
        int far = ends.isEmpty() ? farthest(well) : ends.get(0);
        toys[6] = center(far);
        int mid = ends.size() > 2 ? ends.get(1 + r.nextInt(Math.min(4, ends.size() - 1))) : farthestFrom(well, far);
        toys[1] = center(mid);
        Closet hide = null;
        for (Closet c : closets) if (room[c.cell] != DORM) hide = c;
        if (hide == null && !closets.isEmpty()) hide = closets.get(r.nextInt(closets.size()));
        if (hide != null) {
            toys[4] = new double[]{hide.x + 0.5, hide.z + 0.5};
            toyCloset[4] = closets.indexOf(hide);
        } else {
            toys[4] = center(farthestFrom(well, mid));
        }
        toys[5] = roomSpot(BATH, r);
    }

    double[] roomSpot(int id, Random r) {
        int bx = rooms[id][0] * P, bz = rooms[id][1] * P;
        for (int t = 0; t < 40; t++) {
            int x = bx + 3 + r.nextInt(7), z = bz + 3 + r.nextInt(7);
            if (tile[x][z] == FLOOR && !grate[x][z]) return new double[]{x + 0.5, z + 0.5};
        }
        return new double[]{bx + 6.5, bz + 6.5};
    }

    double[] center(int c) {
        return new double[]{cx(c), cz(c)};
    }

    int farthest(int from) {
        int best = from;
        for (int c = 0; c < N * N; c++) if (dist[from][c] != Integer.MAX_VALUE && dist[from][c] > dist[from][best]) best = c;
        return best;
    }

    int farthestFrom(int a, int b) {
        int best = a;
        int bs = -1;
        for (int c = 0; c < N * N; c++) {
            if (room[c] >= 0) continue;
            int s = Math.min(dist[a][c], dist[b][c]);
            if (s != Integer.MAX_VALUE && s > bs) {
                bs = s;
                best = c;
            }
        }
        return best;
    }

    void lights() {
        int[][] src = new int[S][S];
        int wx = rooms[WELL][0] * P, wz = rooms[WELL][1] * P;
        for (int[] q : new int[][]{{wx + 3, wz + 3}, {wx + 9, wz + 9}, {wx + 3, wz + 9}, {wx + 9, wz + 3}}) {
            if (tile[q[0]][q[1]] != FLOOR) continue;
            src[q[0]][q[1]] = 4;
            lamp[q[0]][q[1]] = true;
        }
        Random r = new Random(seed ^ 0x51ED);
        for (int id = 1; id < rooms.length; id++) {
            if (r.nextDouble() < 0.5) continue;
            int bx = rooms[id][0] * P, bz = rooms[id][1] * P;
            int x = bx + 3 + r.nextInt(7), z = bz + 3 + r.nextInt(7);
            if (tile[x][z] != FLOOR) continue;
            src[x][z] = 3;
            lamp[x][z] = true;
        }
        for (int c = 0; c < N * N; c++) {
            if (room[c] >= 0 || r.nextDouble() > 0.1) continue;
            int x = ci(c) * P + 2, z = cj(c) * P + 2;
            src[x][z] = Math.max(src[x][z], 2);
            lamp[x][z] = true;
        }
        for (int[] row : light) Arrays.fill(row, 0);
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = 0; x < S; x++) {
            for (int z = 0; z < S; z++) {
                if (src[x][z] > 0) {
                    light[x][z] = src[x][z];
                    q.add(new int[]{x, z});
                }
            }
        }
        while (!q.isEmpty()) {
            int[] c = q.poll();
            int l = light[c[0]][c[1]] - 1;
            if (l <= 0) continue;
            for (int k = 0; k < 4; k++) {
                int x = c[0] + DX[k], z = c[1] + DZ[k];
                if (x < 0 || z < 0 || x >= S || z >= S || !passable(x, z)) continue;
                if (light[x][z] < l) {
                    light[x][z] = l;
                    q.add(new int[]{x, z});
                }
            }
        }
        for (Closet c : closets) light[c.x][c.z] = 0;
        for (Nook n : nooks) light[n.x][n.z] = 0;
    }

    public int nookIndex(int x, int z) {
        for (int i = 0; i < nooks.size(); i++) {
            Nook n = nooks.get(i);
            if (n.x == x && n.z == z) return i;
        }
        return -1;
    }

    public boolean passable(int x, int z) {
        if (x < 0 || z < 0 || x >= S || z >= S) return false;
        return tile[x][z] == FLOOR;
    }

    public boolean walkable(double x, double z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        if (ix < 0 || iz < 0 || ix >= S || iz >= S) return false;
        byte t = tile[ix][iz];
        return t == FLOOR || t == CLOSET || t == NOOK;
    }

    public int lightAt(double x, double z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        if (ix < 0 || iz < 0 || ix >= S || iz >= S) return 0;
        return light[ix][iz];
    }

    public boolean clear(double x0, double z0, double x1, double z1, int closetOpen) {
        double dx = x1 - x0, dz = z1 - z0;
        double len = Math.hypot(dx, dz);
        if (len < 1e-6) return true;
        int steps = (int) Math.ceil(len * 4);
        int last = -1;
        for (int s = 1; s < steps; s++) {
            double t = s / (double) steps;
            int ix = (int) Math.floor(x0 + dx * t), iz = (int) Math.floor(z0 + dz * t);
            int key = ix * 4096 + iz;
            if (key == last) continue;
            last = key;
            if (ix < 0 || iz < 0 || ix >= S || iz >= S) return false;
            byte tt = tile[ix][iz];
            if (tt == WALL || tt == PILLAR) return false;
            if (tt == CLOSET) {
                int idx = closetIndex(ix, iz);
                if (idx != closetOpen) {
                    int ex = (int) Math.floor(x1), ez = (int) Math.floor(z1);
                    int sx = (int) Math.floor(x0), sz = (int) Math.floor(z0);
                    if (!(ix == ex && iz == ez) && !(ix == sx && iz == sz)) return false;
                }
            }
        }
        return true;
    }

    public boolean fits(double px, double pz) {
        for (double ox : new double[]{-0.28, 0.28}) {
            for (double oz : new double[]{-0.28, 0.28}) {
                int ix = (int) Math.floor(px + ox), iz = (int) Math.floor(pz + oz);
                if (ix < 0 || iz < 0 || ix >= S || iz >= S) return false;
                byte tt = tile[ix][iz];
                if (tt != FLOOR && tt != CLOSET && tt != NOOK) return false;
            }
        }
        return true;
    }

    public boolean walkClear(double x0, double z0, double x1, double z1) {
        double dx = x1 - x0, dz = z1 - z0;
        double len = Math.hypot(dx, dz);
        if (len < 1e-6) return true;
        int steps = (int) Math.ceil(len * 5);
        for (int s = 1; s <= steps; s++) {
            double t = s / (double) steps;
            double px = x0 + dx * t, pz = z0 + dz * t;
            for (double ox : new double[]{-0.28, 0.28}) {
                for (double oz : new double[]{-0.28, 0.28}) {
                    int ix = (int) Math.floor(px + ox), iz = (int) Math.floor(pz + oz);
                    if (ix < 0 || iz < 0 || ix >= S || iz >= S) return false;
                    byte tt = tile[ix][iz];
                    if (tt != FLOOR && tt != CLOSET && tt != NOOK) return false;
                }
            }
        }
        return true;
    }

    public int closetIndex(int x, int z) {
        for (int i = 0; i < closets.size(); i++) {
            Closet c = closets.get(i);
            if (c.x == x && c.z == z) return i;
        }
        return -1;
    }

    public List<int[]> edges(int c) {
        List<int[]> out = new ArrayList<>();
        for (int k = 0; k < 4; k++) if (open[c][k]) out.add(new int[]{neighbor(c, k), -1});
        for (int v = 0; v < vents.size(); v++) {
            Vent vt = vents.get(v);
            if (vt.a == c) out.add(new int[]{vt.b, v});
            else if (vt.b == c) out.add(new int[]{vt.a, v});
        }
        return out;
    }

    static void shuffle(int[] a, Random r) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    public String ascii(double dx, double dz, List<double[]> ps) {
        StringBuilder sb = new StringBuilder();
        for (int z = 0; z < S; z++) {
            for (int x = 0; x < S; x++) {
                char ch = switch (tile[x][z]) {
                    case WALL -> '#';
                    case FENCE -> '+';
                    case CLOSET -> 'C';
                    case PILLAR -> 'H';
                    case NOOK -> 'n';
                    default -> grate[x][z] ? 'v' : light[x][z] > 0 ? (char) ('0' + Math.min(9, light[x][z])) : '.';
                };
                if ((int) dx == x && (int) dz == z) ch = 'D';
                if (ps != null) for (double[] q : ps) if ((int) q[0] == x && (int) q[1] == z) ch = '@';
                sb.append(ch);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
