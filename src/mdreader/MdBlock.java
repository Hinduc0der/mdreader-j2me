package mdreader;

/** Разобранная строка Markdown: тип блока, текст без разметки и стиль каждого символа. */
public class MdBlock {

    public static final int T_TEXT = 0;
    public static final int T_HEAD = 1;
    public static final int T_LIST = 2;
    public static final int T_QUOTE = 3;
    public static final int T_CODE = 4;
    public static final int T_HR = 5;
    public static final int T_EMPTY = 6;

    public static final byte S_BOLD = 1;
    public static final byte S_ITALIC = 2;
    public static final byte S_CODE = 4;
    public static final byte S_LINK = 8;

    public int type;
    /** Для заголовка — уровень 1..6, для списка — глубина вложенности. */
    public int level;
    public String text;
    public byte[] style;
    /** Маркер списка, рисуется перед первой строкой блока. */
    public String bullet;

    public boolean isBlank() {
        return type == T_EMPTY || text == null || text.length() == 0;
    }
}
