import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** คนที่ 3: Inverted Index + Boolean AND ด้วย Set Intersection */
public class WordIndex {
    private final HashMap<String, Set<Integer>> index =
            new HashMap<String, Set<Integer>>();

    public static String[] words(String text) {
        // เวอร์ชันสาธิต: คำคั่นด้วยช่องว่างหรือเครื่องหมายวรรคตอน
        // ภาษาไทยที่เขียนติดกันทั้งประโยคจะเป็นหนึ่งคำ ไม่ใช่ระบบตัดคำไทย
        return text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{M}\\p{N}]+");
    }

    public void build(List<Note> notes) {
        index.clear();
        for (Note note : notes) {
            String[] terms = words(note.title + " " + note.content);
            for (String term : terms) {
                if (term.isEmpty()) continue;
                Set<Integer> ids = index.get(term);
                if (ids == null) {
                    ids = new LinkedHashSet<Integer>();
                    index.put(term, ids);
                }
                ids.add(note.id); // Set ทำให้คำซ้ำในโน้ตเดียวไม่เพิ่ม id ซ้ำ
            }
        }
    }

    public List<Integer> searchAnd(String query) {
        Set<Integer> result = null;
        for (String term : words(query)) {
            if (term.isEmpty()) continue;
            Set<Integer> found = index.get(term);
            if (found == null) return new ArrayList<Integer>();
            if (result == null) {
                result = new LinkedHashSet<Integer>(found);
            } else {
                // หาจุดร่วม: เก็บเฉพาะโน้ตที่มีคำก่อนหน้าและคำปัจจุบัน
                Set<Integer> common = new LinkedHashSet<Integer>();
                for (int id : result) {
                    if (found.contains(id)) common.add(id);
                }
                result = common;
            }
        }
        if (result == null) return new ArrayList<Integer>();
        return new ArrayList<Integer>(result);
    }

    public String explain(String query) {
        StringBuilder text = new StringBuilder();
        for (String term : words(query)) {
            if (!term.isEmpty()) {
                Set<Integer> ids = index.get(term);
                text.append(term).append(" → ").append(ids == null ? "[]" : ids);
                text.append("\n");
            }
        }
        return text.toString();
    }
}
