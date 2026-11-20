package restaurante.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import restaurante.excepciones.*;

// Clase principal del modelo. La interfaz grafica solo habla con esta clase.
// Todo se guarda en memoria, no hay archivos ni base de datos.
public class Restaurante {

    private final Menu menu = new Menu();
    private final List<Mesa> mesas = new ArrayList<>();
    private final List<Visita> historial = new ArrayList<>();
    private final List<ObservadorRestaurante> observadores = new ArrayList<>();
    private final List<String> bitacora = new ArrayList<>();

    public Restaurante(int cantidadMesas) {
        if (cantidadMesas < 1 || cantidadMesas > 50) {
            throw new IllegalArgumentException("La cantidad de mesas debe estar entre 1 y 50.");
        }
        for (int i = 1; i <= cantidadMesas; i++) {
            Mesa mesa = new Mesa(i);
            // la bitacora tambien es un observador de cada mesa
            mesa.agregarObservador((numero, detalle) -> registrar(numero, detalle));
            mesas.add(mesa);
        }
    }

    private void registrar(int mesa, String detalle) {
        String fecha = LocalDateTime.now().withNano(0).toString().replace('T', ' ');
        String donde = "Menú";
        if (mesa > 0) {
            donde = "Mesa " + mesa;
        }
        bitacora.add(fecha + " | " + donde + " | " + detalle);
    }

    // el observador se agrega tambien a todas las mesas
    public void agregarObservador(ObservadorRestaurante observador) {
        observadores.add(observador);
        for (Mesa mesa : mesas) {
            mesa.agregarObservador(observador);
        }
    }

    // los cambios del menu no son de una mesa, entonces se avisa con mesa = 0
    private void avisarMenu(String detalle) {
        registrar(0, detalle);
        for (ObservadorRestaurante o : observadores) {
            o.actualizar(0, detalle);
        }
    }

    public Menu getMenu() {
        return menu;
    }

    public List<String> getBitacora() {
        return Collections.unmodifiableList(bitacora);
    }

    public List<Mesa> getMesas() {
        return Collections.unmodifiableList(mesas);
    }

    public List<Visita> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    public Mesa mesa(int id) {
        for (Mesa m : mesas) {
            if (m.getId() == id) {
                return m;
            }
        }
        throw new MesaNoDisponibleException("La mesa " + id + " no existe.");
    }

    // visitas cerradas y abiertas (los pagos de una visita abierta tambien cuentan como cobrados)
    public List<Visita> todasLasVisitas() {
        List<Visita> todas = new ArrayList<>(historial);
        for (Mesa m : mesas) {
            if (m.getVisita() != null) {
                todas.add(m.getVisita());
            }
        }
        return todas;
    }

    // ---------- mesas y visitas ----------

    public Visita abrirVisita(int idMesa, String mesero, List<String> aliases, boolean porPersona) {
        return mesa(idMesa).abrirVisita(mesero, aliases, porPersona);
    }

    public Visita.Linea pedir(int idMesa, String alias, int idOpcion, int cantidad) {
        return mesa(idMesa).pedir(alias, menu.obtener(idOpcion), cantidad);
    }

    public void anularPedido(int idMesa, int idLinea, String motivo) {
        mesa(idMesa).anularPedido(idLinea, motivo);
    }

    public void solicitarCuenta(int idMesa) {
        mesa(idMesa).solicitarCuenta();
    }

    public void reabrirCuenta(int idMesa) {
        mesa(idMesa).reabrirCuenta();
    }

    public Visita.Pago pagar(int idMesa, String alias, String metodo, String dato, long monto) {
        MetodoPago metodoPago = MetodoPago.crear(metodo, dato);
        return mesa(idMesa).pagar(alias, metodoPago, monto);
    }

    public Visita cerrarVisita(int idMesa) {
        Visita visita = mesa(idMesa).cerrarVisita();
        historial.add(visita);
        return visita;
    }

    // ---------- menu (se avisa a los observadores) ----------

    public OpcionMenu agregarOpcion(String nombre, String categoria, String descripcion, int calorias, long precio) {
        OpcionMenu opcion = menu.agregar(nombre, categoria, descripcion, calorias, precio);
        avisarMenu("Opción agregada: " + opcion.getNombre());
        return opcion;
    }

    public void modificarOpcion(int id, String nombre, String categoria, String descripcion, int calorias, long precio) {
        menu.modificar(id, nombre, categoria, descripcion, calorias, precio);
        avisarMenu("Opción #" + id + " modificada");
    }

    public void setDisponible(int id, boolean disponible) {
        menu.setDisponible(id, disponible);
        if (disponible) {
            avisarMenu("Opción #" + id + " activada");
        } else {
            avisarMenu("Opción #" + id + " desactivada");
        }
    }

    public void eliminarOpcion(int id) {
        // no se puede eliminar si alguna visita ya la pidio
        menu.eliminar(id, opcion -> tienePedidos(opcion));
        avisarMenu("Opción #" + id + " eliminada");
    }

    private boolean tienePedidos(OpcionMenu opcion) {
        for (Visita v : todasLasVisitas()) {
            for (Visita.Linea l : v.getLineas()) {
                if (l.getOpcionId() == opcion.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void agregarCategoria(String nombre) {
        menu.agregarCategoria(nombre);
        avisarMenu("Categoría agregada: " + nombre);
    }

    public void activarCategoria(String nombre, boolean activa) {
        menu.activarCategoria(nombre, activa);
        if (activa) {
            avisarMenu("Categoría " + nombre + " activada");
        } else {
            avisarMenu("Categoría " + nombre + " desactivada");
        }
    }

    // ---------- filtro calorico ----------

    // proteina elegida + una opcion de cada categoria activa (menos proteinas) sin pasar de max calorias
    public List<Combinacion> buscarCombinaciones(int idProteina, int max, EstrategiaCombinacion estrategia) {
        if (max <= 0) {
            throw new IllegalArgumentException("El máximo de calorías debe ser mayor que cero.");
        }
        if (estrategia == null) {
            throw new IllegalArgumentException("Elija un algoritmo de búsqueda.");
        }
        OpcionMenu proteina = menu.obtener(idProteina);
        if (!proteina.getCategoria().equals(Menu.PROTEINAS) || !proteina.isDisponible()) {
            throw new IllegalArgumentException("Debe elegir una proteína disponible.");
        }

        // opciones activas de cada categoria requerida
        List<List<OpcionMenu>> porCategoria = new ArrayList<>();
        for (String categoria : menu.categoriasRequeridas()) {
            List<OpcionMenu> activas = menu.listarPorCategoria(categoria, true);
            if (activas.isEmpty()) {
                throw new LimiteCaloricoSinResultadosException("La categoría requerida '" + categoria + "' no tiene opciones activas. Actívelas o desactive la categoría.");
            }
            porCategoria.add(activas);
        }

        List<Combinacion> resultado = new ArrayList<>(estrategia.generar(proteina, porCategoria, max));
        if (resultado.isEmpty()) {
            throw new LimiteCaloricoSinResultadosException("No hay comidas completas con " + proteina.getNombre() + " de " + max + " kcal o menos. Suba el límite o cambie la proteína.");
        }
        resultado.sort(Combinacion.ORDEN);
        return resultado;
    }

    // restaurante de ejemplo con 8 mesas y un menu ya cargado
    public static Restaurante demo() {
        Restaurante r = new Restaurante(8);

        // categoria, nombre, descripcion, calorias, precio
        String[][] datos = {
            {"Guarniciones", "Arroz blanco", "Arroz al vapor", "205", "1500"},
            {"Guarniciones", "Frijoles molidos", "Frijoles negros", "180", "1500"},
            {"Guarniciones", "Puré de papa", "Con mantequilla", "237", "1800"},
            {"Guarniciones", "Papas fritas", "Porción mediana", "365", "2200"},
            {"Proteínas", "Pechuga a la plancha", "Pollo 180 g", "280", "4500"},
            {"Proteínas", "Filet de res", "Res 200 g", "420", "7500"},
            {"Proteínas", "Pescado al ajillo", "Filete de tilapia", "310", "6000"},
            {"Proteínas", "Tofu salteado", "Opción vegetariana", "190", "4000"},
            {"Ensaladas", "Ensalada verde", "Lechuga, pepino, tomate", "70", "2000"},
            {"Ensaladas", "Ensalada César", "Con crutones", "260", "3200"},
            {"Ensaladas", "Ensalada de palmito", "Palmito y aguacate", "150", "3000"},
            {"Postres", "Gelatina", "Sabor fresa", "80", "1200"},
            {"Postres", "Tres leches", "Porción", "390", "2800"},
            {"Postres", "Fruta picada", "Frutas de temporada", "95", "2000"},
            {"Bebidas", "Agua", "Botella 500 ml", "0", "1000"},
            {"Bebidas", "Fresco natural", "Vaso 12 oz", "120", "1600"},
            {"Bebidas", "Refresco", "Lata 355 ml", "150", "1500"},
            {"Bebidas", "Café con leche", "Taza", "110", "1800"}
        };
        for (String[] fila : datos) {
            r.agregarOpcion(fila[1], fila[0], fila[2], Integer.parseInt(fila[3]), Long.parseLong(fila[4]));
        }
        return r;
    }
}
