import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/** คนที่ 2: Trie + Prefix Search (ค้นจากต้นชื่อ ไม่ใช่กลางชื่อ) */
public class TitleTrie {
    private static class Node {
        HashMap<Character, Node> children = new HashMap<Character, Node>();
        // เก็บ id ของโน้ตที่ผ่านโหนดนี้ ทำให้คืนผลโดยไม่ต้อง recursion
        ArrayList<Integer> noteIds = new ArrayList<Integer>();
    }

    private Node root = new Node();

    public void build(List<Note> notes) {
        root = new Node();
        // ลูปนอก: เพิ่มโน้ตทีละใบ
        for (Note note : notes) {
            Node current = root;
            current.noteIds.add(note.id);
            String title = note.title.toLowerCase(Locale.ROOT);
            // ลูปใน: เดินตามตัวอักษรของชื่อทีละตัว
            for (int i = 0; i < title.length(); i++) {
                char letter = title.charAt(i);
                Node next = current.children.get(letter);
                if (next == null) {
                    next = new Node();
                    current.children.put(letter, next);
                }
                current = next;
                current.noteIds.add(note.id);
            }
        }
    }

    public List<Integer> search(String prefix) {
        Node current = root;
        String text = prefix.toLowerCase(Locale.ROOT);
        for (int i = 0; i < text.length(); i++) {
            current = current.children.get(text.charAt(i));
            if (current == null) return new ArrayList<Integer>();
        }
        // คืนสำเนา ป้องกันผู้เรียกแก้ข้อมูลภายใน Trie
        return new ArrayList<Integer>(current.noteIds);
    }
}
