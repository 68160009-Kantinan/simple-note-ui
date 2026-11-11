import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** ส่วนกลาง: บันทึกไฟล์โดยใช้ไลบรารี Java มาตรฐาน */
public class NoteStore {
    public static void save(Path path, List<Note> notes) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Properties data = new Properties();
        data.setProperty("count", "" + notes.size());
        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);
            data.setProperty(i + ".id", "" + n.id);
            data.setProperty(i + ".title", n.title);
            data.setProperty(i + ".content", n.content);
        }
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            data.store(writer, "Simplenote Prototype");
        }
        if (Files.exists(path)) {
            Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static List<Note> load(Path path) throws IOException {
        List<Note> notes = new ArrayList<Note>();
        Properties data = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            data.load(reader);
        }
        try {
            int count = Integer.parseInt(data.getProperty("count"));
            if (count < 0 || count > 100000) throw new IllegalArgumentException();
            java.util.HashSet<Integer> ids = new java.util.HashSet<Integer>();
            for (int i = 0; i < count; i++) {
                int id = Integer.parseInt(data.getProperty(i + ".id"));
                String title = data.getProperty(i + ".title");
                String content = data.getProperty(i + ".content");
                if (id < 1 || !ids.add(id) || title == null || title.trim().isEmpty()
                        || content == null) throw new IllegalArgumentException();
                notes.add(new Note(id, title, content));
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("รูปแบบไฟล์โน้ตไม่ถูกต้อง", e);
        }
        return notes;
    }
}
