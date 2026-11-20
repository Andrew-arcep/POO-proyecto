package restaurante.ui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

// Metodos de ayuda que usan todos los paneles.
public final class Ui {

    private Ui() {
    }

    // ejecuta la accion y si el modelo lanza un error lo muestra en una ventanita.
    // devuelve true si salio bien
    public static boolean ejecutar(Component padre, Runnable accion) {
        try {
            accion.run();
            return true;
        } catch (RuntimeException e) {
            String mensaje = e.getMessage();
            if (mensaje == null) {
                mensaje = e.toString();
            }
            JOptionPane.showMessageDialog(padre, mensaje, "No se pudo completar la acción", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    public static long numero(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio.");
        }
        try {
            return Long.parseLong(texto.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + campo + "' debe ser un número entero (sin letras ni decimales).");
        }
    }

    public static int entero(String texto, String campo) {
        long n = numero(texto, campo);
        if (n > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("'" + campo + "' es demasiado grande.");
        }
        return (int) n;
    }

    // si el campo esta vacio devuelve null (sin limite)
    public static LocalDate fecha(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("'" + campo + "' debe tener el formato AAAA-MM-DD (ej. 2026-11-18).");
        }
    }

    // modelo de tabla donde no se pueden editar las celdas
    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    public static JTable tabla(DefaultTableModel modelo, int... anchos) {
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(24);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        return tabla;
    }

    // panel con los componentes en fila, alineados a la izquierda
    public static JPanel fila(Component... componentes) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (Component c : componentes) {
            panel.add(c);
        }
        return panel;
    }
}
