package qltc;

import qltc.auth.Auth;
import qltc.db.DB;
import qltc.db.Sql;
import qltc.ui.LoginFrame;
import qltc.ui.Ui;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            try {
                DB.get().close();
                Auth.init();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Không khởi động được ứng dụng.\nKiểm tra file db.properties và MySQL.\n\n" + Sql.explain(e),
                        "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
