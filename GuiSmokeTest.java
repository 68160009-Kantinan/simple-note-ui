import javax.swing.*;
import java.lang.reflect.*;
import java.nio.file.*;

/** ตรวจการเชื่อมหน้าจอในโฟลเดอร์ทดสอบที่ไม่มี notes.properties */
public class GuiSmokeTest {
    private static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    private static Object call(Object o, String name) throws Exception {
        Method m = o.getClass().getDeclaredMethod(name); m.setAccessible(true); return m.invoke(o);
    }
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        Path dataFile = Paths.get(System.getProperty("simplenote.dataDir", "data"), "notes.properties");
        if (Files.exists(dataFile))
            throw new IllegalStateException("Run only in an empty test directory");
        SwingUtilities.invokeAndWait(() -> {
            SimplenoteApp app = new SimplenoteApp();
            try {
                JTextField q = (JTextField) field(app, "query");
                JComboBox<?> mode = (JComboBox<?>) field(app, "mode");
                DefaultListModel<?> model = (DefaultListModel<?>) field(app, "model");
                q.setText("ma"); check(model.size() == 2);
                mode.setSelectedIndex(1); q.setText("java stack"); check(model.size() == 1);
                q.setText(""); call(app, "newNote");
                ((JTextField) field(app,"title")).setText("ทดสอบ GUI");
                ((JTextArea) field(app,"editor")).setText("java test");
                check((Boolean) call(app,"saveNote"));
                check(NoteStore.load(dataFile).size() == 5);
                q.setText("java test"); check(model.size() == 1);
                q.setText("");
                JList<?> list = (JList<?>) field(app,"list");
                list.setSelectedIndex(0);
                JTextField title = (JTextField) field(app,"title");
                JTextArea editor = (JTextArea) field(app,"editor");
                title.setText("Renamed ไทย"); editor.setText("first line\nบรรทัดสอง");
                check((Boolean) call(app,"saveNote"));
                q.setText("renamed"); mode.setSelectedIndex(0); check(model.size() == 1);
                q.setText("java stack"); mode.setSelectedIndex(1); check(model.size() == 0);
                q.setText("");
                Method remove = app.getClass().getDeclaredMethod("removeNote",int.class);
                remove.setAccessible(true); remove.invoke(app,1);
                check(NoteStore.load(dataFile).size() == 4);
                SimplenoteApp reopened = new SimplenoteApp();
                try {
                    check(((DefaultListModel<?>) field(reopened,"model")).size() == 4);
                    check(Files.exists(dataFile.resolveSibling("notes.properties.bak")));
                } finally { reopened.dispose(); }
                System.out.println("PASS: GUI search, add, rename, multiline, reindex, delete, reopen, backup");
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { app.dispose(); }
        });
    }
}
