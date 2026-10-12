package mdreader;

/**
 * Русские строки интерфейса.
 *
 * Язык выбирается на сборке: `build.bat` подаёт javac нужный файл из `i18n/<язык>/`.
 * Resource bundles из Java SE в CLDC 1.1 нет, а держать оба языка в JAR ради телефона
 * с мегабайтом кучи незачем — поэтому два отдельных билда.
 *
 * Все поля `static final String`: ProGuard подставляет их по месту, и класс
 * из собранного JAR исчезает целиком.
 *
 * ВАЖНО: набор полей в `i18n/ru` и `i18n/en` должен совпадать — если в одном языке
 * поля не хватает, сборка этого языка просто не скомпилируется. Это и есть проверка.
 */
final class Strings {

    private Strings() {
    }

    // Команды
    static final String OPEN = "Открыть";
    static final String UP = "Вверх";
    static final String BACK = "Назад";
    static final String EXIT = "Выход";
    static final String OK = "OK";

    // Обзор файлов
    static final String CHOOSE_FILE = "Выбор файла";
    static final String PHONE_MEMORY = "Память телефона";
    static final String ALL_DRIVES = "Все диски";
    static final String MERGE_DRIVES = "Объединить диски";
    static final String NO_FILE_ACCESS = "Нет доступа к файлам";
    static final String NO_ROOTS = "Корней не найдено";
    static final String EMPTY_DIR = "(пусто)";
    static final String NO_ACCESS = "Нет доступа: ";

    // Чтение
    static final String NO_BOOK = "Книга не открыта";
    static final String READ_ERROR = "Ошибка чтения";
    static final String END = "Конец";
    static final String START = "Начало";
    static final String NO_HEADING_FWD = "Заголовков дальше нет";
    static final String NO_HEADING_BACK = "Заголовков выше нет";
    static final String AUTOSCROLL_ON = "Автопрокрутка вкл";
    static final String AUTOSCROLL_OFF = "Автопрокрутка выкл";
    static final String SPEED = "Скорость: ";
    static final String MS = " мс";
    static final String FONT_IS = "Шрифт: ";
    static final String FONT_SMALL_LC = "мелкий";
    static final String FONT_MEDIUM_LC = "средний";
    static final String FONT_LARGE_LC = "крупный";

    // Меню
    static final String MENU = "Меню";
    static final String RESUME = "Продолжить чтение";
    static final String CONTENTS = "Содержание";
    static final String GOTO_PERCENT = "Перейти к %";
    static final String OPEN_OTHER = "Открыть другой файл";
    static final String SETTINGS = "Настройки";
    static final String AUTOSCROLL = "Автопрокрутка";
    static final String ABOUT = "О программе";
    static final String GOTO_TITLE = "Перейти к % (0-100)";
    static final String NO_HEADINGS = "Заголовков в файле нет";

    // Настройки
    static final String FONT = "Шрифт";
    static final String[] FONT_SIZES = { "Мелкий", "Средний", "Крупный" };
    static final String THEME = "Тема";
    static final String[] THEMES = { "День", "Ночь", "Сепия" };
    static final String ENCODING = "Кодировка";
    static final String[] ENC_NAMES = { "Авто", "UTF-8", "Windows-1251", "Latin-1" };
    static final String VIEW = "Вид";
    static final String[] VIEW_OPTIONS = { "Во весь экран", "Строка состояния", "Склеивать абзацы" };

    // Ошибки
    static final String ERROR = "Ошибка";
    static final String OPEN_FAILED = "Не удалось открыть файл: ";
    static final String FILE_NOT_FOUND = "Файл не найден";

    // О программе
    static final String ABOUT_DESC = "Читалка Markdown для телефонов на Java.\nВерсия ";
    static final String ABOUT_CREDITS = "Замысел и постановка задачи: hinducoder\nКод: Claude (Anthropic)\n\n";
    static final String ABOUT_SOURCE = "Исходники:\n";
    static final String ABOUT_KEYS =
        "2 / 8 — строка\n7 / 9 — страница\n4 / 6 — глава\n5 — меню\n"
        + "0 — автопрокрутка\n1 / 3 — скорость\n* — тема\n# — шрифт\n";
}
