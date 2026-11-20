package restaurante.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import restaurante.modelo.EstadoMesa;
import restaurante.modelo.Mesa;
import restaurante.modelo.ObservadorRestaurante;
import restaurante.modelo.Restaurante;
import restaurante.modelo.Visita;

// Pestaña con las mesas como botones de colores:
// verde = disponible, naranja = ocupada, rojo = pendiente de pago.
public class PanelMesas extends JPanel implements ObservadorRestaurante {

    private final Restaurante restaurante;
    private final Consumer<Integer> irAVisita;
    private final JPanel rejilla = new JPanel(new GridLayout(0, 4, 10, 10));

    public PanelMesas(Restaurante restaurante, Consumer<Integer> irAVisita) {
        super(new BorderLayout(8, 8));
        this.restaurante = restaurante;
        this.irAVisita = irAVisita;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(new JLabel("<html><b>Mesas</b> — clic en una <font color='green'>disponible</font> para abrir una visita; clic en una ocupada para atenderla.</html>"), BorderLayout.NORTH);
        add(new JScrollPane(rejilla), BorderLayout.CENTER);
        actualizar(0, "");
    }

    // vuelve a dibujar todos los botones de las mesas
    @Override
    public void actualizar(int mesa, String detalle) {
        rejilla.removeAll();
        for (Mesa m : restaurante.getMesas()) {
            Visita visita = m.getVisita();
            String extra = "";
            if (visita != null) {
                extra = "<br>" + visita.getMesero() + " · " + visita.getComensales().size() + " pers.<br>"
                        + Visita.colones(visita.totalMesa());
            }

            JButton boton = new JButton("<html><center><b>Mesa " + m.getId() + "</b><br>" + m.getEstado() + extra + "</center></html>");
            boton.setPreferredSize(new Dimension(150, 110));
            boton.setOpaque(true);

            if (m.estaDisponible()) {
                boton.setBackground(new Color(0xB7E4C7));
            } else if (m.getEstado() == EstadoMesa.PENDIENTE_PAGO) {
                boton.setBackground(new Color(0xF4A6A6));
            } else {
                boton.setBackground(new Color(0xFFD59E));
            }

            int id = m.getId();
            boton.addActionListener(e -> {
                if (restaurante.mesa(id).estaDisponible()) {
                    abrir(id);
                } else {
                    irAVisita.accept(id);
                }
            });
            rejilla.add(boton);
        }
        rejilla.revalidate();
        rejilla.repaint();
    }

    // ventana para abrir una visita: mesero, cantidad de personas, alias y modalidad
    private void abrir(int idMesa) {
        JTextField txtMesero = new JTextField(14);
        JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        JComboBox<String> cboModalidad = new JComboBox<>(new String[]{"Cuenta conjunta", "Pagos individuales"});
        JPanel panelAliases = new JPanel(new GridLayout(0, 1, 2, 2));
        List<JTextField> camposAlias = new ArrayList<>();

        // crea un campo de texto por cada persona
        Runnable crearCampos = () -> {
            panelAliases.removeAll();
            camposAlias.clear();
            int cantidad = (Integer) spCantidad.getValue();
            for (int i = 1; i <= cantidad; i++) {
                JTextField campo = new JTextField("Persona " + i);
                campo.setBorder(BorderFactory.createTitledBorder("Alias del comensal " + i));
                camposAlias.add(campo);
                panelAliases.add(campo);
            }
            panelAliases.revalidate();
            panelAliases.repaint();
        };
        spCantidad.addChangeListener(e -> crearCampos.run());
        crearCampos.run();

        JPanel arriba = new JPanel(new GridLayout(0, 2, 6, 6));
        arriba.add(new JLabel("Mesero responsable:"));
        arriba.add(txtMesero);
        arriba.add(new JLabel("Cantidad de personas:"));
        arriba.add(spCantidad);
        arriba.add(new JLabel("Modalidad de cuenta:"));
        arriba.add(cboModalidad);

        JScrollPane scroll = new JScrollPane(panelAliases);
        scroll.setPreferredSize(new Dimension(320, 200));

        JPanel formulario = new JPanel(new BorderLayout(6, 6));
        formulario.add(arriba, BorderLayout.NORTH);
        formulario.add(scroll, BorderLayout.CENTER);

        // si hay un error la ventana se vuelve a mostrar para corregir los datos
        while (JOptionPane.showConfirmDialog(this, formulario, "Abrir visita en la mesa " + idMesa,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            List<String> aliases = new ArrayList<>();
            for (JTextField campo : camposAlias) {
                aliases.add(campo.getText());
            }
            boolean porPersona = cboModalidad.getSelectedIndex() == 1;
            boolean ok = Ui.ejecutar(this, () -> restaurante.abrirVisita(idMesa, txtMesero.getText(), aliases, porPersona));
            if (ok) {
                irAVisita.accept(idMesa);
                return;
            }
        }
    }
}
