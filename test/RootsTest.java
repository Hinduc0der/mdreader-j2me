import java.util.Vector;

import mdreader.Roots;

/**
 * Проверка склейки корней. Гоняет НАСТОЯЩИЙ mdreader.Roots (он не зависит от lcdui),
 * поэтому тест ловит расхождение с телефоном, а не с копией алгоритма.
 *
 * Запуск:
 *   javac -d build\test src\mdreader\Roots.java test\RootsTest.java
 *   java -cp build\test RootsTest
 */
public class RootsTest {

    private static int failed = 0;

    public static void main(String[] args) {
        // Nokia E90, S60 3rd FP1 — то, что видно на фото с телефона
        check("E90: имя+буква на каждый том",
                new String[] { "Память тлф/", "C:/", "Карта памяти/", "E:/" }, false,
                new String[] { "C:/|Память тлф", "E:/|Карта памяти" });

        // Nokia 5300 и прочие S40 — только буквы, склеивать нечего
        check("S40: только буквы",
                new String[] { "C:/", "E:/" }, false,
                new String[] { "C:/|C:", "E:/|E:" });

        // Эмулятор WTK
        check("WTK: один корень root1/",
                new String[] { "root1/" }, false,
                new String[] { "root1/|root1" });

        // Режим «Все диски» — ничего не трогаем
        check("показать всё как есть",
                new String[] { "Память тлф/", "C:/" }, true,
                new String[] { "Память тлф/|Память тлф/", "C:/|C:/" });

        // Буква без пары в конце списка не должна потеряться
        check("непарная буква в хвосте",
                new String[] { "Память тлф/", "C:/", "Z:/" }, false,
                new String[] { "C:/|Память тлф", "Z:/|Z:" });

        // Имя без буквы за ним тоже остаётся
        check("имя без буквы",
                new String[] { "Память тлф/", "Карта памяти/" }, false,
                new String[] { "Память тлф/|Память тлф", "Карта памяти/|Карта памяти" });

        check("пустой список", new String[0], false, new String[0]);

        if (failed == 0) {
            System.out.println("\nВсе проверки пройдены");
        } else {
            System.out.println("\nПровалено проверок: " + failed);
            System.exit(1);
        }
    }

    private static void check(String name, String[] raw, boolean showAll, String[] expected) {
        Vector in = new Vector();
        for (int i = 0; i < raw.length; i++) {
            in.addElement(raw[i]);
        }
        Vector out = Roots.merge(in, showAll);

        String[] got = new String[out.size()];
        for (int i = 0; i < out.size(); i++) {
            String[] it = (String[]) out.elementAt(i);
            got[i] = it[0] + "|" + it[1];
        }

        if (join(got).equals(join(expected))) {
            System.out.println("OK   " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name);
            System.out.println("     ждали:  " + join(expected));
            System.out.println("     вышло:  " + join(got));
        }
    }

    private static String join(String[] a) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append("  ");
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }
}
