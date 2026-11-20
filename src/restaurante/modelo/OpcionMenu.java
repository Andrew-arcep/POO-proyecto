package restaurante.modelo;

// Una opcion del menu (un plato, una bebida, etc).
// Los datos se validan aca para que nunca quede una opcion invalida.
public class OpcionMenu {

    private final int id;
    private String nombre;
    private String categoria;
    private String descripcion;
    private int calorias;
    private long precio;
    private boolean disponible = true;

    OpcionMenu(int id, String nombre, String categoria, String descripcion, int calorias, long precio) {
        this.id = id;
        modificar(nombre, categoria, descripcion, calorias, precio);
    }

    final void modificar(String nombre, String categoria, String descripcion, int calorias, long precio) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (calorias < 0) {
            throw new IllegalArgumentException("Las calorías no pueden ser negativas.");
        }
        if (precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        this.nombre = nombre.trim();
        this.categoria = categoria;
        if (descripcion == null) {
            this.descripcion = "";
        } else {
            this.descripcion = descripcion.trim();
        }
        this.calorias = calorias;
        this.precio = precio;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCalorias() {
        return calorias;
    }

    public long getPrecio() {
        return precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    @Override
    public String toString() {
        return nombre + " (" + calorias + " kcal, " + Visita.colones(precio) + ")";
    }
}
