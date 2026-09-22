package mdreader;

/**
 * Разбор одной строки Markdown. Ровно то подмножество, которое имеет смысл
 * на экране 176x220: заголовки, списки, цитаты, код, разделители и инлайн-стили.
 */
public final class MdParser {

    private MdParser() {
    }

    public static MdBlock parse(String line, boolean inFence) {
        MdBlock b = new MdBlock();
        if (inFence) {
            b.type = MdBlock.T_CODE;
            b.text = expandTabs(line);
            b.style = fill(b.text.length(), MdBlock.S_CODE);
            return b;
        }

        int indent = 0;
        int p = 0;
        while (p < line.length() && (line.charAt(p) == ' ' || line.charAt(p) == '\t')) {
            indent += line.charAt(p) == '\t' ? 4 : 1;
            p++;
        }
        String s = line.substring(p);

        if (s.length() == 0) {
            b.type = MdBlock.T_EMPTY;
            b.text = "";
            b.style = new byte[0];
            return b;
        }

        // отступ в четыре пробела и больше — блок кода
        if (indent >= 4) {
            b.type = MdBlock.T_CODE;
            b.text = expandTabs(s);
            b.style = fill(b.text.length(), MdBlock.S_CODE);
            return b;
        }

        if (isRule(s)) {
            b.type = MdBlock.T_HR;
            b.text = "";
            b.style = new byte[0];
            return b;
        }

        if (s.charAt(0) == '#') {
            int lvl = 0;
            while (lvl < s.length() && s.charAt(lvl) == '#' && lvl < 6) {
                lvl++;
            }
            if (lvl < s.length() && s.charAt(lvl) == ' ') {
                b.type = MdBlock.T_HEAD;
                b.level = lvl;
                inline(b, trimHashes(s.substring(lvl + 1)));
                return b;
            }
        }

        if (s.charAt(0) == '>') {
            String rest = s.substring(1);
            if (rest.startsWith(" ")) {
                rest = rest.substring(1);
            }
            b.type = MdBlock.T_QUOTE;
            inline(b, rest);
            return b;
        }

        char c0 = s.charAt(0);
        if ((c0 == '-' || c0 == '*' || c0 == '+') && s.length() > 1 && s.charAt(1) == ' ') {
            b.type = MdBlock.T_LIST;
            b.level = indent / 2;
            b.bullet = "• ";
            inline(b, s.substring(2));
            return b;
        }

        int d = 0;
        while (d < s.length() && s.charAt(d) >= '0' && s.charAt(d) <= '9') {
            d++;
        }
        if (d > 0 && d + 1 < s.length() && (s.charAt(d) == '.' || s.charAt(d) == ')') && s.charAt(d + 1) == ' ') {
            b.type = MdBlock.T_LIST;
            b.level = indent / 2;
            b.bullet = s.substring(0, d + 1) + " ";
            inline(b, s.substring(d + 2));
            return b;
        }

        b.type = MdBlock.T_TEXT;
        inline(b, s);
        return b;
    }

    /**
     * Приклеивает к блоку продолжение абзаца со следующей строки.
     * В Markdown перенос внутри абзаца незначим, а на экране в 176 точек
     * рваные строки из исходника выглядят плохо.
     */
    public static void join(MdBlock b, MdBlock next) {
        String sep = b.text.length() > 0 && !b.text.endsWith(" ") ? " " : "";
        int n = b.text.length() + sep.length() + next.text.length();
        byte[] st = new byte[n];
        System.arraycopy(b.style, 0, st, 0, b.style.length);
        System.arraycopy(next.style, 0, st, b.text.length() + sep.length(), next.style.length);
        b.text = b.text + sep + next.text;
        b.style = st;
    }

    /** Строка-ограда блока кода. */
    public static boolean isFence(String line) {
        String s = line.trim();
        return s.startsWith("```") || s.startsWith("~~~");
    }

    private static boolean isRule(String s) {
        String t = s.trim();
        if (t.length() < 3) {
            return false;
        }
        char c = t.charAt(0);
        if (c != '-' && c != '*' && c != '_') {
            return false;
        }
        for (int i = 0; i < t.length(); i++) {
            if (t.charAt(i) != c && t.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    private static String trimHashes(String s) {
        int end = s.length();
        while (end > 0 && (s.charAt(end - 1) == '#' || s.charAt(end - 1) == ' ')) {
            end--;
        }
        return s.substring(0, end);
    }

    private static String expandTabs(String s) {
        if (s.indexOf('\t') < 0) {
            return s;
        }
        StringBuffer sb = new StringBuffer(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\t') {
                sb.append("    ");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static byte[] fill(int n, byte v) {
        byte[] a = new byte[n];
        for (int i = 0; i < n; i++) {
            a[i] = v;
        }
        return a;
    }

    /** Снимает инлайн-разметку, запоминая стиль каждого оставшегося символа. */
    private static void inline(MdBlock b, String s) {
        final char tick = '`';
        int n = s.length();
        StringBuffer out = new StringBuffer(n);
        byte[] st = new byte[n];
        int len = 0;
        byte cur = 0;
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);

            if (c == '\\' && i + 1 < n) {
                out.append(s.charAt(i + 1));
                st[len++] = cur;
                i += 2;
                continue;
            }
            if (c == tick) {
                cur ^= MdBlock.S_CODE;
                i++;
                continue;
            }
            if ((cur & MdBlock.S_CODE) == 0 && (c == '*' || c == '_')) {
                boolean dbl = i + 1 < n && s.charAt(i + 1) == c;
                if (dbl) {
                    cur ^= MdBlock.S_BOLD;
                    i += 2;
                } else {
                    cur ^= MdBlock.S_ITALIC;
                    i++;
                }
                continue;
            }
            // ссылка [текст](url) и картинка ![подпись](url) — оставляем видимую часть
            if ((cur & MdBlock.S_CODE) == 0 && (c == '[' || (c == '!' && i + 1 < n && s.charAt(i + 1) == '['))) {
                int open = c == '!' ? i + 1 : i;
                int close = s.indexOf(']', open);
                if (close > 0) {
                    String label = s.substring(open + 1, close);
                    int after = close + 1;
                    int skipTo = after;
                    if (after < n && s.charAt(after) == '(') {
                        int par = s.indexOf(')', after);
                        if (par > 0) {
                            skipTo = par + 1;
                        }
                    }
                    byte lstyle = (byte) (cur | MdBlock.S_LINK);
                    for (int k = 0; k < label.length(); k++) {
                        char lc = label.charAt(k);
                        if (lc == '*' || lc == tick || lc == '_') {
                            continue;
                        }
                        out.append(lc);
                        st[len++] = lstyle;
                    }
                    i = skipTo;
                    continue;
                }
            }
            out.append(c);
            st[len++] = cur;
            i++;
        }
        b.text = out.toString();
        byte[] trimmed = new byte[len];
        System.arraycopy(st, 0, trimmed, 0, len);
        b.style = trimmed;
    }
}
