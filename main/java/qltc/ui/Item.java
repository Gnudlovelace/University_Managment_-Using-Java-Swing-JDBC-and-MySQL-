package qltc.ui;

/** Một lựa chọn trong combobox: value dùng cho SQL, label hiển thị. */
public record Item(String value, String label) {
    @Override public String toString() { return label; }
}
