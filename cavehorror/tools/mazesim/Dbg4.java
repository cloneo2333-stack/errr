import java.util.List;
import vn.cavemind.maze.core.Brain;
import vn.cavemind.maze.core.Round;

public class Dbg4 {
    public static void main(String[] a) {
        Sim.DEBUG = true;
        String kind = a.length > 0 ? a[0] : "greedy";
        int game = a.length > 1 ? Integer.parseInt(a[1]) : 0;
        long seed = 1000L * game + kind.hashCode();
        Round rd = new Round(seed, Round.Mode.PLAY);
        Round.Runner rn = rd.add(1);
        Sim.Bot b = new Sim.Bot(rd, rn, kind, seed + 5);
        Brain.Mode pm = null;
        long chaseAt = -1;
        while (rd.end == Round.End.NONE) {
            double bx = rn.x, bz = rn.z;
            int ps = b.path.size();
            b.tick();
            rd.tick();
            Brain br = rd.brain;
            if (br.mode == Brain.Mode.CHASE && pm != Brain.Mode.CHASE) {
                chaseAt = rd.t;
                System.out.println("---- chase at t=" + rd.t);
            }
            if (chaseAt >= 0 && (rd.t - chaseAt) % 4 == 0 && rd.t - chaseAt < 400) {
                System.out.printf("t=%d mode=%s dist=%.2f botMove=%.3f bot=(%.1f,%.1f) dam=(%.1f,%.1f) vis=%s clear=%s pathLen=%d%n", rd.t, br.mode,
                    Math.hypot(rn.x - br.x, rn.z - br.z), Math.hypot(rn.x - bx, rn.z - bz), rn.x, rn.z, br.x, br.z, rd.visibleToDamned(rn),
                    rd.g.clear(br.x, br.z, rn.x, rn.z, -1), b.path.size());
                StringBuilder sb = new StringBuilder();
                for (double[] q : b.path) sb.append(String.format("(%.1f,%.1f)", q[0], q[1]));
                System.out.println("   path " + sb + " sees=" + b.seesDamned() + " flee=" + b.fleeUntil + " crouch=" + rn.crouch + " goalToy=" + b.goalToy);
            }
            if (rd.t > 2560 && rd.t < 2610) System.out.printf("  t=%d bot (%.2f,%.2f) path %d->%d goalToy=%d flee=%d%n", rd.t, rn.x, rn.z, ps, b.path.size(), b.goalToy, b.fleeUntil);
            pm = br.mode;
        }
        System.out.println("end " + rd.end + " t=" + rd.t);
        System.out.println(rd.g.ascii(rd.brain.x, rd.brain.z, List.of(new double[]{rn.x, rn.z})));
    }
}
