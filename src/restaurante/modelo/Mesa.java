package restaurante.modelo;

import java.util.ArrayList;
import java.util.List;

// Una mesa del restaurante.
// Es el contexto del patron State (el estado decide que se puede hacer)
// y tambien el sujeto del patron Observer (avisa cuando algo cambia).
public class Mesa {

    private final int id;
    private EstadoMesa estado = EstadoMesa.DISPONIBLE;
    private Visita visita;
    private final List<ObservadorRestaurante> observadores = new ArrayList<>();

    public Mesa(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    public Visita getVisita() {
        return visita;
    }

    public boolean estaDisponible() {
        return estado == EstadoMesa.DISPONIBLE;
    }

    // Todas estas operaciones se las pasan al estado actual,
    // que es el que dice si se puede o no.

    public Visita abrirVisita(String mesero, List<String> aliases, boolean porPersona) {
        return estado.abrir(this, mesero, aliases, porPersona);
    }

    public Visita.Linea pedir(String alias, OpcionMenu opcion, int cantidad) {
        return estado.pedir(this, alias, opcion, cantidad);
    }

    public Visita.Linea anularPedido(int idLinea, String motivo) {
        return estado.anular(this, idLinea, motivo);
    }

    public void solicitarCuenta() {
        estado.solicitarCuenta(this);
    }

    public void reabrirCuenta() {
        estado.reabrir(this);
    }

    public Visita.Pago pagar(String alias, MetodoPago metodo, long monto) {
        return estado.pagar(this, alias, metodo, monto);
    }

    public Visita cerrarVisita() {
        return estado.cerrar(this);
    }

    // sin public para que solo lo use el estado (mismo paquete) y no la interfaz
    void setEstado(EstadoMesa estado) {
        this.estado = estado;
    }

    void setVisita(Visita visita) {
        this.visita = visita;
    }

    public void agregarObservador(ObservadorRestaurante observador) {
        observadores.add(observador);
    }

    void notificar(String detalle) {
        for (ObservadorRestaurante o : observadores) {
            o.actualizar(id, detalle);
        }
    }
}
