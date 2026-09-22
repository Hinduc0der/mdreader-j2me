package mdreader;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Экран чтения: сам считает разметку, сам рисует, сам листает.
 * Страница задаётся парой (байтовое смещение строки; номер визуальной строки внутри неё).
 */
public class ReaderCanvas extends Canvas {

    private final MDReaderMIDlet midlet;
    private final Settings st;
    private Book book;

    private long pageOff;
    private int pageSkip;

    private long nextOff;
    private int nextSkip;

    private Timer scrollTimer;
    private boolean autoScroll;

    private String toast;
    private long toastUntil;

    // цвета текущей схемы
    private int cBg;
    private int cFg;
    private int cDim;
    private int cAccent;

    public ReaderCanvas(MDReaderMIDlet midlet, Settings st) {
        this.midlet = midlet;
        this.st = st;
        setFullScreenMode(st.fullScreen);
        applyScheme();
    }

    public void setBook(Book b, long off) {
        this.book = b;
        this.pageOff = off < 0 ? b.start() : off;
        this.pageSkip = 0;
        repaint();
    }

    public Book book() {
        return book;
    }

    public long position() {
        return pageOff;
    }

    public void applyScheme() {
        switch (st.scheme) {
            case 1: // ночь
                cBg = 0x000000;
                cFg = 0xD8D8D8;
                cDim = 0x707070;
                cAccent = 0x8FB8E8;
                break;
            case 2: // сепия
                cBg = 0xF3E7CE;
                cFg = 0x3B2F20;
                cDim = 0x8A7A5C;
                cAccent = 0x7A4B1E;
                break;
            default: // день
                cBg = 0xFFFFFF;
                cFg = 0x101010;
                cDim = 0x909090;
                cAccent = 0x1A4E8A;
        }
        setFullScreenMode(st.fullScreen);
        repaint();
    }

    // ------------------------------------------------------------------ шрифты

    private int baseSize() {
        if (st.fontSize <= 0) {
            return Font.SIZE_SMALL;
        }
        return st.fontSize == 1 ? Font.SIZE_MEDIUM : Font.SIZE_LARGE;
    }

    private int bigger(int size) {
        if (size == Font.SIZE_SMALL) {
            return Font.SIZE_MEDIUM;
        }
        return Font.SIZE_LARGE;
    }

    private Font fontFor(MdBlock b, byte style) {
        int face = Font.FACE_PROPORTIONAL;
        int s = Font.STYLE_PLAIN;
        int size = baseSize();

        if (b.type == MdBlock.T_CODE || (style & MdBlock.S_CODE) != 0) {
            face = Font.FACE_MONOSPACE;
        }
        if (b.type == MdBlock.T_HEAD) {
            s |= Font.STYLE_BOLD;
            if (b.level <= 2) {
                size = bigger(size);
            }
        }
        if ((style & MdBlock.S_BOLD) != 0) {
            s |= Font.STYLE_BOLD;
        }
        if ((style & MdBlock.S_ITALIC) != 0) {
            s |= Font.STYLE_ITALIC;
        }
        if ((style & MdBlock.S_LINK) != 0) {
            s |= Font.STYLE_UNDERLINED;
        }
        return Font.getFont(face, s, size);
    }

    private Font baseFontFor(MdBlock b) {
        return fontFor(b, (byte) 0);
    }

    /**
     * Ширина колонки текста. На широком экране (внутренний дисплей E90 — 800 точек)
     * строка во всю ширину нечитаема, поэтому колонку ограничиваем примерно
     * 45 знаками и центрируем.
     */
    private int columnWidth() {
        int w = getWidth() - 4;
        Font f = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, baseSize());
        int max = f.charWidth('о') * 45;
        return w > max ? max : w;
    }

    private int marginX() {
        return (getWidth() - columnWidth()) / 2;
    }

    private int indentFor(MdBlock b) {
        int pad = 2;
        if (b.type == MdBlock.T_QUOTE) {
            return pad + 8;
        }
        if (b.type == MdBlock.T_LIST) {
            return pad + 6 + b.level * 8;
        }
        if (b.type == MdBlock.T_CODE) {
            return pad + 4;
        }
        return pad;
    }

    // ------------------------------------------------------------------ разметка

    /** Границы визуальных строк блока: пары (начало, конец) в тексте блока. */
    private int[] wrap(MdBlock b, int avail) {
        String t = b.text;
        int n = t.length();
        if (n == 0) {
            return new int[] { 0, 0 };
        }
        int[] res = new int[16];
        int cnt = 0;
        int start = 0;
        int lastSpace = -1;
        int w = 0;
        boolean code = b.type == MdBlock.T_CODE;

        for (int i = 0; i < n; i++) {
            char c = t.charAt(i);
            byte sty = i < b.style.length ? b.style[i] : 0;
            int cw = fontFor(b, sty).charWidth(c);
            if (c == ' ') {
                lastSpace = i;
            }
            if (w + cw > avail && i > start) {
                int brk = (!code && lastSpace > start) ? lastSpace : i;
                if (cnt + 2 > res.length) {
                    int[] bigger = new int[res.length * 2];
                    System.arraycopy(res, 0, bigger, 0, cnt);
                    res = bigger;
                }
                res[cnt++] = start;
                res[cnt++] = brk;
                start = (!code && lastSpace > start) ? brk + 1 : brk;
                lastSpace = -1;
                w = 0;
                for (int k = start; k <= i; k++) {
                    byte s2 = k < b.style.length ? b.style[k] : 0;
                    w += fontFor(b, s2).charWidth(t.charAt(k));
                }
            } else {
                w += cw;
            }
        }
        if (cnt + 2 > res.length) {
            int[] bigger = new int[res.length + 2];
            System.arraycopy(res, 0, bigger, 0, cnt);
            res = bigger;
        }
        res[cnt++] = start;
        res[cnt++] = n;

        int[] out = new int[cnt];
        System.arraycopy(res, 0, out, 0, cnt);
        return out;
    }

    private int wrapCount(MdBlock b, int avail) {
        return wrap(b, avail).length / 2;
    }

    private int wrapCount(MdBlock b) {
        if (b.type == MdBlock.T_EMPTY) {
            return 1;
        }
        return wrapCount(b, columnWidth() - indentFor(b) - 2);
    }

    /**
     * Блок, начинающийся со смещения off: строка плюс приклеенные к ней
     * продолжения абзаца. В nextOut попадает смещение следующего блока.
     */
    private MdBlock blockAt(long off, boolean fence, long[] nextOut) throws IOException {
        MdBlock b = MdParser.parse(book.lineAt(off), fence);
        long next = book.nextLineOffset(off);
        if (!fence && st.joinLines && (b.type == MdBlock.T_TEXT || b.type == MdBlock.T_QUOTE)) {
            int guard = 0;
            long size = book.size();
            while (next < size && guard++ < 300) {
                String l2 = book.lineAt(next);
                if (MdParser.isFence(l2)) {
                    break;
                }
                MdBlock b2 = MdParser.parse(l2, false);
                if (b2.type != b.type || b2.isBlank()) {
                    break;
                }
                MdParser.join(b, b2);
                next = book.nextLineOffset(next);
            }
        }
        nextOut[0] = next;
        return b;
    }

    /** Отматывает смещение к началу абзаца, чтобы не встать в его середину. */
    private long blockStart(long off) throws IOException {
        if (!st.joinLines || off <= book.start()) {
            return off;
        }
        MdBlock cur = MdParser.parse(book.lineAt(off), false);
        if (cur.type != MdBlock.T_TEXT && cur.type != MdBlock.T_QUOTE) {
            return off;
        }
        long p = off;
        int guard = 0;
        while (p > book.start() && guard++ < 300) {
            long prev = book.prevLineOffset(p);
            if (prev == p) {
                break;
            }
            MdBlock b = MdParser.parse(book.lineAt(prev), false);
            if (b.type != cur.type || b.isBlank()) {
                break;
            }
            p = prev;
        }
        return p;
    }

    // ------------------------------------------------------------------ рисование

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        g.setColor(cBg);
        g.fillRect(0, 0, w, h);

        if (book == null) {
            g.setColor(cFg);
            g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM));
            g.drawString("Книга не открыта", 4, 4, Graphics.TOP | Graphics.LEFT);
            return;
        }

        int statusH = st.showStatus ? Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL).getHeight() + 2 : 0;
        try {
            layout(g, pageOff, pageSkip, h - statusH, true);
        } catch (IOException e) {
            g.setColor(cFg);
            g.drawString("Ошибка чтения", 4, 4, Graphics.TOP | Graphics.LEFT);
        }

        if (statusH > 0) {
            drawStatus(g, w, h, statusH);
        }
        if (toast != null && System.currentTimeMillis() < toastUntil) {
            drawToast(g, w, h);
        }
    }

    /**
     * Общий проход разметки: рисует (draw=true) или только считает, где кончится страница.
     * Результат попадает в nextOff/nextSkip.
     */
    private void layout(Graphics g, long startOff, int skip, int bottom, boolean draw) throws IOException {
        int left = marginX();
        int colW = columnWidth();
        int y = 2;
        long off = startOff;
        boolean fence = false;
        long size = book.size();

        long[] nx = new long[1];
        while (y < bottom && off < size) {
            String line = book.lineAt(off);
            if (MdParser.isFence(line)) {
                fence = !fence;
                off = book.nextLineOffset(off);
                skip = 0;
                continue;
            }
            MdBlock b = blockAt(off, fence, nx);

            if (b.type == MdBlock.T_EMPTY) {
                y += baseFontFor(b).getHeight() / 2;
                off = nx[0];
                skip = 0;
                continue;
            }
            if (b.type == MdBlock.T_HR) {
                int lh = baseFontFor(b).getHeight() / 2 + 2;
                if (y + lh > bottom) {
                    break;
                }
                if (draw) {
                    g.setColor(cDim);
                    g.drawLine(left + 6, y + lh / 2, left + colW - 6, y + lh / 2);
                }
                y += lh;
                off = nx[0];
                skip = 0;
                continue;
            }
            if (b.type == MdBlock.T_HEAD && y > 4) {
                y += 3;
            }

            int indent = indentFor(b);
            int avail = colW - indent - 2;
            int[] parts = wrap(b, avail);
            Font base = baseFontFor(b);
            int lh = base.getHeight();
            int lines = parts.length / 2;

            for (int k = skip; k < lines; k++) {
                if (y + lh > bottom) {
                    nextOff = off;
                    nextSkip = k;
                    return;
                }
                if (draw) {
                    drawVisualLine(g, b, parts[k * 2], parts[k * 2 + 1], left + indent, y, k == 0, lh);
                }
                y += lh;
            }
            skip = 0;
            off = nx[0];
        }
        nextOff = off;
        nextSkip = 0;
    }

    private void drawVisualLine(Graphics g, MdBlock b, int from, int to, int indent, int y, boolean first, int lh) {
        int x = indent;

        if (b.type == MdBlock.T_QUOTE) {
            g.setColor(cDim);
            g.fillRect(indent - 6, y, 2, lh);
        }
        if (b.type == MdBlock.T_CODE) {
            g.setColor(cBg == 0x000000 ? 0x1C1C1C : (cBg == 0xFFFFFF ? 0xF0F0F0 : 0xE8D9BC));
            g.fillRect(marginX(), y, columnWidth(), lh);
        }
        if (b.type == MdBlock.T_LIST && first && b.bullet != null) {
            Font bf = baseFontFor(b);
            g.setFont(bf);
            g.setColor(cDim);
            g.drawString(b.bullet, indent - 6, y, Graphics.TOP | Graphics.LEFT);
            x = indent - 6 + bf.stringWidth(b.bullet);
        }

        int i = from;
        while (i < to) {
            byte sty = i < b.style.length ? b.style[i] : 0;
            int j = i + 1;
            while (j < to && (j < b.style.length ? b.style[j] : 0) == sty) {
                j++;
            }
            Font f = fontFor(b, sty);
            g.setFont(f);
            if (b.type == MdBlock.T_HEAD) {
                g.setColor(cAccent);
            } else if ((sty & MdBlock.S_LINK) != 0) {
                g.setColor(cAccent);
            } else if (b.type == MdBlock.T_QUOTE || b.type == MdBlock.T_CODE) {
                g.setColor(cDim);
            } else {
                g.setColor(cFg);
            }
            g.drawSubstring(b.text, i, j - i, x, y, Graphics.TOP | Graphics.LEFT);
            x += f.substringWidth(b.text, i, j - i);
            i = j;
        }
    }

    private void drawStatus(Graphics g, int w, int h, int statusH) {
        Font f = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(f);
        int y = h - statusH + 1;
        g.setColor(cBg);
        g.fillRect(0, h - statusH, w, statusH);
        g.setColor(cDim);
        g.drawLine(0, h - statusH, w, h - statusH);

        String left = book.name();
        String right = book.percent(pageOff) + "%";

        // Значок автопрокрутки рисуем треугольником, а не символом: в шрифтах S60 и S40
        // нет U+25B6, и телефон вместо стрелки ставит пустой квадрат (проверено на E90).
        int triW = autoScroll ? Math.max(4, f.getHeight() / 2) : 0;
        int triGap = autoScroll ? 3 : 0;
        int rw = f.stringWidth(right) + triW + triGap;
        while (left.length() > 3 && f.stringWidth(left) > w - rw - 8) {
            left = left.substring(0, left.length() - 2);
        }
        g.drawString(left, 2, y, Graphics.TOP | Graphics.LEFT);
        g.drawString(right, w - 2 - triW - triGap, y, Graphics.TOP | Graphics.RIGHT);
        if (autoScroll) {
            int tx = w - 2 - triW;
            int ty = y + (f.getHeight() - triW) / 2;
            g.fillTriangle(tx, ty, tx, ty + triW, tx + triW, ty + triW / 2);
        }
    }

    private void drawToast(Graphics g, int w, int h) {
        Font f = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(f);
        int tw = f.stringWidth(toast) + 10;
        int th = f.getHeight() + 6;
        int x = (w - tw) / 2;
        int y = h / 2 - th / 2;
        g.setColor(cFg);
        g.fillRect(x, y, tw, th);
        g.setColor(cBg);
        g.drawString(toast, x + 5, y + 3, Graphics.TOP | Graphics.LEFT);
    }

    public void toast(String msg) {
        toast = msg;
        toastUntil = System.currentTimeMillis() + 1200;
        repaint();
    }

    // ------------------------------------------------------------------ навигация

    private int textBottom() {
        int statusH = st.showStatus ? Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL).getHeight() + 2 : 0;
        return getHeight() - statusH;
    }

    public void pageDown() {
        try {
            layout(null, pageOff, pageSkip, textBottom(), false);
            if (nextOff >= book.size() && nextSkip == 0) {
                toast("Конец");
                return;
            }
            pageOff = nextOff;
            pageSkip = nextSkip;
            repaint();
        } catch (IOException e) {
            toast("Ошибка чтения");
        }
    }

    public void pageUp() {
        try {
            int bottom = textBottom();
            int need = bottom - 4;
            while (need > 0) {
                if (!lineUp()) {
                    break;
                }
                need -= currentLineHeight();
            }
            repaint();
        } catch (IOException e) {
            toast("Ошибка чтения");
        }
    }

    private int currentLineHeight() throws IOException {
        long[] nx = new long[1];
        return baseFontFor(blockAt(pageOff, false, nx)).getHeight();
    }

    public void lineDown() {
        try {
            long[] nx = new long[1];
            MdBlock b = blockAt(pageOff, false, nx);
            if (pageSkip + 1 < wrapCount(b)) {
                pageSkip++;
            } else {
                if (nx[0] >= book.size()) {
                    toast("Конец");
                    return;
                }
                pageOff = nx[0];
                pageSkip = 0;
            }
            repaint();
        } catch (IOException e) {
            toast("Ошибка чтения");
        }
    }

    private boolean lineUp() throws IOException {
        if (pageSkip > 0) {
            pageSkip--;
            return true;
        }
        if (pageOff <= book.start()) {
            return false;
        }
        long prev = blockStart(book.prevLineOffset(pageOff));
        if (prev == pageOff) {
            return false;
        }
        pageOff = prev;
        long[] nx = new long[1];
        pageSkip = wrapCount(blockAt(pageOff, false, nx)) - 1;
        return true;
    }

    public void lineUpPublic() {
        try {
            if (!lineUp()) {
                toast("Начало");
            }
            repaint();
        } catch (IOException e) {
            toast("Ошибка чтения");
        }
    }

    /** Следующий/предыдущий заголовок — «глава». */
    public void jumpHeading(boolean forward) {
        try {
            long off = pageOff;
            for (int i = 0; i < 3000; i++) {
                off = forward ? book.nextLineOffset(off) : book.prevLineOffset(off);
                if (off <= book.start() || off >= book.size()) {
                    break;
                }
                String line = book.lineAt(off);
                if (line.length() > 1 && line.charAt(0) == '#') {
                    pageOff = off;
                    pageSkip = 0;
                    repaint();
                    return;
                }
            }
            toast(forward ? "Заголовков дальше нет" : "Заголовков выше нет");
        } catch (IOException e) {
            toast("Ошибка чтения");
        }
    }

    public void gotoOffset(long off) {
        try {
            pageOff = blockStart(off);
        } catch (IOException e) {
            pageOff = off;
        }
        pageSkip = 0;
        repaint();
    }

    public void gotoPercent(int pct) {
        try {
            gotoOffset(book.offsetForPercent(pct));
        } catch (IOException e) {
            toast("Ошибка чтения");
        }
    }

    /** Список заголовков для «Содержания». */
    public Vector headings() {
        Vector v = new Vector();
        try {
            long off = book.start();
            long size = book.size();
            int guard = 0;
            while (off < size && guard++ < 20000 && v.size() < 200) {
                String line = book.lineAt(off);
                if (line.length() > 1 && line.charAt(0) == '#') {
                    int lvl = 0;
                    while (lvl < line.length() && line.charAt(lvl) == '#') {
                        lvl++;
                    }
                    StringBuffer sb = new StringBuffer();
                    for (int i = 1; i < lvl; i++) {
                        sb.append(' ');
                    }
                    sb.append(line.substring(lvl).trim());
                    v.addElement(new Object[] { sb.toString(), new Long(off) });
                }
                off = book.nextLineOffset(off);
            }
        } catch (IOException e) {
            // покажем то, что успели собрать
        }
        return v;
    }

    // ------------------------------------------------------------------ автопрокрутка

    public void toggleAutoScroll() {
        if (autoScroll) {
            stopAutoScroll();
            toast("Автопрокрутка выкл");
        } else {
            startAutoScroll();
            toast("Автопрокрутка вкл");
        }
    }

    public void startAutoScroll() {
        stopAutoScroll();
        autoScroll = true;
        scrollTimer = new Timer();
        scrollTimer.schedule(new TimerTask() {
            public void run() {
                lineDown();
            }
        }, st.scrollDelay, st.scrollDelay);
    }

    public void stopAutoScroll() {
        autoScroll = false;
        if (scrollTimer != null) {
            scrollTimer.cancel();
            scrollTimer = null;
        }
    }

    public boolean isAutoScroll() {
        return autoScroll;
    }

    public void changeSpeed(int delta) {
        st.scrollDelay += delta;
        if (st.scrollDelay < 200) {
            st.scrollDelay = 200;
        }
        if (st.scrollDelay > 4000) {
            st.scrollDelay = 4000;
        }
        if (autoScroll) {
            startAutoScroll();
        }
        toast("Скорость: " + st.scrollDelay + " мс");
    }

    // ------------------------------------------------------------------ клавиши

    protected void keyPressed(int key) {
        int action = -1;
        try {
            action = getGameAction(key);
        } catch (Exception e) {
            // на части аппаратов getGameAction кидается на странных кодах
        }

        if (key == Canvas.KEY_NUM5 || action == Canvas.FIRE) {
            midlet.showMenu();
            return;
        }
        if (key == Canvas.KEY_NUM2 || action == Canvas.UP) {
            lineUpPublic();
            return;
        }
        if (key == Canvas.KEY_NUM8 || action == Canvas.DOWN) {
            lineDown();
            return;
        }
        if (key == Canvas.KEY_NUM4 || action == Canvas.LEFT) {
            jumpHeading(false);
            return;
        }
        if (key == Canvas.KEY_NUM6 || action == Canvas.RIGHT) {
            jumpHeading(true);
            return;
        }
        if (key == Canvas.KEY_NUM7 || action == Canvas.GAME_A) {
            pageUp();
            return;
        }
        if (key == Canvas.KEY_NUM9 || action == Canvas.GAME_B) {
            pageDown();
            return;
        }
        if (key == ' ') {
            pageDown();
            return;
        }
        if (key == 8) {
            pageUp();
            return;
        }
        if (key == Canvas.KEY_NUM0) {
            toggleAutoScroll();
            return;
        }
        if (key == Canvas.KEY_NUM1) {
            changeSpeed(150);
            return;
        }
        if (key == Canvas.KEY_NUM3) {
            changeSpeed(-150);
            return;
        }
        if (key == Canvas.KEY_STAR) {
            st.scheme = (st.scheme + 1) % 3;
            applyScheme();
            return;
        }
        if (key == Canvas.KEY_POUND) {
            st.fontSize = (st.fontSize + 1) % 3;
            applyScheme();
            toast("Шрифт: " + (st.fontSize == 0 ? "мелкий" : st.fontSize == 1 ? "средний" : "крупный"));
            return;
        }
        // левая/правая софт-клавиши на S40 и SE
        if (key == -6 || key == -21) {
            midlet.showMenu();
            return;
        }
        if (key == -7 || key == -22) {
            midlet.closeBook();
            return;
        }
    }

    protected void keyRepeated(int key) {
        keyPressed(key);
    }

    /** Листание тапом по экрану — для сенсорных аппаратов вроде 5800 и китайских. */
    protected void pointerPressed(int x, int y) {
        if (y < getHeight() / 3) {
            pageUp();
        } else if (y > getHeight() * 2 / 3) {
            pageDown();
        } else {
            midlet.showMenu();
        }
    }
}
