import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** ส่วนกลาง: หน้าต่างเชื่อมฟีเจอร์ของสมาชิกทั้งสามคน */
public class SimplenoteApp extends JFrame {
    // ใช้ฟอนต์ที่มีอักษรไทยกับทุก control รวมถึงตอนเปิดผ่านเครื่องมือทดสอบ
    static { applyFonts(); }

    private static void applyFonts() {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, new javax.swing.plaf.FontUIResource("Tahoma", Font.PLAIN, 15));
            }
        }
    }
    private final List<Note> notes = new ArrayList<Note>();
    private final History history = new History();
    private final TitleTrie trie = new TitleTrie();
    private final WordIndex index = new WordIndex();
    private final DefaultListModel<Note> model = new DefaultListModel<Note>();
    private final JList<Note> list = new JList<Note>(model);
    private final JTextField title = new JTextField();
    private final JTextArea editor = new JTextArea();
    private final JTextField query = new JTextField(18);
    private final JTextArea detail = new JTextArea(4, 40);
    private final JLabel status = new JLabel("พร้อมใช้งาน");
    private final JComboBox<String> mode = new JComboBox<String>(
            new String[]{"ชื่อขึ้นต้น (Trie)", "ทุกคำต้องมี (AND)"});
    private final Path file = Paths.get(System.getProperty("simplenote.dataDir", "data"),
            "notes.properties").toAbsolutePath();
    private final JLabel count = new JLabel();
    private final JLabel editState = new JLabel(" ");
    private int selectedId = -1;
    private int nextId = 1;
    private boolean changing = false;
    private String previousText = "";

    public SimplenoteApp() {
        super("Simplenote UI — ทีมโอ่งมังกร");
        setSize(1120, 780);
        setMinimumSize(new Dimension(880, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(new Color(242, 246, 251));
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(new Color(30, 64, 114));
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        JLabel brand = new JLabel("Simplenote  /  สมุดโน้ตของเรา");
        brand.setFont(new Font("Tahoma", Font.BOLD, 23)); brand.setForeground(Color.WHITE);
        header.add(brand, BorderLayout.CENTER);
        JLabel badge = new JLabel("JAVA DESKTOP  •  ทีมโอ่งมังกร");
        badge.setForeground(new Color(218, 232, 255)); header.add(badge, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel sidebar = new JPanel(new BorderLayout(10, 12));
        sidebar.setBackground(new Color(234, 240, 249));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 16, 18, 16));
        JPanel search = new JPanel(new java.awt.GridLayout(0, 1, 0, 9)); search.setOpaque(false);
        search.add(new JLabel("ค้นหาโน้ต")); search.add(query); search.add(mode);
        sidebar.add(search, BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(54); list.setSelectionBackground(new Color(211, 229, 253));
        list.setSelectionForeground(new Color(22, 56, 100));
        list.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> source, Object value,
                    int position, boolean selected, boolean focus) {
                JLabel row = (JLabel) super.getListCellRendererComponent(source, value, position, selected, focus);
                row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
                return row;
            }
        });
        sidebar.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel sideBottom = new JPanel(new BorderLayout(0, 10)); sideBottom.setOpaque(false);
        sideBottom.add(count, BorderLayout.NORTH);
        JButton create = new JButton("+  สร้างโน้ตใหม่");
        create.addActionListener(e -> newNote()); sideBottom.add(create, BorderLayout.SOUTH);
        sidebar.add(sideBottom, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(12, 12));
        right.setBackground(Color.WHITE);
        right.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        JPanel editorTop = new JPanel(new BorderLayout(0, 12)); editorTop.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
        button(actions, "บันทึก  Ctrl+S", () -> saveNote());
        button(actions, "Undo", () -> restoreText(history.undo(editor.getText())));
        button(actions, "Redo", () -> restoreText(history.redo(editor.getText())));
        button(actions, "ลบโน้ต", () -> deleteNote());
        editorTop.add(actions, BorderLayout.NORTH);
        JPanel name = new JPanel(new BorderLayout());
        name.add(new JLabel("ชื่อโน้ต: "), BorderLayout.WEST);
        name.add(title, BorderLayout.CENTER);
        name.setOpaque(false); title.setFont(new Font("Tahoma", Font.BOLD, 19));
        editorTop.add(name, BorderLayout.CENTER);
        editState.setForeground(new Color(112, 79, 22));
        editorTop.add(editState, BorderLayout.SOUTH);
        right.add(editorTop, BorderLayout.NORTH);
        editor.setLineWrap(true); editor.setWrapStyleWord(true);
        editor.setMargin(new Insets(16, 16, 16, 16)); editor.setFont(new Font("Tahoma", Font.PLAIN, 17));
        right.add(new JScrollPane(editor), BorderLayout.CENTER);
        detail.setEditable(false); detail.setLineWrap(true); detail.setWrapStyleWord(true);
        detail.setBackground(new Color(241, 246, 252)); detail.setMargin(new Insets(10, 10, 10, 10));
        JPanel learning = new JPanel(new BorderLayout()); learning.setOpaque(false);
        JCheckBox explain = new JCheckBox("แสดงหลักการทำงานสำหรับนำเสนอ"); explain.setOpaque(false);
        JScrollPane explanation = new JScrollPane(detail); explanation.setVisible(false);
        explain.addActionListener(e -> {
            explanation.setVisible(explain.isSelected()); right.revalidate(); right.repaint();
        });
        learning.add(explain, BorderLayout.NORTH); learning.add(explanation, BorderLayout.CENTER);
        right.add(learning, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                sidebar, right);
        split.setDividerLocation(310); split.setResizeWeight(0.0); split.setBorder(null);
        add(split, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        status.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));

        // Listener เป็นโค้ดส่วนหน้าจอ ไม่ใช่อัลกอริทึมที่สมาชิกต้องนำเสนอ
        editor.getDocument().addDocumentListener(listener(() -> {
            if (!changing) {
                history.record(previousText);
                previousText = editor.getText();
                showHistory();
            }
        }));
        query.getDocument().addDocumentListener(listener(() -> refreshList()));
        title.getDocument().addDocumentListener(listener(() -> {
            if (!changing) showHistory();
        }));
        mode.addActionListener(e -> refreshList());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !changing && list.getSelectedValue() != null) {
                Note target = list.getSelectedValue();
                if (target.id != selectedId) {
                    if (allowLeave()) { openNote(target); selectCurrent(); }
                    else selectCurrent();
                }
            }
        });
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (allowLeave()) dispose();
            }
        });
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) notes.addAll(NoteStore.load(file));
            else {
                notes.add(new Note(1, "Java Stack", "java stack undo redo"));
                notes.add(new Note(2, "Java Queue", "java queue fifo"));
                notes.add(new Note(3, "Market", "milk bread"));
                notes.add(new Note(4, "Math", "matrix algebra"));
                NoteStore.save(file, notes);
            }
            for (Note n : notes) nextId = Math.max(nextId, n.id + 1);
            rebuild(); refreshList();
            if (!notes.isEmpty()) { openNote(notes.get(0)); selectCurrent(); }
            else newNote();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "เปิดไฟล์ไม่สำเร็จ: " + e.getMessage()
                    + "\nโปรแกรมจะปิดเพื่อไม่เขียนทับไฟล์เดิม");
            throw new IllegalStateException(e);
        }
        shortcut("save", "control S", () -> saveNote());
        shortcut("new", "control N", () -> newNote());
        shortcut("undo", "control Z", () -> restoreText(history.undo(editor.getText())));
        shortcut("redo", "control Y", () -> restoreText(history.redo(editor.getText())));
    }

    private void shortcut(String action, String key, Runnable callback) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), action);
        getRootPane().getActionMap().put(action, new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { callback.run(); }
        });
    }

    private static DocumentListener listener(Runnable action) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    private void button(JPanel panel, String label, Runnable action) {
        JButton b = new JButton(label);
        b.addActionListener(e -> action.run()); panel.add(b);
    }

    private Note current() {
        for (Note n : notes) if (n.id == selectedId) return n;
        return null;
    }

    private boolean dirty() {
        Note n = current();
        if (n == null) return !title.getText().isEmpty() || !editor.getText().isEmpty();
        return !n.title.equals(title.getText()) || !n.content.equals(editor.getText());
    }

    private boolean allowLeave() {
        if (!dirty()) return true;
        int answer = JOptionPane.showConfirmDialog(this,
                "มีการแก้ไขที่ยังไม่บันทึก ต้องการบันทึกหรือไม่?",
                "บันทึกโน้ต", JOptionPane.YES_NO_CANCEL_OPTION);
        if (answer == JOptionPane.YES_OPTION) return saveNote();
        return answer == JOptionPane.NO_OPTION;
    }

    private void selectCurrent() {
        changing = true;
        list.clearSelection();
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).id == selectedId) list.setSelectedIndex(i);
        }
        changing = false;
    }

    private void openNote(Note n) {
        changing = true;
        selectedId = n.id; title.setText(n.title); editor.setText(n.content);
        previousText = n.content; history.clear(); changing = false; showHistory();
    }

    private void newNote() {
        if (!allowLeave()) return;
        query.setText("");
        openNote(new Note(-1, "", "")); selectCurrent(); title.requestFocusInWindow();
    }

    private void deleteNote() {
        Note selected = current();
        if (selected == null) return;
        int answer = JOptionPane.showConfirmDialog(this, "ลบโน้ต “" + selected.title
                + "” หรือไม่? การลบนี้ไม่อยู่ในประวัติ Undo", "ลบโน้ต", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        removeNote(selected.id);
    }

    // แยกการลบจริงจากกล่องยืนยัน เพื่อทดสอบการบันทึกได้โดยไม่กดหน้าจอ
    private void removeNote(int id) {
        List<Note> remaining = new ArrayList<Note>();
        for (Note note : notes) if (note.id != id) remaining.add(note);
        if (!writeNotes(remaining)) return;
        notes.clear(); notes.addAll(remaining);
        openNote(new Note(-1, "", ""));
        rebuild(); refreshList();
        if (!notes.isEmpty()) { openNote(notes.get(0)); selectCurrent(); }
        status.setText("ลบโน้ตและบันทึกแล้ว");
    }

    private boolean writeNotes(List<Note> updated) {
        try { NoteStore.save(file, updated); return true; }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "บันทึกไม่สำเร็จ: " + e.getMessage()); return false;
        }
    }

    private boolean saveNote() {
        String name = title.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "กรุณาใส่ชื่อโน้ต"); return false;
        }
        int id = selectedId == -1 ? nextId : selectedId;
        Note updated = new Note(id, name, editor.getText());
        List<Note> copy = new ArrayList<Note>(notes);
        boolean replaced = false;
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i).id == id) { copy.set(i, updated); replaced = true; break; }
        }
        if (!replaced) copy.add(updated);
        if (!writeNotes(copy)) return false;
        notes.clear(); notes.addAll(copy);
        if (selectedId == -1) nextId++;
        selectedId = id; title.setText(name);
        rebuild(); refreshList(); selectCurrent();
        status.setText("บันทึกแล้ว: " + file); return true;
    }

    private void rebuild() { trie.build(notes); index.build(notes); }

    private void refreshList() {
        String q = query.getText().trim();
        List<Integer> ids = new ArrayList<Integer>();
        if (q.isEmpty()) { for (Note n : notes) ids.add(n.id); }
        else if (mode.getSelectedIndex() == 0) ids = trie.search(q);
        else ids = index.searchAnd(q);
        changing = true; model.clear();
        for (Note n : notes) if (ids.contains(n.id)) model.addElement(n);
        changing = false; selectCurrent();
        count.setText("แสดง " + model.size() + " / " + notes.size() + " โน้ต");
        String explanation = "ผลค้นหา " + model.size() + " ใบ (ค้นเฉพาะข้อมูลที่บันทึกแล้ว)\n";
        if (mode.getSelectedIndex() == 1 && !q.isEmpty()) {
            explanation += index.explain(q) + "AND → " + ids;
        } else explanation += "Trie: เดินจาก root ตามตัวอักษรคำขึ้นต้น → id " + ids;
        detail.setText(explanation);
    }

    private void restoreText(String text) {
        changing = true; editor.setText(text); previousText = text;
        changing = false; showHistory();
    }

    private void showHistory() {
        editState.setText(dirty() ? "มีการแก้ไขที่ยังไม่บันทึก" : "บันทึกครบแล้ว");
        status.setText("Undo Stack: " + history.undoCount() + " | Redo Stack: "
                + history.redoCount() + " | กดบันทึกโน้ตก่อนค้นหาข้อมูลใหม่");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }
            applyFonts();
            new SimplenoteApp().setVisible(true);
        });
    }
}
