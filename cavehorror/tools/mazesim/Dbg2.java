import vn.cavemind.maze.core.Brain;
import vn.cavemind.maze.core.Grid;
import vn.cavemind.maze.core.Round;

public class Dbg2 {
    public static void main(String[] a) {
        int games = a.length > 0 ? Integer.parseInt(a[0]) : 20;
        for (int i = 0; i < games; i++) {
            long seed = 1000L * i + "closet".hashCode();
            Round rd = new Round(seed, Round.Mode.PLAY);
            Round.Runner rn = rd.add(1);
            Sim.Bot b = new Sim.Bot(rd, rn, "closet", seed + 5);
            int visits = 0, sweepsThere = 0, goals = 0;
            int lastAt = -1, lastGoal = -1;
            Brain.Mode lastMode = null;
            double maxBelief = 0;
            while (rd.end == Round.End.NONE) {
                b.tick();
                rd.tick();
                if (rn.closet < 0) continue;
                int cc = rd.g.closets.get(rn.closet).cell;
                Brain br = rd.brain;
                if (br.at == cc && lastAt != cc) visits++;
                if (br.goal == cc && lastGoal != cc) goals++;
                if (br.mode == Brain.Mode.SWEEP && lastMode != Brain.Mode.SWEEP && br.at == cc) sweepsThere++;
                maxBelief = Math.max(maxBelief, br.belief[cc]);
                lastAt = br.at;
                lastGoal = br.goal;
                lastMode = br.mode;
            }
            int cc = rn.closet >= 0 ? rd.g.closets.get(rn.closet).cell : -1;
            System.out.printf("game %d end=%s t=%d closetCell=%d room=%d visits=%d goals=%d sweepsThere=%d maxBelief=%.3f opens=%d sweeps=%d%n", i, rd.end, rd.t, cc,
                cc >= 0 ? rd.g.room[cc] : -9, visits, goals, sweepsThere, maxBelief, rd.brain.opens, rd.brain.sweeps);
        }
    }
}
