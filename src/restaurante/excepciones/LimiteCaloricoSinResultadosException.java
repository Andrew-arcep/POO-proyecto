package restaurante.excepciones;

// se lanza cuando ninguna combinacion cumple el limite de calorias
public class LimiteCaloricoSinResultadosException extends RuntimeException {

    public LimiteCaloricoSinResultadosException(String mensaje) {
        super(mensaje);
    }
}
