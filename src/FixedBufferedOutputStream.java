/* FixedBufferedOutputStream.java */

import java.io.*;

public class FixedBufferedOutputStream extends FilterOutputStream {
    protected int recordSize;
    protected byte buf[];
    protected int count;

    public FixedBufferedOutputStream(OutputStream out, int rs, int size) {
        super(out);

        if (rs <= 0) {
            throw new IllegalArgumentException("Record size <= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        if (rs != 0 && (size % rs) != 0) {
            throw new IllegalArgumentException("Buffer size not a multiple of record size");
        }

        recordSize = rs;
        buf = new byte[size];
    }

    public synchronized void write(byte b[], int off, int len) 
            throws IOException {
        do {
            if (count == buf.length) {
                flushBuffer();
            }

            int n = Math.min(len, buf.length - count);
            System.arraycopy(b, off, buf, count, n);
            len -= n;
            off += n;
            count += n;
        } while (len > 0);
    }

    private void flushBuffer() throws IOException {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }

    public synchronized void flush() throws IOException {
        if (count < buf.length) {
            while ((count % recordSize) != 0) {
                buf[count++] = 0;
            }
        }

        flushBuffer();
        out.flush();
    }

    public synchronized void write(int b) throws IOException {
        if (count == buf.length) {
            flushBuffer();
        }
        buf[count++] = (byte)b;
    }
}
