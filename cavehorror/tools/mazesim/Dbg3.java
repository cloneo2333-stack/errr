import java.util.HashMap;
import java.util.Map;
import vn.cavemind.maze.core.Brain;
import vn.cavemind.maze.core.Round;

public class Dbg3 {
    public static void main(String[] a) {
        String kind = a.length > 0 ? a[0] : "sneak";
        int games = a.length > 1 ? Integer.parseInt(a[1]) : 200;
        Map<String, Integer> before = new HashMap<>();
        Map<String, Integer> dist = new HashMap<>();
        Map<String, Integer> how = new HashMap<>();
        for (int i = 0; i < games; i++) {
            long seed = 1000L * i + kind.hashCode();
            Round rd = new Round(seed, Round.Mode.PLAY);
            Round.Runner rn = rd.add(1);
            Sim.Bot b = new Sim.Bot(rd, rn, kind, seed + 5);
            Brain.Mode pm = Brain.Mode.COUNT, chaseFrom = null;
            double chaseDist = 0;
            String why = "";
            while (rd.end == Round.End.NONE) {
                b.tick();
                rd.tick();
                Brain br = rd.brain;
                if (br.mode == Brain.Mode.CHASE && pm != Brain.Mode.CHASE) {
                    chaseFrom = pm;
                    chaseDist = Math.hypot(rn.x - br.x, rn.z - br.z);
                    why = rd.visibleToDamned(rn) ? (rn.speed < 0.02 ? "saw-still" : "saw-moving") : rd.looksAtDamned(rn) ? "looked" : "?";
                }
                pm = br.mode;
            }
            if (rd.end == Round.End.CAUGHT && chaseFrom != null) {
                before.merge(chaseFrom.name(), 1, Integer::sum);
                dist.merge(String.valueOf((int) chaseDist), 1, Integer::sum);
                how.merge(why, 1, Integer::sum);
            } else if (rd.end == Round.End.CAUGHT) how.merge("nochase:" + rd.brain.mode, 1, Integer::sum);
        }
        System.out.println("chase started from: " + before);
        System.out.println("distance at chase start: " + new java.util.TreeMap<>(dist));
        System.out.println("how seen: " + how);
    }
}
