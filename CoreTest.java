import java.nio.file.*;
import java.util.*;

/** ทดสอบพฤติกรรมจริง ไม่ต้องเปิดหน้าต่าง */
public class CoreTest {
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("PASS: " + name);
    }
    public static void main(String[] args) throws Exception {
        History h = new History();
        h.record("A"); h.record("AB");
        check(h.undo("ABC").equals("AB"), "undo latest");
        check(h.undo("AB").equals("A"), "undo twice");
        check(h.redo("A").equals("AB"), "redo");
        h.record("AB"); check(h.redoCount() == 0, "new edit clears redo");
        h.clear(); check(h.undo("X").equals("X"), "empty undo");
        List<Note> notes = Arrays.asList(new Note(1,"Java Stack","java stack stack"),
                new Note(2,"Java Queue","java queue"), new Note(3,"ภาษาไทย","ทดสอบ โน้ต"),
                new Note(4,"Java Stack","duplicate title"));
        TitleTrie t = new TitleTrie(); t.build(notes);
        check(t.search("JAVA").equals(Arrays.asList(1,2,4)), "prefix and duplicate titles");
        check(t.search("stack").isEmpty(), "prefix excludes middle");
        check(t.search("ภาษา").equals(Arrays.asList(3)), "Thai prefix");
        WordIndex w = new WordIndex(); w.build(notes);
        check(w.searchAnd("JAVA, stack").equals(Arrays.asList(1,4)), "AND and punctuation");
        check(w.searchAnd("java missing").isEmpty(), "missing term");
        check(w.searchAnd("stack stack").equals(Arrays.asList(1,4)), "repeated query term");
        check(w.searchAnd("ทดสอบ").equals(Arrays.asList(3)), "Thai token");
        List<Note> renamed = Arrays.asList(new Note(1,"New","changed"));
        t.build(renamed); w.build(renamed);
        check(t.search("java").isEmpty() && w.searchAnd("stack").isEmpty(), "rebuild removes stale entries");
        Path p = Files.createTempFile("mini-notes-test", ".properties");
        try {
            List<Note> special = Arrays.asList(new Note(9,"โน้ต = ไทย","บรรทัด 1\nline 2 : = \\"));
            NoteStore.save(p, special); Note n = NoteStore.load(p).get(0);
            check(n.id == 9 && n.title.equals(special.get(0).title)
                    && n.content.equals(special.get(0).content), "UTF-8 persistence round trip");
            Files.write(p, "count=1\n0.id=1\n".getBytes("UTF-8"));
            boolean rejected = false;
            try { NoteStore.load(p); } catch (java.io.IOException e) { rejected = true; }
            check(rejected, "reject damaged file");
        } finally { Files.deleteIfExists(p); }
        System.out.println("ALL TESTS PASSED");
    }
}
