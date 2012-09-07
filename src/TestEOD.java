/* TestEOD.java */

import java.io.*;

public class TestEOD {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java TestEOD <path to device>");
            System.exit(1);
        }

        BasicTapeDevice d = new BasicTapeDevice(args[0]);

        System.out.print("Rewinding...");
        System.out.flush();
        d.rewind();
        System.out.println("done!");

        System.out.print("Spacing to end of data...");
        System.out.flush();
        d.spaceEOD();
        System.out.println("done!");
    }
}
