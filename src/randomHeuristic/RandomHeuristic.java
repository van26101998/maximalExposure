package randomHeuristic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;

import data.SensorNetwork;
import model.*;

public class RandomHeuristic {

    // Input
    SensorNetwork net;

    // Output
    ArrayList<Location> path;
    float exposure;
    float currTime;

    // Temp
    Location currLoc;

    float deltaS;

    public RandomHeuristic() {
        this.net = new SensorNetwork();
        this.path = new ArrayList<Location>();
        this.currTime = 0;
        this.exposure = 0;
    }

    public boolean timeCondition(Location nextLoc) {
        int rowIndexOfDest = Math.round(this.net.dest.y / this.deltaS);
        int columnIndexOfDest = Math.round(this.net.dest.x / this.deltaS);
        double shortestTime = (Math.abs(nextLoc.x - columnIndexOfDest * this.deltaS)
                + Math.abs(nextLoc.y - rowIndexOfDest * this.deltaS) + this.deltaS) / this.net.maxSpeed;
        return shortestTime < (this.net.limitTime - this.currTime);
    }

    public Location randomLocation() {
        int columnIndex = Math.round(this.currLoc.x / this.deltaS);
        int rowIndex = Math.round(this.currLoc.y / this.deltaS);
        int newRowIndex, newColumnIndex;

        Random rand = new Random();

        do {
            newRowIndex = rowIndex;
            newColumnIndex = columnIndex;
            int random = rand.nextInt(4);
            if (random == 0) {
                newRowIndex = rowIndex + 1;
            } else if (random == 1) {
                newColumnIndex = columnIndex + 1;
            } else if (random == 2) {
                newRowIndex = rowIndex - 1;
            } else {
                newColumnIndex = columnIndex - 1;
            }
        } while (newColumnIndex > net.grid.K || newColumnIndex < 0 || newRowIndex > net.grid.L || newRowIndex < 0);

        return net.grid.vertices[newRowIndex][newColumnIndex];
    }

    public void computeShortestPath() { // find shortest path to dest
        int rowIndexOfCurrLoc = Math.round(this.currLoc.y / this.deltaS);
        int columnIndexOfCurrLoc = Math.round(this.currLoc.x / this.deltaS);
        int rowIndexOfDest = Math.round(this.net.dest.y / this.deltaS);
        int columnIndexOfDest = Math.round(this.net.dest.x / this.deltaS);

        int tempColumnIndex = columnIndexOfCurrLoc;
        int tempRowIndex = rowIndexOfCurrLoc;
        Location nextLoc;

        while (tempColumnIndex != columnIndexOfDest || tempRowIndex != rowIndexOfDest) {
            if (tempColumnIndex != columnIndexOfDest) {
                if (tempColumnIndex < columnIndexOfDest) {
                    tempColumnIndex++;
                } else {
                    tempColumnIndex--;
                }
                nextLoc = net.grid.vertices[tempRowIndex][tempColumnIndex];
                this.path.add(nextLoc);
                this.currTime += this.deltaS / this.net.maxSpeed;
                this.exposure += this.net.exposureAt(this.currLoc) * this.deltaS / this.net.maxSpeed;
                this.currLoc = nextLoc;
            }

            if (tempRowIndex != rowIndexOfDest) {
                if (tempRowIndex < rowIndexOfDest) {
                    tempRowIndex++;
                } else {
                    tempRowIndex--;
                }
                nextLoc = net.grid.vertices[tempRowIndex][tempColumnIndex];
                this.path.add(nextLoc);
                this.currTime += this.deltaS / this.net.maxSpeed;
                this.exposure += this.net.exposureAt(this.currLoc) * this.deltaS / this.net.maxSpeed;
                this.currLoc = nextLoc;
            }
        }
    }

    public static void saveToFile(String fileName, SensorNetwork net, ArrayList<Location> finalpath, float maxExposure)
            throws Exception {
        File newFile = new File(fileName);
        newFile.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(net.wOfField + " " + net.hOfField);
        writer.newLine();

        writer.write(net.numOfSensors + "");
        writer.newLine();

        for (Sensor sensor : net.listSensors) {
            writer.write(sensor.x + " " + sensor.y + " " + sensor.r);
            writer.newLine();
        }

        writer.write(net.maxSpeed + "");
        writer.newLine();

        writer.write(net.start.x + " " + net.start.y);
        writer.newLine();

        writer.write(net.dest.x + " " + net.dest.y);
        writer.newLine();

        writer.write(net.limitTime + "");
        writer.newLine();

        writer.write(net.maxE + "");
        writer.newLine();

        writer.write(maxExposure + "");
        writer.newLine();

        writer.write(finalpath.size() + "");
        writer.newLine();

        for (Location loc : finalpath) {
            writer.write(loc.x + " " + loc.y);
            writer.newLine();
        }

        writer.flush();
        writer.close();
        System.out.println("Completely saved!");
    }

    public static float[] randomAlgorithm(String dataFile) throws Exception {
        long startTime = System.currentTimeMillis();

        RandomHeuristic rh = new RandomHeuristic();

        SensorNetwork net = rh.net;
        net.initialFromFile(dataFile);
        net.maxSpeed = 5;
        net.maxE = 50;
        net.limitTime = 100;
        net.wOfField = 100;
        net.hOfField = 100;


        net.makeGrid(rh.deltaS = 0.5f);

        int rowIndexOfStart = Math.round(net.start.y / rh.deltaS);
        int columnIndexOfStart = Math.round(net.start.x / rh.deltaS);
        rh.currLoc = net.grid.vertices[rowIndexOfStart][columnIndexOfStart];
        rh.path.add(rh.currLoc);

        Location nextLoc = rh.randomLocation();
        // add random neighbors while intruder have enough time to go to the destination
        while (rh.timeCondition(nextLoc)) {
            rh.currTime += rh.deltaS / net.maxSpeed;
            rh.exposure += net.exposureAt(rh.currLoc) * rh.deltaS / net.maxSpeed;
            rh.currLoc = nextLoc;
            rh.path.add(nextLoc);
            nextLoc = rh.randomLocation();
        }

        // almost out of time
        rh.computeShortestPath();

        long endTime = System.currentTimeMillis();
        float runTime = (float) ((endTime - startTime));
        return new float[]{rh.exposure, runTime};
    }

    public static void main(String[] args) throws Exception {
        String inputFolder = "./input/";


        int[] listNumberSensors = {10, 20, 50, 100, 200};
        int numOfTestEachCase = 10;
        for (int numberSensors : listNumberSensors) {
            for (int i = 1; i <= numOfTestEachCase; i++) {
                String inputFile = inputFolder + numberSensors + "/data_" + numberSensors + "_" + i + ".txt";
                float[] result = randomAlgorithm(inputFile);
                float exposure = result[0];
                float runTime = result[1];
                System.out.printf("%20s \t %10f \t %10d \n", "data_" + numberSensors + "_" + i + ".txt", exposure, (int) (runTime));
            }
        }
    }
}
