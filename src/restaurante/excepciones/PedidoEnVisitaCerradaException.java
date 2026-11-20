package restaurante.excepciones;

// se lanza cuando se pide en una mesa sin visita abierta
public class PedidoEnVisitaCerradaException extends RuntimeException {

    public PedidoEnVisitaCerradaException(String mensaje) {
        super(mensaje);
    }
}
