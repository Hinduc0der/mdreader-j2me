package mdreader;

/**
 * Декодирование байтов в текст. Своими руками, потому что на части S40/SE
 * системный конвертер UTF-8 либо отсутствует, либо портит кириллицу.
 */
public final class Text {

    public static final int ENC_AUTO = 0;
    public static final int ENC_UTF8 = 1;
    public static final int ENC_CP1251 = 2;
    public static final int ENC_LATIN1 = 3;

    public static final String[] ENC_NAMES = { "Авто", "UTF-8", "Windows-1251", "Latin-1" };

    /** Верхняя половина CP1251 (0x80..0xFF). */
    private static final String CP1251_HIGH =
        "ЂЃ‚ѓ„…†‡" +
        "€‰Љ‹ЊЌЋЏ" +
        "ђ‘’“”•–—" +
        "�™љ›њќћџ" +
        " ЎўЈ¤Ґ¦§" +
        "Ё©Є«¬­®Ї" +
        "°±Ііґµ¶·" +
        "ё№є»јЅѕї" +
        "АБВГДЕЖЗ" +
        "ИЙКЛМНОП" +
        "РСТУФХЦЧ" +
        "ШЩЪЫЬЭЮЯ" +
        "абвгдежз" +
        "ийклмноп" +
        "рстуфхцч" +
        "шщъыьэюя";

    private Text() {
    }

    /**
     * Сколько байт в конце буфера образуют незавершённую UTF-8 последовательность.
     * Нужно, чтобы не рвать символ на границе прочитанного окна.
     */
    public static int danglingTail(byte[] b, int off, int len, int enc) {
        if (enc != ENC_UTF8) {
            return 0;
        }
        int end = off + len;
        int back = 0;
        while (back < 3 && back < len) {
            int c = b[end - 1 - back] & 0xFF;
            if (c < 0x80) {
                return 0;
            }
            if (c >= 0xC0) {
                int need = c >= 0xF0 ? 4 : (c >= 0xE0 ? 3 : 2);
                return need > back + 1 ? back + 1 : 0;
            }
            back++;
        }
        return 0;
    }

    public static String decode(byte[] b, int off, int len, int enc) {
        if (enc == ENC_AUTO) {
            enc = detect(b, off, len);
        }
        if (enc == ENC_UTF8) {
            return decodeUtf8(b, off, len);
        }
        char[] out = new char[len];
        for (int i = 0; i < len; i++) {
            int c = b[off + i] & 0xFF;
            if (c < 0x80 || enc == ENC_LATIN1) {
                out[i] = (char) c;
            } else {
                out[i] = CP1251_HIGH.charAt(c - 0x80);
            }
        }
        return new String(out);
    }

    private static String decodeUtf8(byte[] b, int off, int len) {
        char[] out = new char[len];
        int n = 0;
        int i = off;
        int end = off + len;
        while (i < end) {
            int c = b[i++] & 0xFF;
            if (c < 0x80) {
                out[n++] = (char) c;
            } else if (c < 0xC0) {
                out[n++] = '?'; // U+FFFD в шрифтах S40/S60 нет — вышел бы пустой квадрат
            } else if (c < 0xE0) {
                if (i >= end) {
                    break;
                }
                out[n++] = (char) (((c & 0x1F) << 6) | (b[i++] & 0x3F));
            } else if (c < 0xF0) {
                if (i + 1 >= end) {
                    break;
                }
                int c2 = b[i++] & 0x3F;
                int c3 = b[i++] & 0x3F;
                out[n++] = (char) (((c & 0x0F) << 12) | (c2 << 6) | c3);
            } else {
                // за пределами BMP — телефон всё равно не нарисует
                i += 3;
                if (i > end) {
                    break;
                }
                out[n++] = '?'; // U+FFFD в шрифтах S40/S60 нет — вышел бы пустой квадрат
            }
        }
        return new String(out, 0, n);
    }

    /** Грубая, но надёжная эвристика: валидный UTF-8 — значит UTF-8. */
    public static int detect(byte[] b, int off, int len) {
        boolean high = false;
        int i = off;
        int end = off + len;
        while (i < end) {
            int c = b[i++] & 0xFF;
            if (c < 0x80) {
                continue;
            }
            high = true;
            int need;
            if (c >= 0xC2 && c <= 0xDF) {
                need = 1;
            } else if (c >= 0xE0 && c <= 0xEF) {
                need = 2;
            } else if (c >= 0xF0 && c <= 0xF4) {
                need = 3;
            } else {
                return ENC_CP1251;
            }
            if (i + need > end) {
                break;
            }
            for (int k = 0; k < need; k++) {
                int cc = b[i++] & 0xFF;
                if (cc < 0x80 || cc > 0xBF) {
                    return ENC_CP1251;
                }
            }
        }
        return high ? ENC_UTF8 : ENC_UTF8;
    }

    /** Длина BOM в начале файла (0 или 3). */
    public static int bomLength(byte[] b, int off, int len) {
        if (len >= 3 && (b[off] & 0xFF) == 0xEF && (b[off + 1] & 0xFF) == 0xBB && (b[off + 2] & 0xFF) == 0xBF) {
            return 3;
        }
        return 0;
    }
}
