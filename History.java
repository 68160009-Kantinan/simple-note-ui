import java.util.Stack;

/** คนที่ 1: Stack + ขั้นตอน Undo/Redo แบบเก็บสถานะข้อความ */
public class History {
    private final Stack<String> undo = new Stack<String>();
    private final Stack<String> redo = new Stack<String>();

    public void record(String oldText) {
        undo.push(oldText);
        // เมื่อแก้ไขใหม่ เส้นทาง redo เดิมใช้ต่อไม่ได้
        redo.clear();
    }

    public String undo(String currentText) {
        if (undo.empty()) return currentText;
        redo.push(currentText);
        return undo.pop(); // LIFO: เอาสถานะที่ใส่ล่าสุดออกก่อน
    }

    public String redo(String currentText) {
        if (redo.empty()) return currentText;
        undo.push(currentText);
        return redo.pop();
    }

    public int undoCount() { return undo.size(); }
    public int redoCount() { return redo.size(); }
    public void clear() { undo.clear(); redo.clear(); }
}
