package restaurante.excepciones;

// se lanza cuando la mesa esta ocupada o no existe
public class MesaNoDisponibleException extends RuntimeException {

    public MesaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
