package restaurante.excepciones;

// se lanza monto malo, metodo invalido o cierre con saldo pendiente
public class PagoInvalidoException extends RuntimeException {

    public PagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
