package tools.data;

import java.io.IOException;
import tools.HexTool;

public class ByteArrayByteStream {

    private int pos = 0;
    private long bytesRead = 0L;
    private byte[] arr;

    public ByteArrayByteStream(byte[] arr) {
        this.arr = arr;
    }

    public long getPosition() {
        return this.pos;
    }

    public void seek(long offset)
            throws IOException {
        this.pos = (int) offset;
    }

    public long getBytesRead() {
        return this.bytesRead;
    }

    public int readByte() {
        bytesRead ++;
        return ((int)arr[pos++]) & 0xFF;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean b) {
        String nows = "";
        if (this.arr.length - this.pos > 0) {
            byte[] now = new byte[this.arr.length - this.pos];
            System.arraycopy(this.arr, this.pos, now, 0, this.arr.length - this.pos);
            nows = HexTool.toString(now);
        }
        if (b) {
            return "\r\nAll: " + HexTool.toString(this.arr) + "\r\nNow: " + nows;
        }
        return "Data: " + nows;
    }

    public long available() {
        return this.arr.length - this.pos;
    }
}