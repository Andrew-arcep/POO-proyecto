package restaurante.modelo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

// El menu: guarda las categorias (nombre -> si esta activa) y las opciones.
// Las reglas del menu estan aca.
public class Menu {

    public static final String PROTEINAS = "Proteínas";

    private final Map<Integer, OpcionMenu> opciones = new LinkedHashMap<>();
    private final Map<String, Boolean> categorias = new LinkedHashMap<>();
    private int siguienteId = 1;

    public Menu() {
        categorias.put("Guarniciones", true);
        categorias.put(PROTEINAS, true);
        categorias.put("Ensaladas", true);
        categorias.put("Postres", true);
        categorias.put("Bebidas", true);
    }

    // ---------- categorias ----------

    public List<String> getCategorias() {
        return new ArrayList<>(categorias.keySet());
    }

    public boolean esActiva(String categoria) {
        return categorias.get(nombreCategoria(categoria));
    }

    // busca la categoria sin importar mayusculas y devuelve el nombre tal como esta guardado
    public String nombreCategoria(String categoria) {
        String buscada = "";
        if (categoria != null) {
            buscada = categoria.trim();
        }
        for (String nombre : categorias.keySet()) {
            if (nombre.equalsIgnoreCase(buscada)) {
                return nombre;
            }
        }
        throw new IllegalArgumentException("La categoría '" + categoria + "' no existe.");
    }

    public void agregarCategoria(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }
        for (String existente : categorias.keySet()) {
            if (existente.equalsIgnoreCase(nombre.trim())) {
                throw new IllegalArgumentException("La categoría ya existe.");
            }
        }
        categorias.put(nombre.trim(), true);
    }

    public void activarCategoria(String nombre, boolean activa) {
        categorias.put(nombreCategoria(nombre), activa);
    }

    // las categorias que pide el filtro calorico: todas las activas menos proteinas
    public List<String> categoriasRequeridas() {
        List<String> requeridas = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : categorias.entrySet()) {
            if (e.getValue() && !e.getKey().equals(PROTEINAS)) {
                requeridas.add(e.getKey());
            }
        }
        return requeridas;
    }

    // ---------- opciones ----------

    public OpcionMenu agregar(String nombre, String categoria, String descripcion, int calorias, long precio) {
        OpcionMenu nueva = new OpcionMenu(siguienteId, nombre, nombreCategoria(categoria), descripcion, calorias, precio);
        opciones.put(siguienteId, nueva);
        siguienteId++;
        return nueva;
    }

    public OpcionMenu obtener(int id) {
        OpcionMenu opcion = opciones.get(id);
        if (opcion == null) {
            throw new IllegalArgumentException("La opción #" + id + " no existe.");
        }
        return opcion;
    }

    public void modificar(int id, String nombre, String categoria, String descripcion, int calorias, long precio) {
        obtener(id).modificar(nombre, nombreCategoria(categoria), descripcion, calorias, precio);
    }

    public void setDisponible(int id, boolean disponible) {
        obtener(id).setDisponible(disponible);
    }

    // enUso dice si la opcion ya tiene pedidos (eso lo sabe Restaurante, no el menu)
    public void eliminar(int id, Predicate<OpcionMenu> enUso) {
        OpcionMenu opcion = obtener(id);
        if (enUso.test(opcion)) {
            throw new IllegalArgumentException("No se puede eliminar '" + opcion.getNombre() + "': tiene pedidos asociados. Desactívela.");
        }
        opciones.remove(id);
    }

    public List<OpcionMenu> listar() {
        return new ArrayList<>(opciones.values());
    }

    public List<OpcionMenu> listarPorCategoria(String categoria, boolean soloDisponibles) {
        List<OpcionMenu> lista = new ArrayList<>();
        for (OpcionMenu o : opciones.values()) {
            if (o.getCategoria().equals(categoria)) {
                if (!soloDisponibles || o.isDisponible()) {
                    lista.add(o);
                }
            }
        }
        return lista;
    }

    // ---------- busquedas ----------
    // cada criterio es un Predicate y para que salga una opcion tiene que cumplir todos (AND)

    public List<OpcionMenu> buscar(List<Predicate<OpcionMenu>> criterios) {
        List<OpcionMenu> resultado = new ArrayList<>();
        for (OpcionMenu o : opciones.values()) {
            boolean cumpleTodos = true;
            for (Predicate<OpcionMenu> criterio : criterios) {
                if (!criterio.test(o)) {
                    cumpleTodos = false;
                }
            }
            if (cumpleTodos) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    public static Predicate<OpcionMenu> porCategoria(String categoria) {
        return o -> o.getCategoria().equals(categoria);
    }

    public static Predicate<OpcionMenu> porTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escriba el texto a buscar.");
        }
        String buscado = texto.trim().toLowerCase();
        return o -> o.getNombre().toLowerCase().contains(buscado);
    }

    public static Predicate<OpcionMenu> porPrecio(long min, long max) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Rango de precio inválido (0 ≤ mín ≤ máx).");
        }
        return o -> o.getPrecio() >= min && o.getPrecio() <= max;
    }

    public static Predicate<OpcionMenu> porCalorias(int min, int max) {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Rango de calorías inválido (0 ≤ mín ≤ máx).");
        }
        return o -> o.getCalorias() >= min && o.getCalorias() <= max;
    }

    public static Predicate<OpcionMenu> soloDisponibles() {
        return o -> o.isDisponible();
    }
}
