/* TestBasicTapeDevice.java */

import java.io.*;

public class TestBasicTapeDevice {
    private final static String usage = "Usage: java TestBasicTapeDevice path";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println(usage);
            System.exit(1);
        }
        String pathName = args[0];

        /* write a 5MB random file to the tape */
        BasicTapeDevice d = new BasicTapeDevice(pathName);

        InputStream in = new RandomInputStream();
        OutputStream out = d.getOutputStream();
        int bufSize = 64 * 1024;
        int limit = 5 * 1024 * 1024;

        System.out.print("Writing file...");
        System.out.flush();
        StreamCopier.copy(in, out, bufSize, limit);
        System.out.println("done.");

        System.out.print("Rewinding device...");
        System.out.flush();
        d.rewind();
        System.out.println("done.");

        /* hopefully they specified an auto-rewinding tape */
        in = d.getInputStream();
        out = new NullOutputStream();
        bufSize = 64 * 1024;

        System.out.print("Reading file...");
        System.out.flush();
        StreamCopier.copy(in, out, bufSize);
        System.out.println("done.");

        System.out.print("Closing device...");
        System.out.flush();
        d.close();
        System.out.println("done!");
    }
}
