package restaurante.ui;

import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import restaurante.modelo.ObservadorRestaurante;
import restaurante.modelo.Restaurante;

// Ventana principal, tiene una pestaña por cada parte del sistema.
// Todos los paneles se registran como observadores del restaurante para refrescarse solos.
public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal(Restaurante restaurante) {
        super("Sistema de atención y menú inteligente");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane pestanas = new JTabbedPane();

        PanelVisita panelVisita = new PanelVisita(restaurante);
        // al hacer clic en una mesa ocupada se cambia a la pestaña de atencion
        PanelMesas panelMesas = new PanelMesas(restaurante, idMesa -> {
            panelVisita.seleccionar(idMesa);
            pestanas.setSelectedComponent(panelVisita);
        });
        PanelMenu panelMenu = new PanelMenu(restaurante);
        PanelCombinaciones panelCombinaciones = new PanelCombinaciones(restaurante);
        PanelEstadisticas panelEstadisticas = new PanelEstadisticas(restaurante);

        // la bitacora tambien es un observador
        JTextArea areaBitacora = new JTextArea();
        areaBitacora.setEditable(false);
        areaBitacora.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ObservadorRestaurante bitacora = (mesa, detalle) -> {
            areaBitacora.setText(String.join("\n", restaurante.getBitacora()));
            areaBitacora.setCaretPosition(areaBitacora.getDocument().getLength());
        };

        restaurante.agregarObservador(panelMesas);
        restaurante.agregarObservador(panelVisita);
        restaurante.agregarObservador(panelMenu);
        restaurante.agregarObservador(panelCombinaciones);
        restaurante.agregarObservador(panelEstadisticas);
        restaurante.agregarObservador(bitacora);

        pestanas.addTab("Mesas", panelMesas);
        pestanas.addTab("Atención de mesa", panelVisita);
        pestanas.addTab("Menú y búsquedas", panelMenu);
        pestanas.addTab("Combinaciones por calorías", panelCombinaciones);
        pestanas.addTab("Estadísticas", panelEstadisticas);
        pestanas.addTab("Bitácora", new JScrollPane(areaBitacora));
        add(pestanas);

        setSize(1150, 700);
        setLocationRelativeTo(null);
    }
}
