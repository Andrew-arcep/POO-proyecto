package restaurante.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import restaurante.modelo.EstadoMesa;
import restaurante.modelo.Mesa;
import restaurante.modelo.MetodoPago;
import restaurante.modelo.ObservadorRestaurante;
import restaurante.modelo.OpcionMenu;
import restaurante.modelo.Restaurante;
import restaurante.modelo.Visita;

// Pestaña de atencion de una mesa: ver el menu por categoria, hacer pedidos,
// ver la cuenta, registrar pagos y cerrar la visita.
public class PanelVisita extends JPanel implements ObservadorRestaurante {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Restaurante restaurante;
    private final JComboBox<Integer> cboMesa = new JComboBox<>();
    private final JLabel lblInfo = new JLabel(" ");
    private final JComboBox<String> cboCategoria = new JComboBox<>();
    private final JComboBox<String> cboComensal = new JComboBox<>();

    private final DefaultTableModel modeloMenu = Ui.modelo("Id", "Opción", "Descripción", "kcal", "Precio");
    private final DefaultTableModel modeloPedidos = Ui.modelo("#", "Comensal", "Producto", "Cant.", "Precio", "Subtotal", "Hora", "Estado");
    private final JTable tablaMenu = Ui.tabla(modeloMenu, 40, 150, 160, 70, 80);
    private final JTable tablaPedidos = Ui.tabla(modeloPedidos, 30, 80, 170, 45, 75, 80, 70, 150);

    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JTextArea txtResumen = new JTextArea(10, 28);

    private final JButton btnAgregar = new JButton("Agregar al pedido");
    private final JButton btnAnular = new JButton("Anular pedido");
    private final JButton btnCuenta = new JButton("Solicitar cuenta");
    private final JButton btnReabrir = new JButton("Volver a pedir");
    private final JButton btnPagar = new JButton("Registrar pago");
    private final JButton btnCerrar = new JButton("Cerrar visita");

    // mientras se recargan los combos no se deben disparar sus eventos
    private boolean cargando = false;

    public PanelVisita(Restaurante restaurante) {
        super(new BorderLayout(8, 8));
        this.restaurante = restaurante;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(Ui.fila(new JLabel("Mesa:"), cboMesa, lblInfo), BorderLayout.NORTH);

        // ---- lado izquierdo: menu para pedir ----
        JPanel izquierda = new JPanel(new BorderLayout(4, 4));
        izquierda.setBorder(BorderFactory.createTitledBorder("Menú (consulte calorías y precio antes de pedir)"));
        izquierda.add(Ui.fila(new JLabel("Categoría:"), cboCategoria), BorderLayout.NORTH);
        izquierda.add(new JScrollPane(tablaMenu), BorderLayout.CENTER);
        izquierda.add(Ui.fila(new JLabel("Comensal:"), cboComensal, new JLabel("Cantidad:"), spCantidad, btnAgregar), BorderLayout.SOUTH);

        // ---- lado derecho: pedidos y resumen de la cuenta ----
        txtResumen.setEditable(false);
        txtResumen.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel derecha = new JPanel(new BorderLayout(4, 4));
        derecha.setBorder(BorderFactory.createTitledBorder("Pedidos de la visita"));
        derecha.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);
        JPanel abajoDerecha = new JPanel(new BorderLayout());
        abajoDerecha.add(Ui.fila(btnAnular), BorderLayout.NORTH);
        abajoDerecha.add(new JScrollPane(txtResumen), BorderLayout.CENTER);
        derecha.add(abajoDerecha, BorderLayout.SOUTH);

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        division.setResizeWeight(0.45);
        add(division, BorderLayout.CENTER);
        add(Ui.fila(btnCuenta, btnReabrir, btnPagar, btnCerrar), BorderLayout.SOUTH);

        // ---- eventos ----
        cboMesa.addActionListener(e -> {
            if (!cargando) {
                cargarVisita();
            }
        });
        cboCategoria.addActionListener(e -> {
            if (!cargando) {
                cargarMenu();
            }
        });
        btnAgregar.addActionListener(e -> agregar());
        btnAnular.addActionListener(e -> anular());
        btnCuenta.addActionListener(e -> Ui.ejecutar(this, () -> restaurante.solicitarCuenta(mesaActual())));
        btnReabrir.addActionListener(e -> Ui.ejecutar(this, () -> restaurante.reabrirCuenta(mesaActual())));
        btnPagar.addActionListener(e -> pagar());
        btnCerrar.addActionListener(e -> {
            int respuesta = JOptionPane.showConfirmDialog(this, "¿Cerrar la visita y liberar la mesa?", "Cerrar visita", JOptionPane.YES_NO_OPTION);
            if (respuesta == JOptionPane.YES_OPTION) {
                Ui.ejecutar(this, () -> restaurante.cerrarVisita(mesaActual()));
            }
        });

        actualizar(0, "");
    }

    private int mesaActual() {
        if (cboMesa.getSelectedItem() == null) {
            throw new IllegalArgumentException("Seleccione una mesa con visita abierta.");
        }
        return (Integer) cboMesa.getSelectedItem();
    }

    // visita de la mesa elegida, o null si no hay
    private Visita visita() {
        if (cboMesa.getSelectedItem() == null) {
            return null;
        }
        return restaurante.mesa(mesaActual()).getVisita();
    }

    // lo llama la pestaña de mesas para abrir directo una mesa
    public void seleccionar(int idMesa) {
        actualizar(0, "");
        cboMesa.setSelectedItem(idMesa);
    }

    @Override
    public void actualizar(int mesa, String detalle) {
        cargando = true;
        Object mesaAnterior = cboMesa.getSelectedItem();
        Object categoriaAnterior = cboCategoria.getSelectedItem();

        // solo se pueden elegir mesas que tengan una visita abierta
        cboMesa.removeAllItems();
        for (Mesa m : restaurante.getMesas()) {
            if (!m.estaDisponible()) {
                cboMesa.addItem(m.getId());
            }
        }
        if (mesaAnterior != null) {
            cboMesa.setSelectedItem(mesaAnterior);
        }

        cboCategoria.removeAllItems();
        for (String categoria : restaurante.getMenu().getCategorias()) {
            cboCategoria.addItem(categoria);
        }
        if (categoriaAnterior != null) {
            cboCategoria.setSelectedItem(categoriaAnterior);
        }
        cargando = false;

        cargarMenu();
        cargarVisita();
    }

    private void cargarMenu() {
        modeloMenu.setRowCount(0);
        if (cboCategoria.getSelectedItem() == null) {
            return;
        }
        String categoria = (String) cboCategoria.getSelectedItem();
        for (OpcionMenu o : restaurante.getMenu().listarPorCategoria(categoria, true)) {
            modeloMenu.addRow(new Object[]{o.getId(), o.getNombre(), o.getDescripcion(),
                    o.getCalorias() + " kcal", Visita.colones(o.getPrecio())});
        }
    }

    private void cargarVisita() {
        Visita visita = visita();
        modeloPedidos.setRowCount(0);
        cboComensal.removeAllItems();

        boolean hayVisita = visita != null;
        boolean pendientePago = hayVisita && restaurante.mesa(mesaActual()).getEstado() == EstadoMesa.PENDIENTE_PAGO;

        // que botones se pueden usar depende del estado de la mesa
        btnAgregar.setEnabled(hayVisita && !pendientePago);
        btnAnular.setEnabled(hayVisita && !pendientePago);
        btnCuenta.setEnabled(hayVisita && !pendientePago);
        btnReabrir.setEnabled(pendientePago);
        btnPagar.setEnabled(pendientePago);
        btnCerrar.setEnabled(pendientePago);

        if (!hayVisita) {
            lblInfo.setText("No hay mesas ocupadas. Abra una visita en la pestaña Mesas.");
            txtResumen.setText("");
            return;
        }

        String modalidad = "Cuenta conjunta";
        if (visita.isPorPersona()) {
            modalidad = "Pagos individuales";
        }
        lblInfo.setText("Mesero: " + visita.getMesero() + "  |  Inicio: " + visita.getApertura().format(HORA)
                + "  |  " + modalidad + "  |  Estado: " + restaurante.mesa(mesaActual()).getEstado());

        for (String comensal : visita.getComensales()) {
            cboComensal.addItem(comensal);
        }

        for (Visita.Linea l : visita.getLineas()) {
            String estado = "Activo";
            if (l.isAnulada()) {
                estado = "ANULADO: " + l.getMotivoAnulacion();
            }
            modeloPedidos.addRow(new Object[]{l.getId(), l.getAlias(), l.getProducto(), l.getCantidad(),
                    Visita.colones(l.getPrecioAplicado()), Visita.colones(l.getSubtotal()),
                    l.getFechaHora().format(HORA), estado});
        }

        txtResumen.setText(armarResumen(visita));
        txtResumen.setCaretPosition(0);
    }

    // texto con el subtotal de cada persona, por categoria, totales y pagos
    private String armarResumen(Visita visita) {
        StringBuilder sb = new StringBuilder("SUBTOTAL POR PERSONA\n");
        for (String comensal : visita.getComensales()) {
            sb.append(String.format("  %-12s %10s", comensal, Visita.colones(visita.subtotalDe(comensal))));
            if (visita.isPorPersona()) {
                sb.append("  saldo ").append(Visita.colones(visita.saldoDe(comensal)));
            }
            sb.append('\n');
        }

        sb.append("POR CATEGORÍA\n");
        for (Map.Entry<String, Long> e : visita.subtotalPorCategoria().entrySet()) {
            sb.append(String.format("  %-12s %10s%n", e.getKey(), Visita.colones(e.getValue())));
        }

        sb.append(String.format("TOTAL MESA   %12s%n", Visita.colones(visita.totalMesa())));
        sb.append(String.format("PAGADO       %12s%n", Visita.colones(visita.totalPagado())));
        sb.append(String.format("SALDO        %12s%n", Visita.colones(visita.saldo())));

        for (Visita.Pago p : visita.getPagos()) {
            sb.append(String.format("  pago #%d %s %s ref %s%n", p.getId(), p.getMetodo(), Visita.colones(p.getMonto()), p.getReferencia()));
        }
        return sb.toString();
    }

    private void agregar() {
        int fila = tablaMenu.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una opción del menú.");
            return;
        }
        int idOpcion = (Integer) modeloMenu.getValueAt(tablaMenu.convertRowIndexToModel(fila), 0);
        String comensal = (String) cboComensal.getSelectedItem();
        int cantidad = (Integer) spCantidad.getValue();
        Ui.ejecutar(this, () -> restaurante.pedir(mesaActual(), comensal, idOpcion, cantidad));
    }

    private void anular() {
        int fila = tablaPedidos.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione el pedido a anular.");
            return;
        }
        int idPedido = (Integer) modeloPedidos.getValueAt(tablaPedidos.convertRowIndexToModel(fila), 0);
        String motivo = JOptionPane.showInputDialog(this, "Motivo de la anulación (queda en el registro):");
        if (motivo != null) {
            Ui.ejecutar(this, () -> restaurante.anularPedido(mesaActual(), idPedido, motivo));
        }
    }

    // ventana para registrar un pago (los pagos son ficticios)
    private void pagar() {
        Visita visita = visita();
        if (visita == null) {
            return;
        }
        boolean individual = visita.isPorPersona();

        JComboBox<String> cboPersona = new JComboBox<>();
        JComboBox<String> cboMetodo = new JComboBox<>(MetodoPago.NOMBRES.toArray(new String[0]));
        JLabel lblDato = new JLabel();
        JTextField txtDato = new JTextField(10);
        JTextField txtMonto = new JTextField(10);

        // solo salen las personas que todavia deben algo
        for (String comensal : visita.getComensales()) {
            if (visita.saldoDe(comensal) > 0) {
                cboPersona.addItem(comensal);
            }
        }

        // cuando se cambia el metodo o la persona se actualizan el dato que se pide y el monto sugerido
        Runnable alCambiar = () -> {
            String pista = MetodoPago.pista((String) cboMetodo.getSelectedItem());
            if (pista.isEmpty()) {
                lblDato.setText("Dato del método: (no aplica)");
                txtDato.setEnabled(false);
                txtDato.setText("");
            } else {
                lblDato.setText(pista + ":");
                txtDato.setEnabled(true);
            }
            if (individual && cboPersona.getSelectedItem() != null) {
                txtMonto.setText("" + visita.saldoDe((String) cboPersona.getSelectedItem()));
            }
        };
        cboMetodo.addActionListener(e -> alCambiar.run());
        cboPersona.addActionListener(e -> alCambiar.run());
        txtMonto.setText("" + visita.saldo());
        alCambiar.run();

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        formulario.add(new JLabel("Saldo de la mesa:"));
        formulario.add(new JLabel(Visita.colones(visita.saldo())));
        if (individual) {
            formulario.add(new JLabel("¿Quién paga?"));
            formulario.add(cboPersona);
        }
        formulario.add(new JLabel("Método de pago:"));
        formulario.add(cboMetodo);
        formulario.add(lblDato);
        formulario.add(txtDato);
        formulario.add(new JLabel("Monto (colones):"));
        formulario.add(txtMonto);

        // si hay un error se vuelve a mostrar la ventana
        while (JOptionPane.showConfirmDialog(this, formulario, "Registrar pago (ficticio)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            boolean ok = Ui.ejecutar(this, () -> {
                String quien = null;
                if (individual) {
                    quien = (String) cboPersona.getSelectedItem();
                }
                long monto = Ui.numero(txtMonto.getText(), "Monto");
                restaurante.pagar(mesaActual(), quien, (String) cboMetodo.getSelectedItem(), txtDato.getText(), monto);
            });
            if (ok) {
                return;
            }
        }
    }
}
