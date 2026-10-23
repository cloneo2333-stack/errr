import vn.cavemind.maze.core.*;
public class Show { public static void main(String[] a){ Grid g=new Grid(Long.parseLong(a[0])); System.out.print(g.ascii(-1,-1,null)); int[] k=new int[4]; for(Grid.Closet c:g.closets)k[c.kind]++; System.out.println("closets "+g.closets.size()+" kinds "+java.util.Arrays.toString(k)+" nooks "+g.nooks.size()+" vents "+g.vents.size()); } }
