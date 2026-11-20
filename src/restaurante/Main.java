package restaurante;

import javax.swing.*;
import restaurante.modelo.Restaurante;
import restaurante.ui.VentanaPrincipal;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // si falla se queda con el look por defecto
            }
            VentanaPrincipal ventana = new VentanaPrincipal(Restaurante.demo());
            ventana.setVisible(true);
        });
    }
}
