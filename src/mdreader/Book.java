package mdreader;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 * Доступ к файлу книги: файл целиком в память не грузим (на K750 её около мегабайта),
 * держим «окно» и выдаём логические строки по байтовым смещениям.
 */
public class Book {

    private static final int WINDOW = 8192;
    private static final int MAX_LINE = 4096;

    private final String url;
    private FileConnection fc;
    private long size;
    private int enc;
    private int bom;

    private final byte[] buf = new byte[WINDOW];
    private long bufStart = -1;
    private int bufLen;

    public Book(String url, int enc) throws IOException {
        this.url = url;
        this.enc = enc;
        fc = (FileConnection) Connector.open(url, Connector.READ);
        if (!fc.exists()) {
            throw new IOException(Strings.FILE_NOT_FOUND);
        }
        size = fc.fileSize();
        fill(0);
        bom = Text.bomLength(buf, 0, bufLen);
        if (this.enc == Text.ENC_AUTO) {
            this.enc = Text.detect(buf, bom, bufLen - bom);
        }
    }

    public long size() {
        return size;
    }

    public long start() {
        return bom;
    }

    public int encoding() {
        return enc;
    }

    public void setEncoding(int e) {
        enc = (e == Text.ENC_AUTO) ? Text.detect(buf, bom, bufLen - bom) : e;
    }

    public String name() {
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    public String url() {
        return url;
    }

    public void close() {
        try {
            if (fc != null) {
                fc.close();
            }
        } catch (IOException e) {
            // закрываемся — жаловаться некому
        }
        fc = null;
    }

    /** Загружает окно так, чтобы смещение off оказалось внутри буфера. */
    private void fill(long off) throws IOException {
        if (off < 0) {
            off = 0;
        }
        if (bufStart >= 0 && off >= bufStart && off < bufStart + bufLen) {
            return;
        }
        InputStream in = fc.openInputStream();
        try {
            long skipped = 0;
            while (skipped < off) {
                long n = in.skip(off - skipped);
                if (n <= 0) {
                    break;
                }
                skipped += n;
            }
            int total = 0;
            while (total < WINDOW) {
                int n = in.read(buf, total, WINDOW - total);
                if (n < 0) {
                    break;
                }
                total += n;
            }
            bufStart = off;
            bufLen = total;
        } finally {
            in.close();
        }
    }

    private int byteAt(long off) throws IOException {
        if (off < 0 || off >= size) {
            return -1;
        }
        if (bufStart < 0 || off < bufStart || off >= bufStart + bufLen) {
            // при движении назад ставим окно так, чтобы за off оставался запас
            fill(off < WINDOW / 2 ? 0 : off - WINDOW / 2);
        }
        return buf[(int) (off - bufStart)] & 0xFF;
    }

    /** Логическая строка (до перевода строки), начинающаяся со смещения off. */
    public String lineAt(long off) throws IOException {
        long end = lineEnd(off);
        int len = (int) (end - off);
        if (len <= 0) {
            return "";
        }
        byte[] tmp = new byte[len];
        for (int i = 0; i < len; i++) {
            int c = byteAt(off + i);
            tmp[i] = (byte) (c < 0 ? 0 : c);
        }
        return Text.decode(tmp, 0, len, enc);
    }

    /** Смещение конца строки (позиция '\r' или '\n'). */
    private long lineEnd(long off) throws IOException {
        long p = off;
        long limit = off + MAX_LINE;
        while (p < size && p < limit) {
            int c = byteAt(p);
            if (c == '\n' || c == '\r') {
                return p;
            }
            p++;
        }
        return p;
    }

    public long nextLineOffset(long off) throws IOException {
        long p = lineEnd(off);
        if (p >= size) {
            return size;
        }
        int c = byteAt(p);
        if (c == '\r') {
            p++;
            if (p < size && byteAt(p) == '\n') {
                p++;
            }
        } else if (c == '\n') {
            p++;
        } else {
            // строка обрезана по MAX_LINE — продолжаем с того же места
        }
        return p;
    }

    public long prevLineOffset(long off) throws IOException {
        if (off <= bom) {
            return bom;
        }
        long p = off - 1;
        int c = byteAt(p);
        if (c == '\n') {
            p--;
            if (p >= bom && byteAt(p) == '\r') {
                p--;
            }
        } else if (c == '\r') {
            p--;
        }
        while (p >= bom) {
            int b = byteAt(p);
            if (b == '\n' || b == '\r') {
                return p + 1;
            }
            p--;
        }
        return bom;
    }

    public int percent(long off) {
        if (size <= 0) {
            return 0;
        }
        return (int) (off * 100L / size);
    }

    /** Смещение, соответствующее проценту, выровненное на начало строки. */
    public long offsetForPercent(int pct) throws IOException {
        long off = size * pct / 100L;
        if (off <= bom) {
            return bom;
        }
        if (off >= size) {
            off = size - 1;
        }
        return prevLineOffset(off);
    }
}
