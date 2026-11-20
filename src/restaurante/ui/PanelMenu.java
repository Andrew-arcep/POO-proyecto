package restaurante.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import restaurante.modelo.Menu;
import restaurante.modelo.ObservadorRestaurante;
import restaurante.modelo.OpcionMenu;
import restaurante.modelo.Restaurante;
import restaurante.modelo.Visita;

// Pestaña del menu: agregar, modificar y eliminar opciones, y buscar con varios criterios a la vez.
public class PanelMenu extends JPanel implements ObservadorRestaurante {

    private static final String TODAS = "(Todas)";

    private final Restaurante restaurante;
    private final JTextField txtTexto = new JTextField(10);
    private final JTextField txtPrecioMin = new JTextField(5);
    private final JTextField txtPrecioMax = new JTextField(5);
    private final JTextField txtCalMin = new JTextField(4);
    private final JTextField txtCalMax = new JTextField(4);
    private final JComboBox<String> cboCategoria = new JComboBox<>();
    private final JCheckBox chkDisponibles = new JCheckBox("Solo disponibles");
    private final DefaultTableModel modelo = Ui.modelo("Id", "Nombre", "Categoría", "Descripción", "kcal", "Precio", "Disponible");
    private final JTable tabla = Ui.tabla(modelo, 40, 180, 110, 220, 60, 80, 80);
    private final JLabel lblCriterios = new JLabel(" ");

    public PanelMenu(Restaurante restaurante) {
        super(new BorderLayout(8, 8));
        this.restaurante = restaurante;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ---- parte de arriba: busqueda ----
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        busqueda.setBorder(BorderFactory.createTitledBorder("Buscar (los criterios se combinan con Y)"));
        JButton btnBuscar = new JButton("Buscar");
        JButton btnLimpiar = new JButton("Limpiar");
        busqueda.add(new JLabel("Nombre contiene:"));
        busqueda.add(txtTexto);
        busqueda.add(new JLabel("Categoría:"));
        busqueda.add(cboCategoria);
        busqueda.add(new JLabel("Precio:"));
        busqueda.add(txtPrecioMin);
        busqueda.add(new JLabel("a"));
        busqueda.add(txtPrecioMax);
        busqueda.add(new JLabel("kcal:"));
        busqueda.add(txtCalMin);
        busqueda.add(new JLabel("a"));
        busqueda.add(txtCalMax);
        busqueda.add(chkDisponibles);
        busqueda.add(btnBuscar);
        busqueda.add(btnLimpiar);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(busqueda, BorderLayout.CENTER);
        norte.add(lblCriterios, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // ---- parte de abajo: botones ----
        JButton btnAgregar = new JButton("Agregar opción");
        JButton btnModificar = new JButton("Modificar");
        JButton btnActivar = new JButton("Activar / desactivar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnNuevaCategoria = new JButton("Nueva categoría");
        JButton btnCategoria = new JButton("Activar/desactivar categoría");
        add(Ui.fila(btnAgregar, btnModificar, btnActivar, btnEliminar, btnNuevaCategoria, btnCategoria), BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> buscar());
        btnLimpiar.addActionListener(e -> limpiar());
        btnAgregar.addActionListener(e -> mostrarFormulario(null));

        btnModificar.addActionListener(e -> {
            OpcionMenu opcion = seleccionada();
            if (opcion != null) {
                mostrarFormulario(opcion);
            }
        });

        btnActivar.addActionListener(e -> {
            OpcionMenu opcion = seleccionada();
            if (opcion != null) {
                Ui.ejecutar(this, () -> restaurante.setDisponible(opcion.getId(), !opcion.isDisponible()));
            }
        });

        btnEliminar.addActionListener(e -> {
            OpcionMenu opcion = seleccionada();
            if (opcion == null) {
                return;
            }
            int respuesta = JOptionPane.showConfirmDialog(this, "¿Eliminar '" + opcion.getNombre() + "'?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (respuesta == JOptionPane.YES_OPTION) {
                Ui.ejecutar(this, () -> restaurante.eliminarOpcion(opcion.getId()));
            }
        });

        btnNuevaCategoria.addActionListener(e -> {
            String nombre = JOptionPane.showInputDialog(this, "Nombre de la nueva categoría:");
            if (nombre != null) {
                Ui.ejecutar(this, () -> restaurante.agregarCategoria(nombre));
            }
        });

        btnCategoria.addActionListener(e -> {
            JComboBox<String> combo = new JComboBox<>(restaurante.getMenu().getCategorias().toArray(new String[0]));
            int respuesta = JOptionPane.showConfirmDialog(this, combo, "Categoría a activar/desactivar", JOptionPane.OK_CANCEL_OPTION);
            if (respuesta == JOptionPane.OK_OPTION) {
                String categoria = (String) combo.getSelectedItem();
                Ui.ejecutar(this, () -> restaurante.activarCategoria(categoria, !restaurante.getMenu().esActiva(categoria)));
            }
        });

        actualizar(0, "");
    }

    private void limpiar() {
        txtTexto.setText("");
        txtPrecioMin.setText("");
        txtPrecioMax.setText("");
        txtCalMin.setText("");
        txtCalMax.setText("");
        chkDisponibles.setSelected(false);
        cboCategoria.setSelectedIndex(0);
        buscar();
    }

    // devuelve la opcion de la fila seleccionada (o null si no hay ninguna)
    private OpcionMenu seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una opción de la tabla.");
            return null;
        }
        // convertRowIndexToModel por si la tabla esta ordenada
        int filaModelo = tabla.convertRowIndexToModel(fila);
        int id = (Integer) modelo.getValueAt(filaModelo, 0);
        return restaurante.getMenu().obtener(id);
    }

    @Override
    public void actualizar(int mesa, String detalle) {
        Object seleccionada = cboCategoria.getSelectedItem();
        cboCategoria.removeAllItems();
        cboCategoria.addItem(TODAS);
        for (String categoria : restaurante.getMenu().getCategorias()) {
            cboCategoria.addItem(categoria);
        }
        if (seleccionada != null) {
            cboCategoria.setSelectedItem(seleccionada);
        }
        buscar();
    }

    // arma la lista de criterios segun lo que haya escrito el usuario y muestra los resultados
    private void buscar() {
        List<Predicate<OpcionMenu>> criterios = new ArrayList<>();
        List<String> textos = new ArrayList<>(); // para mostrar que criterios se aplicaron

        boolean ok = Ui.ejecutar(this, () -> {
            String categoria = (String) cboCategoria.getSelectedItem();
            if (categoria != null && !categoria.equals(TODAS)) {
                criterios.add(Menu.porCategoria(categoria));
                textos.add("categoría = " + categoria);
            }

            if (!txtTexto.getText().isBlank()) {
                criterios.add(Menu.porTexto(txtTexto.getText()));
                textos.add("nombre contiene '" + txtTexto.getText().trim() + "'");
            }

            if (!txtPrecioMin.getText().isBlank() || !txtPrecioMax.getText().isBlank()) {
                long min = 0;
                long max = Long.MAX_VALUE;
                if (!txtPrecioMin.getText().isBlank()) {
                    min = Ui.numero(txtPrecioMin.getText(), "Precio mínimo");
                }
                if (!txtPrecioMax.getText().isBlank()) {
                    max = Ui.numero(txtPrecioMax.getText(), "Precio máximo");
                }
                criterios.add(Menu.porPrecio(min, max));
                String hasta = "∞";
                if (max != Long.MAX_VALUE) {
                    hasta = "" + max;
                }
                textos.add("precio entre " + min + " y " + hasta);
            }

            if (!txtCalMin.getText().isBlank() || !txtCalMax.getText().isBlank()) {
                int min = 0;
                int max = Integer.MAX_VALUE;
                if (!txtCalMin.getText().isBlank()) {
                    min = Ui.entero(txtCalMin.getText(), "Calorías mínimas");
                }
                if (!txtCalMax.getText().isBlank()) {
                    max = Ui.entero(txtCalMax.getText(), "Calorías máximas");
                }
                criterios.add(Menu.porCalorias(min, max));
                String hasta = "∞";
                if (max != Integer.MAX_VALUE) {
                    hasta = "" + max;
                }
                textos.add("calorías entre " + min + " y " + hasta);
            }

            if (chkDisponibles.isSelected()) {
                criterios.add(Menu.soloDisponibles());
                textos.add("solo disponibles");
            }
        });
        if (!ok) {
            return;
        }

        modelo.setRowCount(0);
        for (OpcionMenu o : restaurante.getMenu().buscar(criterios)) {
            String disponible = "No";
            if (o.isDisponible()) {
                disponible = "Sí";
            }
            modelo.addRow(new Object[]{o.getId(), o.getNombre(), o.getCategoria(), o.getDescripcion(),
                    o.getCalorias(), Visita.colones(o.getPrecio()), disponible});
        }

        String aplicados = "ninguno (se muestra todo)";
        if (!textos.isEmpty()) {
            aplicados = String.join(" Y ", textos);
        }
        lblCriterios.setText("Criterios aplicados: " + aplicados + "  →  " + modelo.getRowCount() + " resultado(s)");
    }

    // formulario para agregar (opcion == null) o modificar una opcion
    private void mostrarFormulario(OpcionMenu opcion) {
        String nombreInicial = "";
        String descInicial = "";
        String calInicial = "";
        String precioInicial = "";
        if (opcion != null) {
            nombreInicial = opcion.getNombre();
            descInicial = opcion.getDescripcion();
            calInicial = "" + opcion.getCalorias();
            precioInicial = "" + opcion.getPrecio();
        }

        JTextField txtNombre = new JTextField(nombreInicial, 15);
        JTextField txtDesc = new JTextField(descInicial, 15);
        JTextField txtCal = new JTextField(calInicial, 8);
        JTextField txtPrecio = new JTextField(precioInicial, 8);
        JComboBox<String> cboCat = new JComboBox<>(restaurante.getMenu().getCategorias().toArray(new String[0]));
        if (opcion != null) {
            cboCat.setSelectedItem(opcion.getCategoria());
        }

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        formulario.add(new JLabel("Nombre:"));
        formulario.add(txtNombre);
        formulario.add(new JLabel("Categoría:"));
        formulario.add(cboCat);
        formulario.add(new JLabel("Descripción:"));
        formulario.add(txtDesc);
        formulario.add(new JLabel("Calorías por porción:"));
        formulario.add(txtCal);
        formulario.add(new JLabel("Precio (colones):"));
        formulario.add(txtPrecio);

        String titulo = "Nueva opción";
        if (opcion != null) {
            titulo = "Modificar opción";
        }

        // si hay un error se vuelve a mostrar el formulario con lo que escribio
        while (JOptionPane.showConfirmDialog(this, formulario, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            boolean ok = Ui.ejecutar(this, () -> {
                int calorias = Ui.entero(txtCal.getText(), "Calorías");
                long precio = Ui.numero(txtPrecio.getText(), "Precio");
                String categoria = (String) cboCat.getSelectedItem();
                if (opcion == null) {
                    restaurante.agregarOpcion(txtNombre.getText(), categoria, txtDesc.getText(), calorias, precio);
                } else {
                    restaurante.modificarOpcion(opcion.getId(), txtNombre.getText(), categoria, txtDesc.getText(), calorias, precio);
                }
            });
            if (ok) {
                return;
            }
        }
    }
}
