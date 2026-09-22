package mdreader;

import java.util.Vector;

/**
 * Разбор списка корней файловой системы, отданного FileSystemRegistry.listRoots().
 *
 * Symbian (S60 3rd) выдаёт на каждый том ДВЕ записи: сначала локализованное имя,
 * сразу за ним букву диска — «Память тлф/», «C:/», «Карта памяти/», «E:/».
 * В обзоре это выглядит как два одинаковых диска. Склеиваем такую пару в один
 * пункт: подпись берём человеческую, а путь — по букве, она на Symbian каноническая
 * и открывается гарантированно.
 *
 * Аппараты, отдающие только буквы (S40, эмулятор WTK) или только имена, проходят
 * через тот же код без изменений: пара не складывается, пункт выводится как есть.
 *
 * Вынесено из FileBrowser отдельным классом и без зависимостей от lcdui, чтобы
 * алгоритм можно было прогнать на настольной Java — см. test/RootsTest.java.
 */
public final class Roots {

    private Roots() {
    }

    /**
     * @param raw     корни в том порядке, в каком их отдал listRoots()
     * @param showAll не склеивать, показать всё как есть
     * @return Vector из String[] { путь, подпись }
     */
    public static Vector merge(Vector raw, boolean showAll) {
        Vector out = new Vector();
        if (showAll) {
            for (int i = 0; i < raw.size(); i++) {
                String r = (String) raw.elementAt(i);
                out.addElement(new String[] { r, r });
            }
            return out;
        }
        int i = 0;
        while (i < raw.size()) {
            String cur = (String) raw.elementAt(i);
            String next = i + 1 < raw.size() ? (String) raw.elementAt(i + 1) : null;
            if (!isDriveRoot(cur) && next != null && isDriveRoot(next)) {
                out.addElement(new String[] { next, trimSlash(cur) });
                i += 2;
            } else {
                out.addElement(new String[] { cur, trimSlash(cur) });
                i++;
            }
        }
        return out;
    }

    /** «C:/» и «C:» — да, «Память тлф/» и «root1/» — нет. */
    public static boolean isDriveRoot(String root) {
        String p = trimSlash(root);
        if (p.length() != 2 || p.charAt(1) != ':') {
            return false;
        }
        char c = p.charAt(0);
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static String trimSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
