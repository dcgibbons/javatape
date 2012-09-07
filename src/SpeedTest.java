/* SpeedTest.java */

import java.io.*;
import java.security.*;

public class SpeedTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java SpeedTest infile tapepath");
            System.exit(1);
        }

        FileInputStream in = new FileInputStream(args[0]);
        BasicTapeDevice d = new BasicTapeDevice(args[1]);

        int bs = d.getBlockSize();
        int bufSize = 1024 * 1024;

        MessageDigest md = MessageDigest.getInstance("SHA");
        OutputStream bufout = 
                new FixedBufferedOutputStream(d.getOutputStream(), bs, bufSize);

        OutputStream out = new DigestOutputStream(bufout, md);

        System.out.print("Beginning copy of file to tape...");
        System.out.flush();
        long start = System.currentTimeMillis();
        StreamCopier.copy(in, out, bufSize);
        out.flush();
        long end = System.currentTimeMillis();
        System.out.println("done.");
        System.out.println(end-start + "ms elapsed time.");

        d.close();
    }
}
