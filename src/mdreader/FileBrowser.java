package mdreader;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

/**
 * Обзор карты памяти через JSR-75. Показываем каталоги и файлы с текстовыми
 * расширениями — на телефоне ковыряться в сотнях чужих файлов незачем.
 */
public class FileBrowser extends List implements CommandListener {

    private static final String[] EXT = { ".md", ".markdown", ".mdown", ".txt" };

    private final MDReaderMIDlet midlet;
    private final Settings settings;
    private final Command cmdOpen = new Command(Strings.OPEN, Command.ITEM, 1);
    private final Command cmdUp = new Command(Strings.UP, Command.BACK, 2);
    private final Command cmdAllRoots = new Command(Strings.ALL_DRIVES, Command.SCREEN, 5);
    private final Command cmdFewRoots = new Command(Strings.MERGE_DRIVES, Command.SCREEN, 5);
    private final Command cmdExit = new Command(Strings.EXIT, Command.EXIT, 9);

    private String dir;
    private Vector entries = new Vector();

    public FileBrowser(MDReaderMIDlet midlet, Settings settings, String startDir) {
        super(Strings.CHOOSE_FILE, List.IMPLICIT);
        this.midlet = midlet;
        this.settings = settings;
        addCommand(cmdOpen);
        addCommand(cmdUp);
        addCommand(cmdExit);
        setCommandListener(this);
        setSelectCommand(cmdOpen);
        show(startDir == null ? "" : startDir);
    }

    public String currentDir() {
        return dir;
    }

    public void show(String path) {
        dir = path;
        deleteAll();
        entries.removeAllElements();

        if (path == null || path.length() == 0) {
            setTitle(Strings.PHONE_MEMORY);
            removeCommand(cmdAllRoots);
            removeCommand(cmdFewRoots);
            addCommand(showAllRoots() ? cmdFewRoots : cmdAllRoots);
            try {
                showRoots();
            } catch (Exception e) {
                append(Strings.NO_FILE_ACCESS, null);
            }
            if (entries.isEmpty()) {
                append(Strings.NO_ROOTS, null);
            }
            return;
        }

        removeCommand(cmdAllRoots);
        removeCommand(cmdFewRoots);
        setTitle(shortTitle(path));
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ);
            Vector dirs = new Vector();
            Vector files = new Vector();
            Enumeration e = fc.list("*", false);
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                if (name.endsWith("/")) {
                    dirs.addElement(name);
                } else if (accepted(name)) {
                    files.addElement(name);
                }
            }
            for (int i = 0; i < dirs.size(); i++) {
                String name = (String) dirs.elementAt(i);
                entries.addElement(path + name);
                append("[" + name.substring(0, name.length() - 1) + "]", null);
            }
            for (int i = 0; i < files.size(); i++) {
                String name = (String) files.elementAt(i);
                entries.addElement(path + name);
                append(name, null);
            }
            if (entries.isEmpty()) {
                append(Strings.EMPTY_DIR, null);
            }
        } catch (Exception ex) {
            append(Strings.NO_ACCESS + ex.getMessage(), null);
        } finally {
            try {
                if (fc != null) {
                    fc.close();
                }
            } catch (Exception ex) {
                // не страшно
            }
        }
    }

    /** Корни файловой системы; склейку дублей Symbian делает {@link Roots}. */
    private void showRoots() {
        Vector raw = new Vector();
        Enumeration roots = FileSystemRegistry.listRoots();
        while (roots.hasMoreElements()) {
            raw.addElement((String) roots.nextElement());
        }
        Vector items = Roots.merge(raw, showAllRoots());
        for (int i = 0; i < items.size(); i++) {
            String[] it = (String[]) items.elementAt(i);
            entries.addElement("file:///" + it[0]);
            append(it[1], null);
        }
    }

    private boolean showAllRoots() {
        return settings != null && settings.showAllRoots;
    }

    private static boolean accepted(String name) {
        String lower = name.toLowerCase();
        for (int i = 0; i < EXT.length; i++) {
            if (lower.endsWith(EXT[i])) {
                return true;
            }
        }
        return false;
    }

    private static String shortTitle(String path) {
        String p = path;
        if (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(slash + 1) : p;
    }

    private void goUp() {
        if (dir == null || dir.length() == 0) {
            return;
        }
        String p = dir;
        if (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        int slash = p.lastIndexOf('/');
        if (slash <= "file://".length()) {
            show("");
        } else {
            show(p.substring(0, slash + 1));
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == cmdExit) {
            midlet.exit();
            return;
        }
        if (c == cmdUp) {
            goUp();
            return;
        }
        if (c == cmdAllRoots || c == cmdFewRoots) {
            if (settings != null) {
                settings.showAllRoots = (c == cmdAllRoots);
                settings.save();
            }
            show("");
            return;
        }
        int i = getSelectedIndex();
        if (i < 0 || i >= entries.size()) {
            return;
        }
        String target = (String) entries.elementAt(i);
        if (target.endsWith("/")) {
            show(target);
        } else {
            midlet.openBook(target);
        }
    }
}
