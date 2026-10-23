import java.util.List;
import vn.cavemind.maze.core.Brain;
import vn.cavemind.maze.core.Grid;
import vn.cavemind.maze.core.Round;

public class Dbg {
    public static void main(String[] a) throws Exception {
        String kind = a.length > 0 ? a[0] : "corner";
        int games = a.length > 1 ? Integer.parseInt(a[1]) : 200;
        for (int i = 0; i < games; i++) {
            long seed = 1000L * i + kind.hashCode();
            Round rd = new Round(seed, Round.Mode.PLAY);
            Round.Runner rn = rd.add(1);
            Sim.Bot b = new Sim.Bot(rd, rn, kind, seed + 5);
            int sweeps = 0;
            Brain.Mode last = null;
            while (rd.end == Round.End.NONE) {
                b.tick();
                rd.tick();
                if (rd.brain.mode != last) {
                    last = rd.brain.mode;
                }
                long idle = rd.t - rd.brain.idleSince;
                if (idle == 400) {
                    Brain br = rd.brain;
                    System.out.printf("seed %d t=%d idle: mode=%s at=%d goal=%d pos=%.2f,%.2f y=%.1f why=%s peak=%.3f%n", seed, rd.t, br.mode, br.at, br.goal, br.x, br.z, br.y, br.why, br.peak());
                    System.out.printf("player %.3f,%.3f closet=%d crouch=%s vis=%s looks=%s walkClear=%s clear=%s dist=%.3f sight=%.1f%n", rn.x, rn.z, rn.closet, rn.crouch,
                        rd.visibleToDamned(rn), rd.looksAtDamned(rn), rd.g.walkClear(br.x, br.z, rn.x, rn.z), rd.g.clear(br.x, br.z, rn.x, rn.z, rd.closetOpen),
                        Math.hypot(rn.x - br.x, rn.z - br.z), rd.sightOf(rn));
                    java.lang.reflect.Field f = Brain.class.getDeclaredField("route");
                    f.setAccessible(true);
                    java.util.ArrayDeque<int[]> route = (java.util.ArrayDeque<int[]>) f.get(br);
                    int[] h = route.peek();
                    System.out.printf("route size %d head %s fitsHere=%s centerFits=%s%n", route.size(), h == null ? "-" : h[0] + "/" + h[1],
                        rd.g.fits(br.x, br.z), rd.g.fits(Grid.cx(br.at), Grid.cz(br.at)));
                    if (h != null) System.out.printf("walkClear->head %s, center->head %s, head center %.1f,%.1f%n", rd.g.walkClear(br.x, br.z, Grid.cx(h[0]), Grid.cz(h[0])),
                        rd.g.walkClear(Grid.cx(br.at), Grid.cz(br.at), Grid.cx(h[0]), Grid.cz(h[0])), Grid.cx(h[0]), Grid.cz(h[0]));
                    String[] rows = rd.g.ascii(br.x, br.z, List.of(new double[]{rn.x, rn.z})).split("\n");
                    for (int q = (int) Math.max(0, br.z - 6); q < Math.min(rows.length, br.z + 6); q++) System.out.println(rows[q]);
                    return;
                }
            }
        }
        System.out.println("no idle found");
    }
}
