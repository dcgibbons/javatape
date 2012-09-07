/* BasicTapeDevice.java */

import java.io.*;

public class BasicTapeDevice {
    private FileDescriptor fd;
    private InputStream in;
    private OutputStream out;
    private boolean eof;
    private boolean eom;
    private boolean ignoreEOM;

    public BasicTapeDevice(String pathName) throws IOException {
        fd = new FileDescriptor();
        tapeOpen(pathName);
        in = new TapeInputStream();
        out = new TapeOutputStream();
        eof = false;
        eom = false;
        ignoreEOM = false;
    }

    public synchronized void close() throws IOException {
        if (fd != null) {
            try {
                if (fd.valid()) {
                    tapeClose();
                }
            } finally {
                fd = null;
            }
        }
    }

    public InputStream getInputStream() throws IOException {
        ensureOpen();
        return in;
    }

    public OutputStream getOutputStream() throws IOException {
        ensureOpen();
        return out;
    }

    public int getBlockSize() throws IOException {
        ensureOpen();
        return tapeGetBlockSize();
    }

    public void setBlockSize(int bs) throws IOException {
        ensureOpen();
        tapeSetBlockSize(bs);
    }

    public void rewind() throws IOException {
        ensureOpen();
        tapeRewind();
    }

    public void spaceEOD() throws IOException {
        ensureOpen();
        tapeSpaceEOD();
    }

    public void clearEOF() throws IOException {
        ensureOpen();
        if (eof) {
            eof = false;
            /* assume that the file mark has already been skipped */
        } else { 
            throw new IOException("not at end of file");
        }
    }

    public void clearEOM() throws IOException {
        ensureOpen();
        if (eom) {
            ignoreEOM = true;
        } else {
            throw new IOException("not at logical end of media");
        }
    }

    class TapeInputStream extends InputStream {
        private byte[] temp = new byte[1];

        public int read() throws IOException {
            int n = read(temp, 0, 1);
            if (n <= 0) {
                return -1;
            }

            return temp[0] & 0xff;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || off+len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            if (eof) {
                return -1;
            }

            ensureOpen();

            int n = tapeRead(b, off, len);
            if (n <= 0) {
                return -1;
            }

            return n;
        }

        public long skip(long numbytes) throws IOException {
            return 0;
        }

        public void close() throws IOException {
            BasicTapeDevice.this.close();
        }
    }

    class TapeOutputStream extends OutputStream {
        private byte[] temp = new byte[1];

        public void write(int b) throws IOException {
            temp[0] = (byte) b;
            write(temp, 0, 1);
        }

        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || off+len > b.length) {
                throw new IndexOutOfBoundsException();
            }
            if (eom && !ignoreEOM) {
                throw new LogicalEOMException("logical end-of-media");
            }

            int n = tapeWrite(b, off, len);
            while (n < len) {
                n += tapeWrite(b, off + n, len - n);
            }
        }

        public void close() throws IOException {
            BasicTapeDevice.this.close();
        }
    }

    protected void finalize() {
        try {
            close();
        } catch (IOException ex) {
        }
    }

    private void ensureOpen() throws IOException {
        if (fd == null || !fd.valid()) {
            throw new IOException("tape device is not open");
        }
    }

    private static native void initFields();
    private native void tapeOpen(String pathName) throws IOException;
    private native void tapeClose() throws IOException;
    private native int tapeRead(byte[] b, int off, int len) throws IOException;
    private native int tapeWrite(byte[] b, int off, int len) throws IOException;
    private native int tapeGetBlockSize() throws IOException;
    private native void tapeSetBlockSize(int bs) throws IOException;
    private native void tapeRewind() throws IOException;
    private native void tapeSpaceEOD() throws IOException;

    /* load the JNI library specific for this platform */
    static {
        StringBuffer buf = new StringBuffer("Tape");
        String osName = System.getProperty("os.name");
        if (osName.equals("Windows NT") || osName.equals("Windows 2000")) {
            buf.append("WinNT");
        } else {
            buf.append(osName);
        }

        System.loadLibrary(buf.toString());

        initFields();
    }
}
