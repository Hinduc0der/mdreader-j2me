package mdreader;

/**
 * English UI strings.
 *
 * The language is picked at build time: `build.bat` passes javac the right file from
 * `i18n/<lang>/`. CLDC 1.1 has no resource bundles, and shipping both languages inside
 * the JAR is pointless on a phone with a megabyte of heap — hence two separate builds.
 *
 * Every field is a `static final String`: ProGuard inlines them at the use site and the
 * class disappears from the built JAR entirely.
 *
 * IMPORTANT: the field set in `i18n/ru` and `i18n/en` must match. If a language is
 * missing a field, that language simply fails to compile — that is the check.
 */
final class Strings {

    private Strings() {
    }

    // Commands
    static final String OPEN = "Open";
    static final String UP = "Up";
    static final String BACK = "Back";
    static final String EXIT = "Exit";
    static final String OK = "OK";

    // File browser
    static final String CHOOSE_FILE = "Choose a file";
    static final String PHONE_MEMORY = "Phone memory";
    static final String ALL_DRIVES = "All drives";
    static final String MERGE_DRIVES = "Merge drives";
    static final String NO_FILE_ACCESS = "No access to files";
    static final String NO_ROOTS = "No roots found";
    static final String EMPTY_DIR = "(empty)";
    static final String NO_ACCESS = "No access: ";

    // Reading
    static final String NO_BOOK = "No book open";
    static final String READ_ERROR = "Read error";
    static final String END = "End";
    static final String START = "Start";
    static final String NO_HEADING_FWD = "No headings below";
    static final String NO_HEADING_BACK = "No headings above";
    static final String AUTOSCROLL_ON = "Auto-scroll on";
    static final String AUTOSCROLL_OFF = "Auto-scroll off";
    static final String SPEED = "Speed: ";
    static final String MS = " ms";
    static final String FONT_IS = "Font: ";
    static final String FONT_SMALL_LC = "small";
    static final String FONT_MEDIUM_LC = "medium";
    static final String FONT_LARGE_LC = "large";

    // Menu
    static final String MENU = "Menu";
    static final String RESUME = "Continue reading";
    static final String CONTENTS = "Contents";
    static final String GOTO_PERCENT = "Go to %";
    static final String OPEN_OTHER = "Open another file";
    static final String SETTINGS = "Settings";
    static final String AUTOSCROLL = "Auto-scroll";
    static final String ABOUT = "About";
    static final String GOTO_TITLE = "Go to % (0-100)";
    static final String NO_HEADINGS = "No headings in this file";

    // Settings
    static final String FONT = "Font";
    static final String[] FONT_SIZES = { "Small", "Medium", "Large" };
    static final String THEME = "Theme";
    static final String[] THEMES = { "Day", "Night", "Sepia" };
    static final String ENCODING = "Encoding";
    static final String[] ENC_NAMES = { "Auto", "UTF-8", "Windows-1251", "Latin-1" };
    static final String VIEW = "View";
    static final String[] VIEW_OPTIONS = { "Full screen", "Status bar", "Join paragraphs" };

    // Errors
    static final String ERROR = "Error";
    static final String OPEN_FAILED = "Could not open the file: ";
    static final String FILE_NOT_FOUND = "File not found";

    // About
    static final String ABOUT_DESC = "A Markdown reader for Java phones.\nVersion ";
    static final String ABOUT_CREDITS = "Idea and direction: hinducoder\nCode: Claude (Anthropic)\n\n";
    static final String ABOUT_SOURCE = "Source:\n";
    static final String ABOUT_KEYS =
        "2 / 8 - line\n7 / 9 - page\n4 / 6 - chapter\n5 - menu\n"
        + "0 - auto-scroll\n1 / 3 - speed\n* - theme\n# - font\n";
}
