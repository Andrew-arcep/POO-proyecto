package restaurante.modelo;

import java.util.List;
import restaurante.excepciones.*;

// Patron State hecho con un enum.
// Cada estado sobreescribe solo lo que se puede hacer en ese estado.
// Lo que no sobreescribe cae en los metodos de abajo, que lanzan error.
//
// DISPONIBLE -> OCUPADA <-> PENDIENTE_PAGO -> DISPONIBLE
public enum EstadoMesa {

    DISPONIBLE("Disponible") {
        @Override
        Visita abrir(Mesa mesa, String mesero, List<String> aliases, boolean porPersona) {
            // si los datos estan mal el constructor lanza error y la mesa no cambia
            Visita visita = new Visita(mesa.getId(), mesero, aliases, porPersona);
            mesa.setVisita(visita);
            mesa.setEstado(OCUPADA);
            mesa.notificar("Visita abierta: " + visita.getMesero() + ", " + aliases.size() + " persona(s)");
            return visita;
        }

        @Override
        Visita.Linea pedir(Mesa mesa, String alias, OpcionMenu opcion, int cantidad) {
            throw sinVisita(mesa);
        }

        @Override
        Visita.Linea anular(Mesa mesa, int idLinea, String motivo) {
            throw sinVisita(mesa);
        }
    },

    OCUPADA("Ocupada") {
        @Override
        Visita.Linea pedir(Mesa mesa, String alias, OpcionMenu opcion, int cantidad) {
            Visita.Linea linea = mesa.getVisita().agregarLinea(alias, opcion, cantidad);
            mesa.notificar(linea.getAlias() + " pidió " + cantidad + " x " + linea.getProducto());
            return linea;
        }

        @Override
        Visita.Linea anular(Mesa mesa, int idLinea, String motivo) {
            Visita.Linea linea = mesa.getVisita().anularLinea(idLinea, motivo);
            mesa.notificar("Se anuló #" + idLinea + " (" + linea.getProducto() + "): " + linea.getMotivoAnulacion());
            return linea;
        }

        @Override
        void solicitarCuenta(Mesa mesa) {
            mesa.setEstado(PENDIENTE_PAGO);
            mesa.notificar("Cuenta solicitada: " + Visita.colones(mesa.getVisita().totalMesa()));
        }
    },

    PENDIENTE_PAGO("Pendiente de pago") {
        @Override
        void reabrir(Mesa mesa) {
            mesa.setEstado(OCUPADA);
            mesa.notificar("La mesa vuelve a admitir pedidos");
        }

        @Override
        Visita.Pago pagar(Mesa mesa, String alias, MetodoPago metodo, long monto) {
            Visita.Pago pago = mesa.getVisita().registrarPago(alias, metodo, monto);
            mesa.notificar("Pago " + metodo + " " + Visita.colones(monto) + " ref " + pago.getReferencia());
            return pago;
        }

        @Override
        Visita cerrar(Mesa mesa) {
            Visita visita = mesa.getVisita();
            visita.cerrar(); // si queda saldo lanza error y no se cambia nada
            mesa.setVisita(null);
            mesa.setEstado(DISPONIBLE);
            mesa.notificar("Visita cerrada. Cobrado " + Visita.colones(visita.totalPagado()));
            return visita;
        }
    };

    private final String texto;

    EstadoMesa(String texto) {
        this.texto = texto;
    }

    @Override
    public String toString() {
        return texto;
    }

    // ---------- comportamiento por defecto: no se permite ----------

    Visita abrir(Mesa mesa, String mesero, List<String> aliases, boolean porPersona) {
        throw new MesaNoDisponibleException("La mesa " + mesa.getId() + " está ocupada hasta que se pague y cierre la visita.");
    }

    Visita.Linea pedir(Mesa mesa, String alias, OpcionMenu opcion, int cantidad) {
        throw noPermitida(mesa, "agregar pedidos");
    }

    Visita.Linea anular(Mesa mesa, int idLinea, String motivo) {
        throw noPermitida(mesa, "anular pedidos");
    }

    void solicitarCuenta(Mesa mesa) {
        throw noPermitida(mesa, "solicitar la cuenta");
    }

    void reabrir(Mesa mesa) {
        throw noPermitida(mesa, "reabrir la cuenta");
    }

    Visita.Pago pagar(Mesa mesa, String alias, MetodoPago metodo, long monto) {
        throw new PagoInvalidoException("La cuenta de la mesa " + mesa.getId() + " no está pendiente de pago.");
    }

    Visita cerrar(Mesa mesa) {
        throw noPermitida(mesa, "cerrar la visita");
    }

    private IllegalStateException noPermitida(Mesa mesa, String operacion) {
        return new IllegalStateException("No se puede " + operacion + " en la mesa " + mesa.getId() + ": está '" + texto + "'.");
    }

    private static PedidoEnVisitaCerradaException sinVisita(Mesa mesa) {
        return new PedidoEnVisitaCerradaException("La mesa " + mesa.getId() + " no tiene una visita abierta: no se pueden agregar ni modificar pedidos.");
    }
}
