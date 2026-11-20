package restaurante.ui;

import java.awt.BorderLayout;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import restaurante.modelo.EstadisticasServicio;
import restaurante.modelo.ObservadorRestaurante;
import restaurante.modelo.Restaurante;

// Pestaña de estadisticas: se elige la consulta, el rango de fechas y muestra la tabla
// con la explicacion de como se calculo.
public class PanelEstadisticas extends JPanel implements ObservadorRestaurante {

    private static final String GLOBAL = "(Global)";

    private final Restaurante restaurante;
    private final EstadisticasServicio estadisticas;
    private final JTextField txtDesde = new JTextField(9);
    private final JTextField txtHasta = new JTextField(9);
    private final JComboBox<String> cboConsulta = new JComboBox<>(new String[]{
            "Artículos más solicitados", "Ventas cobradas", "Consumo por categoría", "Visitas cerradas por mesero"});
    private final JComboBox<String> cboCategoria = new JComboBox<>();
    private final JTextArea txtExplicacion = new JTextArea(3, 40);
    private final JTable tabla = new JTable();
    private final JLabel lblEstado = new JLabel(" ");

    public PanelEstadisticas(Restaurante restaurante) {
        super(new BorderLayout(8, 8));
        this.restaurante = restaurante;
        this.estadisticas = new EstadisticasServicio(restaurante);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton btnConsultar = new JButton("Consultar");
        JButton btnHoy = new JButton("Hoy");

        add(Ui.fila(new JLabel("Consulta:"), cboConsulta,
                new JLabel("Categoría (solo para 'más solicitados'):"), cboCategoria,
                new JLabel("Desde (AAAA-MM-DD):"), txtDesde,
                new JLabel("Hasta:"), txtHasta,
                btnHoy, btnConsultar), BorderLayout.NORTH);

        tabla.setRowHeight(24);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        txtExplicacion.setEditable(false);
        txtExplicacion.setLineWrap(true);
        txtExplicacion.setWrapStyleWord(true);
        txtExplicacion.setBorder(BorderFactory.createTitledBorder("¿Cómo se calcula?"));
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(lblEstado, BorderLayout.NORTH);
        sur.add(txtExplicacion, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        btnHoy.addActionListener(e -> {
            txtDesde.setText(LocalDate.now().toString());
            txtHasta.setText(LocalDate.now().toString());
        });
        btnConsultar.addActionListener(e -> consultar());
        actualizar(0, "");
    }

    // recarga las categorias cuando cambia el menu
    @Override
    public void actualizar(int mesa, String detalle) {
        Object seleccionada = cboCategoria.getSelectedItem();
        cboCategoria.removeAllItems();
        cboCategoria.addItem(GLOBAL);
        for (String categoria : restaurante.getMenu().getCategorias()) {
            cboCategoria.addItem(categoria);
        }
        if (seleccionada != null) {
            cboCategoria.setSelectedItem(seleccionada);
        }
    }

    private void consultar() {
        Ui.ejecutar(this, () -> {
            LocalDate desde = Ui.fecha(txtDesde.getText(), "Desde");
            LocalDate hasta = Ui.fecha(txtHasta.getText(), "Hasta");

            String categoria = (String) cboCategoria.getSelectedItem();
            if (GLOBAL.equals(categoria)) {
                categoria = null;
            }

            EstadisticasServicio.Resultado resultado;
            int consulta = cboConsulta.getSelectedIndex();
            if (consulta == 0) {
                resultado = estadisticas.articulosMasSolicitados(categoria, desde, hasta);
            } else if (consulta == 1) {
                resultado = estadisticas.ventasCobradas(desde, hasta);
            } else if (consulta == 2) {
                resultado = estadisticas.ventasPorCategoria(desde, hasta);
            } else {
                resultado = estadisticas.visitasPorMesero(desde, hasta);
            }

            DefaultTableModel modelo = Ui.modelo(resultado.encabezados);
            for (String[] fila : resultado.filas) {
                modelo.addRow(fila);
            }
            tabla.setModel(modelo);
            txtExplicacion.setText(resultado.explicacion);

            if (resultado.filas.isEmpty()) {
                lblEstado.setText("No hay datos en el rango indicado.");
            } else {
                lblEstado.setText(resultado.titulo + ": " + resultado.filas.size() + " fila(s)");
            }
        });
    }
}
