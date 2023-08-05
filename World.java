
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Scanner;

/** Simulation of the robots in their world */

public class World {

    public static final int SIZE = 12;

    protected Set<Point> availableFuel = Collections.newSetFromMap(new ConcurrentHashMap<Point, Boolean>());
    private Random rand = new Random();
    protected Robot[] robots;

    public World() {
        robots = new Robot[] { null, new Robot(this, 0, 0, "red", false), new Robot(this, SIZE - 1, SIZE - 1, "blue", false) };
    }

    /**
     * This world can no longer be used after this call.
     */
    public void reset() {
        availableFuel.clear();
        for (int i = 1; i <= 2; i++) {
            robots[i].updatePending();
            robots[i].cancel();
        }
    }

    public Set<Point> getAvailableFuel() {
        return availableFuel;
    }

    public void updateWorld() {
        addFuel(false);
        for (int i = 1; i <= 2; i++) {
            robots[i].updatePending();
        }
    }

    public void loadRobotProgram(int id, File code) {
        try{
            Scanner scanner = new Scanner(code);
            ProgramNode prog = new Parser().parse(scanner);
            if (prog == null) {
                System.out.println("Robot " + id + " was given an empty program and ignored it.");
            }
            else {
                System.out.println("Robot " + id + " now has program: ");
                System.out.println(prog);
                robots[id].setProgram(prog);
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("Robot program source file not found");
        }
        catch (ParserFailureException e) {
            System.out.println("Parser error:");
            System.out.println(e.getMessage());
        }
    }

    public void start() {
        // add some initial fuel
        if (availableFuel.isEmpty()) {
            addFuel(true);
            addFuel(true);
        }
        new Thread(()-> {
                try {robots[1].run();}
                catch (RobotInterruptedException e) {}
                robots[1].setFinished(true);
        }).start();
        
        new Thread(() -> {
                try {
                    robots[2].run();
                } catch (RobotInterruptedException e) {
                }
                robots[2].setFinished(true);
        }).start();
    }

    public Robot getRobot(int id) {
        if (id <= 0 || id > robots.length) {
            return null;
        }
        return robots[id];
    }

    /** Returns the opponent robot of the argument */
    public Robot getOtherRobot(Robot robot) {
        if (robot == robots[2])
            return robots[1];
        if (robot == robots[1])
            return robots[2];
        return null;
    }

    private void addFuel(boolean definitely) {
        if (definitely || rand.nextDouble() < 0.2) {
            int x = rand.nextInt(12);
            int y = rand.nextInt(12);
            Point fuel = new Point(x, y);
            availableFuel.add(fuel);
        }
    }
    
}
