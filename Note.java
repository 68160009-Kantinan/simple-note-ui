/** ข้อมูลโน้ตหนึ่งใบ ใช้ร่วมกันทั้งสามส่วน */
public class Note {
    public final int id;
    public final String title;
    public final String content;

    public Note(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public String toString() { return title; }
}
