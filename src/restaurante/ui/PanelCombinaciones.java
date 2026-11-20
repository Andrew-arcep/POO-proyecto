package restaurante.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import restaurante.excepciones.LimiteCaloricoSinResultadosException;
import restaurante.modelo.BacktrackingConPoda;
import restaurante.modelo.Combinacion;
import restaurante.modelo.EstrategiaCombinacion;
import restaurante.modelo.FuerzaBruta;
import restaurante.modelo.Menu;
import restaurante.modelo.ObservadorRestaurante;
import restaurante.modelo.OpcionMenu;
import restaurante.modelo.Restaurante;
import restaurante.modelo.Visita;

// Pestaña del filtro calorico: se elige una proteina y un maximo de calorias
// y muestra las comidas completas que cumplen.
public class PanelCombinaciones extends JPanel implements ObservadorRestaurante {

    private final Restaurante restaurante;
    private final JComboBox<OpcionMenu> cboProteina = new JComboBox<>();
    private final JTextField txtMax = new JTextField("800", 6);
    private final JComboBox<EstrategiaCombinacion> cboEstrategia =
            new JComboBox<>(new EstrategiaCombinacion[]{new BacktrackingConPoda(), new FuerzaBruta()});
    private final DefaultTableModel modelo = Ui.modelo("#", "Combinación completa", "Calorías", "Precio");
    private final JTable tabla = Ui.tabla(modelo, 40, 700, 90, 90);
    private final JLabel lblRequeridas = new JLabel(" ");
    private final JLabel lblMensaje = new JLabel(" ");

    public PanelCombinaciones(Restaurante restaurante) {
        super(new BorderLayout(8, 8));
        this.restaurante = restaurante;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // para que el combo muestre el nombre del algoritmo y no el objeto
        cboEstrategia.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice, boolean seleccionado, boolean foco) {
                String texto = "";
                if (valor != null) {
                    texto = ((EstrategiaCombinacion) valor).nombre();
                }
                return super.getListCellRendererComponent(lista, texto, indice, seleccionado, foco);
            }
        });

        JButton btnBuscar = new JButton("Buscar combinaciones");

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(Ui.fila(new JLabel("Proteína:"), cboProteina, new JLabel("Máx. calorías (X):"), txtMax,
                new JLabel("Algoritmo:"), cboEstrategia, btnBuscar), BorderLayout.NORTH);
        norte.add(lblRequeridas, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(lblMensaje, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> buscar());
        actualizar(0, "");
    }

    // se llama cuando cambia el menu: vuelve a cargar las proteinas
    @Override
    public void actualizar(int mesa, String detalle) {
        Object seleccionada = cboProteina.getSelectedItem();
        cboProteina.removeAllItems();
        for (OpcionMenu o : restaurante.getMenu().listarPorCategoria(Menu.PROTEINAS, true)) {
            cboProteina.addItem(o);
        }
        if (seleccionada != null) {
            cboProteina.setSelectedItem(seleccionada);
        }
        lblRequeridas.setText("<html>Categorías requeridas (una opción de cada una): <b>"
                + String.join("</b>, <b>", restaurante.getMenu().categoriasRequeridas()) + "</b></html>");
    }

    private void buscar() {
        modelo.setRowCount(0);
        lblMensaje.setForeground(Color.DARK_GRAY);
        EstrategiaCombinacion estrategia = (EstrategiaCombinacion) cboEstrategia.getSelectedItem();

        try {
            if (cboProteina.getSelectedItem() == null) {
                throw new IllegalArgumentException("No hay proteínas disponibles para elegir.");
            }
            int max = Ui.entero(txtMax.getText(), "Máximo de calorías");
            OpcionMenu proteina = (OpcionMenu) cboProteina.getSelectedItem();
            List<Combinacion> resultado = restaurante.buscarCombinaciones(proteina.getId(), max, estrategia);

            int numero = 1;
            for (Combinacion c : resultado) {
                // texto tipo "Guarniciones: Arroz  +  Postres: Gelatina"
                String texto = "";
                for (OpcionMenu o : c.getOpciones()) {
                    if (!texto.isEmpty()) {
                        texto += "  +  ";
                    }
                    texto += o.getCategoria() + ": " + o.getNombre();
                }
                modelo.addRow(new Object[]{numero, texto, c.getCalorias() + " kcal", Visita.colones(c.getPrecio())});
                numero++;
            }
            lblMensaje.setText(resultado.size() + " combinación(es) ≤ " + max + " kcal  |  "
                    + estrategia.nombre() + ": " + estrategia.nodosExplorados() + " nodos explorados");

        } catch (LimiteCaloricoSinResultadosException e) {
            // no es un error del usuario, solo no hay resultados, por eso va en rojo en la pantalla
            lblMensaje.setForeground(new Color(0xB00020));
            lblMensaje.setText(e.getMessage());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Dato inválido", JOptionPane.WARNING_MESSAGE);
        }
    }
}
