package mdreader;

import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

/** Точка входа и все экраны, кроме самого чтения. */
public class MDReaderMIDlet extends MIDlet implements CommandListener {

    private Display display;
    private Settings settings;
    private ReaderCanvas reader;
    private FileBrowser browser;

    private List menu;
    private List toc;
    private Vector tocOffsets;
    private TextBox gotoBox;
    private Form settingsForm;
    private ChoiceGroup cgFont;
    private ChoiceGroup cgScheme;
    private ChoiceGroup cgEnc;
    private ChoiceGroup cgView;

    private final Command cmdBack = new Command("Назад", Command.BACK, 2);
    private final Command cmdOk = new Command("OK", Command.OK, 1);

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
            settings = Settings.load();
            reader = new ReaderCanvas(this, settings);

            if (settings.lastFile != null && settings.lastFile.length() > 0) {
                if (openBook(settings.lastFile)) {
                    return;
                }
            }
            showBrowser();
        } else {
            display.setCurrent(reader.book() != null ? (Displayable) reader : (Displayable) browser);
        }
    }

    protected void pauseApp() {
        if (reader != null) {
            reader.stopAutoScroll();
            savePosition();
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (reader != null) {
            reader.stopAutoScroll();
            savePosition();
            if (reader.book() != null) {
                reader.book().close();
            }
        }
        if (settings != null) {
            settings.save();
        }
    }

    public void exit() {
        destroyApp(true);
        notifyDestroyed();
    }

    // ------------------------------------------------------------------ книги

    public boolean openBook(String url) {
        try {
            Book old = reader.book();
            if (old != null) {
                savePosition();
                old.close();
            }
            Book b = new Book(url, settings.encoding);
            long pos = settings.positionFor(url);
            reader.setBook(b, pos);
            settings.setPosition(url, pos < 0 ? b.start() : pos);
            settings.lastFile = url;
            settings.save();
            display.setCurrent(reader);
            return true;
        } catch (Throwable t) {
            error("Не удалось открыть файл: " + t.getMessage());
            return false;
        }
    }

    public void closeBook() {
        savePosition();
        showBrowser();
    }

    private void savePosition() {
        if (reader != null && reader.book() != null) {
            settings.setPosition(reader.book().url(), reader.position());
            settings.save();
        }
    }

    public void showBrowser() {
        if (browser == null) {
            browser = new FileBrowser(this, settings, settings.lastDir);
        }
        display.setCurrent(browser);
    }

    private void error(String msg) {
        Alert a = new Alert("Ошибка", msg, null, AlertType.ERROR);
        a.setTimeout(3000);
        display.setCurrent(a, browser != null ? (Displayable) browser : (Displayable) reader);
    }

    // ------------------------------------------------------------------ меню

    public void showMenu() {
        reader.stopAutoScroll();
        menu = new List("Меню", List.IMPLICIT);
        menu.append("Продолжить чтение", null);
        menu.append("Содержание", null);
        menu.append("Перейти к %", null);
        menu.append("Открыть другой файл", null);
        menu.append("Настройки", null);
        menu.append("Автопрокрутка", null);
        menu.append("О программе", null);
        menu.append("Выход", null);
        menu.addCommand(cmdBack);
        menu.setCommandListener(this);
        display.setCurrent(menu);
    }

    private void showToc() {
        Vector h = reader.headings();
        if (h.isEmpty()) {
            error("Заголовков в файле нет");
            return;
        }
        toc = new List("Содержание", List.IMPLICIT);
        tocOffsets = new Vector();
        for (int i = 0; i < h.size(); i++) {
            Object[] pair = (Object[]) h.elementAt(i);
            toc.append((String) pair[0], null);
            tocOffsets.addElement(pair[1]);
        }
        toc.addCommand(cmdBack);
        toc.setCommandListener(this);
        display.setCurrent(toc);
    }

    private void showGoto() {
        gotoBox = new TextBox("Перейти к % (0-100)", String.valueOf(reader.book().percent(reader.position())), 3, TextField.NUMERIC);
        gotoBox.addCommand(cmdOk);
        gotoBox.addCommand(cmdBack);
        gotoBox.setCommandListener(this);
        display.setCurrent(gotoBox);
    }

    private void showSettings() {
        settingsForm = new Form("Настройки");
        cgFont = new ChoiceGroup("Шрифт", Choice.EXCLUSIVE, new String[] { "Мелкий", "Средний", "Крупный" }, null);
        cgFont.setSelectedIndex(settings.fontSize, true);
        cgScheme = new ChoiceGroup("Тема", Choice.EXCLUSIVE, new String[] { "День", "Ночь", "Сепия" }, null);
        cgScheme.setSelectedIndex(settings.scheme, true);
        cgEnc = new ChoiceGroup("Кодировка", Choice.EXCLUSIVE, Text.ENC_NAMES, null);
        cgEnc.setSelectedIndex(settings.encoding, true);
        cgView = new ChoiceGroup("Вид", Choice.MULTIPLE, new String[] { "Во весь экран", "Строка состояния", "Склеивать абзацы" }, null);
        cgView.setSelectedIndex(0, settings.fullScreen);
        cgView.setSelectedIndex(1, settings.showStatus);
        cgView.setSelectedIndex(2, settings.joinLines);

        settingsForm.append(cgFont);
        settingsForm.append(cgScheme);
        settingsForm.append(cgEnc);
        settingsForm.append(cgView);
        settingsForm.addCommand(cmdOk);
        settingsForm.addCommand(cmdBack);
        settingsForm.setCommandListener(this);
        display.setCurrent(settingsForm);
    }

    private void applySettings() {
        settings.fontSize = cgFont.getSelectedIndex();
        settings.scheme = cgScheme.getSelectedIndex();
        int enc = cgEnc.getSelectedIndex();
        settings.fullScreen = cgView.isSelected(0);
        settings.showStatus = cgView.isSelected(1);
        settings.joinLines = cgView.isSelected(2);
        if (enc != settings.encoding && reader.book() != null) {
            settings.encoding = enc;
            reader.book().setEncoding(enc);
        } else {
            settings.encoding = enc;
        }
        settings.save();
        reader.applyScheme();
    }

    /** Ссылка на исходники. */
    private static final String HOMEPAGE = "github.com/Hinduc0der/mdreader-j2me";

    private void showAbout() {
        Form f = new Form("MD Reader");
        f.append("Читалка Markdown для телефонов на Java.\nВерсия " + version() + "\n\n");
        f.append("Замысел и постановка задачи: hinducoder\nКод: Claude (Anthropic)\n\n");
        if (HOMEPAGE.length() > 0) {
            f.append("Исходники:\n" + HOMEPAGE + "\n\n");
        }
        f.append("2 / 8 — строка\n7 / 9 — страница\n4 / 6 — глава\n5 — меню\n0 — автопрокрутка\n1 / 3 — скорость\n* — тема\n# — шрифт\n");
        f.addCommand(cmdBack);
        f.setCommandListener(this);
        display.setCurrent(f);
    }

    /** Версию берём из манифеста, чтобы не забывать править её в двух местах. */
    private String version() {
        String v = getAppProperty("MIDlet-Version");
        return v == null ? "?" : v;
    }

    // ------------------------------------------------------------------ команды

    public void commandAction(Command c, Displayable d) {
        if (d == menu && c == List.SELECT_COMMAND) {
            switch (menu.getSelectedIndex()) {
                case 0:
                    display.setCurrent(reader);
                    break;
                case 1:
                    showToc();
                    break;
                case 2:
                    showGoto();
                    break;
                case 3:
                    closeBook();
                    break;
                case 4:
                    showSettings();
                    break;
                case 5:
                    display.setCurrent(reader);
                    reader.startAutoScroll();
                    break;
                case 6:
                    showAbout();
                    break;
                default:
                    exit();
            }
            return;
        }
        if (d == toc && c == List.SELECT_COMMAND) {
            int i = toc.getSelectedIndex();
            if (i >= 0 && i < tocOffsets.size()) {
                reader.gotoOffset(((Long) tocOffsets.elementAt(i)).longValue());
            }
            display.setCurrent(reader);
            return;
        }
        if (d == gotoBox && c == cmdOk) {
            try {
                int pct = Integer.parseInt(gotoBox.getString().trim());
                if (pct < 0) {
                    pct = 0;
                }
                if (pct > 100) {
                    pct = 100;
                }
                reader.gotoPercent(pct);
            } catch (NumberFormatException e) {
                // ввели ерунду — просто возвращаемся
            }
            display.setCurrent(reader);
            return;
        }
        if (d == settingsForm && c == cmdOk) {
            applySettings();
            display.setCurrent(reader);
            return;
        }
        if (c == cmdBack) {
            display.setCurrent(reader.book() != null ? (Displayable) reader : (Displayable) browser);
        }
    }
}
