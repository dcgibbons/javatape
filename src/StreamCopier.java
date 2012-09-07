/* StreamCopier.java */

import java.io.*;

public class StreamCopier {
    public static void copy(InputStream in, OutputStream out) 
            throws IOException {
        copy(in, out, 256, 0);
    }

    public static void copy(InputStream in, OutputStream out, int bufSize) 
            throws IOException {
        copy(in, out, bufSize, 0);
    }

    public static void copy(InputStream in, OutputStream out, int bufSize, 
            long limit) throws IOException {
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[bufSize];
                long totalWritten = 0;
                while (true) {
                    if (limit != 0 && totalWritten >= limit) {
                        break;
                    }
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    out.write(buffer, 0, bytesRead);
                    totalWritten += bytesRead;
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            copy(System.in, System.out);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
