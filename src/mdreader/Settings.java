package mdreader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;

import javax.microedition.rms.RecordStore;

/**
 * Настройки и позиции чтения. Всё лежит одной записью в RMS: на старых аппаратах
 * запись в хранилище небыстрая, и дробить её незачем.
 */
public class Settings {

    private static final String STORE = "mdreader";
    private static final int MAX_RECENT = 20;

    public int fontSize = 1;      // 0 мелкий, 1 средний, 2 крупный
    public int scheme = 0;        // 0 день, 1 ночь, 2 сепия
    public int encoding = Text.ENC_AUTO;
    public int scrollDelay = 900; // мс на строку при автопрокрутке
    public boolean fullScreen = true;
    public boolean showStatus = true;
    public boolean joinLines = true;   // склеивать строки одного абзаца
    public boolean showAllRoots = false; // показывать корни как есть, без склейки дублей
    public String lastDir = "";
    public String lastFile = "";

    private final Vector recentUrls = new Vector();
    private final Vector recentPos = new Vector();

    public static Settings load() {
        Settings s = new Settings();
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
                int ver = in.readByte(); // версия формата
                s.fontSize = in.readByte();
                s.scheme = in.readByte();
                s.encoding = in.readByte();
                s.scrollDelay = in.readInt();
                s.fullScreen = in.readBoolean();
                s.showStatus = in.readBoolean();
                s.joinLines = in.readBoolean();
                s.lastDir = in.readUTF();
                s.lastFile = in.readUTF();
                int n = in.readByte();
                for (int i = 0; i < n; i++) {
                    s.recentUrls.addElement(in.readUTF());
                    s.recentPos.addElement(new Long(in.readLong()));
                }
                // Поля версии 2 дописаны в конец записи: запись от 0.1.0 читается как есть.
                if (ver >= 2) {
                    s.showAllRoots = in.readBoolean();
                }
            }
        } catch (Exception e) {
            // первый запуск или битая запись — живём со значениями по умолчанию
        } finally {
            close(rs);
        }
        return s;
    }

    public void save() {
        RecordStore rs = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeByte(2);
            out.writeByte(fontSize);
            out.writeByte(scheme);
            out.writeByte(encoding);
            out.writeInt(scrollDelay);
            out.writeBoolean(fullScreen);
            out.writeBoolean(showStatus);
            out.writeBoolean(joinLines);
            out.writeUTF(lastDir == null ? "" : lastDir);
            out.writeUTF(lastFile == null ? "" : lastFile);
            int n = recentUrls.size();
            out.writeByte(n);
            for (int i = 0; i < n; i++) {
                out.writeUTF((String) recentUrls.elementAt(i));
                out.writeLong(((Long) recentPos.elementAt(i)).longValue());
            }
            out.writeBoolean(showAllRoots);
            out.flush();
            byte[] data = bos.toByteArray();

            rs = RecordStore.openRecordStore(STORE, true);
            if (rs.getNumRecords() > 0) {
                rs.setRecord(1, data, 0, data.length);
            } else {
                rs.addRecord(data, 0, data.length);
            }
        } catch (Exception e) {
            // не сохранилось — не повод падать
        } finally {
            close(rs);
        }
    }

    public long positionFor(String url) {
        int i = recentUrls.indexOf(url);
        return i < 0 ? -1L : ((Long) recentPos.elementAt(i)).longValue();
    }

    public void setPosition(String url, long off) {
        int i = recentUrls.indexOf(url);
        if (i >= 0) {
            recentUrls.removeElementAt(i);
            recentPos.removeElementAt(i);
        }
        recentUrls.insertElementAt(url, 0);
        recentPos.insertElementAt(new Long(off), 0);
        while (recentUrls.size() > MAX_RECENT) {
            recentUrls.removeElementAt(recentUrls.size() - 1);
            recentPos.removeElementAt(recentPos.size() - 1);
        }
        lastFile = url;
    }

    public Vector recent() {
        return recentUrls;
    }

    private static void close(RecordStore rs) {
        try {
            if (rs != null) {
                rs.closeRecordStore();
            }
        } catch (Exception e) {
            // закрываем, ошибка тут ни на что не влияет
        }
    }
}
