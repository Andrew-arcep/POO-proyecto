package restaurante.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import restaurante.excepciones.*;

// Una visita a una mesa: los comensales, lo que pidieron y lo que pagaron.
// Los comensales se identifican por un alias que no se puede repetir.
// Los metodos que modifican no son public, solo los usa la Mesa (por medio de su estado).
// Siempre se valida antes de modificar algo.
public class Visita {

    // los montos van en colones enteros (long) para no tener problemas con decimales
    public static String colones(long monto) {
        return String.format(Locale.US, "₡%,d", monto);
    }

    // ---------- clase Linea: un articulo pedido ----------
    // guarda el precio que tenia al pedirlo y si se anulo
    public static class Linea {
        private final int id;
        private final int opcionId;
        private final int cantidad;
        private final String alias;
        private final String producto;
        private final String categoria;
        private final long precio;
        private final LocalDateTime fecha = LocalDateTime.now();
        private boolean anulada;
        private String motivo = "";

        Linea(int id, String alias, OpcionMenu opcion, int cantidad) {
            this.id = id;
            this.alias = alias;
            this.cantidad = cantidad;
            this.opcionId = opcion.getId();
            this.producto = opcion.getNombre();
            this.categoria = opcion.getCategoria();
            this.precio = opcion.getPrecio();
        }

        public int getId() {
            return id;
        }

        public int getOpcionId() {
            return opcionId;
        }

        public int getCantidad() {
            return cantidad;
        }

        public String getAlias() {
            return alias;
        }

        public String getProducto() {
            return producto;
        }

        public String getCategoria() {
            return categoria;
        }

        public long getPrecioAplicado() {
            return precio;
        }

        public long getSubtotal() {
            return precio * cantidad;
        }

        public LocalDateTime getFechaHora() {
            return fecha;
        }

        public boolean isAnulada() {
            return anulada;
        }

        public String getMotivoAnulacion() {
            return motivo;
        }
    }

    // ---------- clase Pago (es ficticio) ----------
    // el alias es null cuando la cuenta es conjunta
    public static class Pago {
        private final int id;
        private final String alias;
        private final String referencia;
        private final long monto;
        private final MetodoPago metodo;
        private final LocalDateTime fecha = LocalDateTime.now();

        Pago(int id, String alias, long monto, MetodoPago metodo) {
            this.id = id;
            this.alias = alias;
            this.monto = monto;
            this.metodo = metodo;
            this.referencia = metodo.generarReferencia();
        }

        public int getId() {
            return id;
        }

        public String getAlias() {
            return alias;
        }

        public long getMonto() {
            return monto;
        }

        public MetodoPago getMetodo() {
            return metodo;
        }

        public String getReferencia() {
            return referencia;
        }

        public LocalDateTime getFechaHora() {
            return fecha;
        }
    }

    // ---------- atributos de la visita ----------

    private static int contador = 0;

    private final int id;
    private final int numeroMesa;
    private final String mesero;
    private final boolean porPersona; // true = cada quien paga lo suyo, false = cuenta conjunta
    private final LocalDateTime apertura = LocalDateTime.now();
    private LocalDateTime cierre;
    private final List<String> comensales = new ArrayList<>();
    private final List<Linea> lineas = new ArrayList<>();
    private final List<Pago> pagos = new ArrayList<>();

    Visita(int numeroMesa, String mesero, List<String> aliases, boolean porPersona) {
        if (mesero == null || mesero.isBlank()) {
            throw new IllegalArgumentException("El mesero responsable es obligatorio.");
        }
        if (aliases == null || aliases.isEmpty()) {
            throw new IllegalArgumentException("Debe haber al menos un comensal.");
        }
        if (aliases.size() > 20) {
            throw new IllegalArgumentException("Máximo 20 comensales por mesa.");
        }

        // revisa que ningun alias este vacio o repetido
        Set<String> vistos = new HashSet<>();
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) {
                throw new IllegalArgumentException("Todos los comensales necesitan un alias.");
            }
            boolean esNuevo = vistos.add(alias.trim().toLowerCase());
            if (!esNuevo) {
                throw new IllegalArgumentException("Alias repetido: '" + alias.trim() + "'.");
            }
            comensales.add(alias.trim());
        }

        contador++;
        this.id = contador;
        this.numeroMesa = numeroMesa;
        this.mesero = mesero.trim();
        this.porPersona = porPersona;
    }

    // ---------- metodos que modifican (solo los usa la Mesa) ----------

    Linea agregarLinea(String alias, OpcionMenu opcion, int cantidad) {
        if (estaCerrada()) {
            throw new PedidoEnVisitaCerradaException("La visita ya está cerrada.");
        }
        String comensal = comensal(alias);
        if (opcion == null || !opcion.isDisponible()) {
            throw new IllegalArgumentException("La opción no está disponible.");
        }
        if (cantidad < 1 || cantidad > 99) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y 99.");
        }
        Linea linea = new Linea(lineas.size() + 1, comensal, opcion, cantidad);
        lineas.add(linea);
        return linea;
    }

    Linea anularLinea(int idLinea, String motivo) {
        if (estaCerrada()) {
            throw new PedidoEnVisitaCerradaException("La visita ya está cerrada.");
        }

        Linea linea = null;
        for (Linea x : lineas) {
            if (x.id == idLinea) {
                linea = x;
            }
        }
        if (linea == null) {
            throw new IllegalArgumentException("El pedido #" + idLinea + " no existe.");
        }
        if (linea.anulada) {
            throw new IllegalArgumentException("Ese pedido ya estaba anulado.");
        }

        // no se puede anular si lo ya pagado pasaria a ser mas que el nuevo total
        boolean pagoDeMas = totalPagado() > totalMesa() - linea.getSubtotal();
        if (porPersona && pagadoPor(linea.alias) > subtotalDe(linea.alias) - linea.getSubtotal()) {
            pagoDeMas = true;
        }
        if (pagoDeMas) {
            throw new IllegalArgumentException("No se puede anular: ya hay pagos aplicados que superarían el nuevo total.");
        }

        linea.anulada = true;
        if (motivo == null || motivo.isBlank()) {
            linea.motivo = "Sin motivo";
        } else {
            linea.motivo = motivo.trim();
        }
        return linea;
    }

    Pago registrarPago(String alias, MetodoPago metodo, long monto) {
        if (estaCerrada()) {
            throw new PagoInvalidoException("La visita ya está cerrada.");
        }
        if (monto <= 0) {
            throw new PagoInvalidoException("El monto debe ser positivo.");
        }

        String quien = null;
        long pendiente = saldo();
        if (porPersona) {
            if (alias == null) {
                throw new PagoInvalidoException("Indique quién paga (modalidad por persona).");
            }
            quien = comensal(alias);
            pendiente = saldoDe(quien);
        }

        if (monto > pendiente) {
            throw new PagoInvalidoException("El monto (" + colones(monto) + ") excede el saldo pendiente (" + colones(pendiente) + ").");
        }

        Pago pago = new Pago(pagos.size() + 1, quien, monto, metodo);
        pagos.add(pago);
        return pago;
    }

    void cerrar() {
        if (saldo() != 0) {
            throw new PagoInvalidoException("No se puede cerrar: saldo pendiente " + colones(saldo()) + ".");
        }
        cierre = LocalDateTime.now();
    }

    // ---------- consultas ----------

    public int getId() {
        return id;
    }

    public int getNumeroMesa() {
        return numeroMesa;
    }

    public String getMesero() {
        return mesero;
    }

    public boolean isPorPersona() {
        return porPersona;
    }

    public LocalDateTime getApertura() {
        return apertura;
    }

    public LocalDateTime getCierre() {
        return cierre;
    }

    public boolean estaCerrada() {
        return cierre != null;
    }

    public List<String> getComensales() {
        return Collections.unmodifiableList(comensales);
    }

    public List<Linea> getLineas() {
        return Collections.unmodifiableList(lineas);
    }

    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }

    // devuelve el alias como esta guardado, o lanza error si no es de esta visita
    public String comensal(String alias) {
        String buscado = "";
        if (alias != null) {
            buscado = alias.trim();
        }
        for (String c : comensales) {
            if (c.equalsIgnoreCase(buscado)) {
                return c;
            }
        }
        throw new IllegalArgumentException("El comensal '" + alias + "' no pertenece a esta visita.");
    }

    public long subtotalDe(String alias) {
        long total = 0;
        for (Linea l : lineas) {
            if (!l.anulada && l.alias.equals(alias)) {
                total += l.getSubtotal();
            }
        }
        return total;
    }

    public long totalMesa() {
        long total = 0;
        for (Linea l : lineas) {
            if (!l.anulada) {
                total += l.getSubtotal();
            }
        }
        return total;
    }

    public Map<String, Long> subtotalPorCategoria() {
        Map<String, Long> mapa = new TreeMap<>();
        for (Linea l : lineas) {
            if (!l.anulada) {
                mapa.merge(l.categoria, l.getSubtotal(), Long::sum);
            }
        }
        return mapa;
    }

    public long totalPagado() {
        long total = 0;
        for (Pago p : pagos) {
            total += p.monto;
        }
        return total;
    }

    public long pagadoPor(String alias) {
        long total = 0;
        for (Pago p : pagos) {
            if (alias.equals(p.alias)) {
                total += p.monto;
            }
        }
        return total;
    }

    public long saldo() {
        return totalMesa() - totalPagado();
    }

    public long saldoDe(String alias) {
        return subtotalDe(alias) - pagadoPor(alias);
    }
}
