package restaurante.modelo;

import java.util.List;
import restaurante.excepciones.PagoInvalidoException;

// Clase abstracta: lo que tienen en comun todos los metodos de pago.
// Cada subclase valida el dato que le toca (tarjeta, telefono).
public abstract class MetodoPago {

    public static final List<String> NOMBRES = List.of("Efectivo", "Tarjeta", "SINPE Móvil");

    private static int contador = 1000;
    private final String nombre;

    protected MetodoPago(String nombre) {
        this.nombre = nombre;
    }

    // cada metodo pone su propio prefijo en la referencia
    protected abstract String prefijo();

    // dato extra para mostrar (ultimos digitos, telefono). Por defecto no hay
    public String detalle() {
        return "";
    }

    public String getNombre() {
        return nombre;
    }

    public final String generarReferencia() {
        contador++;
        return prefijo() + "-" + contador;
    }

    @Override
    public String toString() {
        return nombre + detalle();
    }

    // texto que se le muestra al usuario para pedir el dato del metodo
    public static String pista(String nombre) {
        if (nombre.equals("Tarjeta")) {
            return "Últimos 4 dígitos";
        }
        if (nombre.equals("SINPE Móvil")) {
            return "Teléfono (8 dígitos)";
        }
        return "";
    }

    public static MetodoPago crear(String nombre, String dato) {
        String limpio = "";
        if (dato != null) {
            limpio = dato.trim();
        }
        if (nombre == null || !NOMBRES.contains(nombre)) {
            throw new PagoInvalidoException("El método de pago '" + nombre + "' no está disponible.");
        }
        if (nombre.equals("Tarjeta")) {
            return new Tarjeta(limpio);
        }
        if (nombre.equals("SINPE Móvil")) {
            return new Sinpe(limpio);
        }
        return new Efectivo();
    }

    // ---------- subclases ----------

    public static class Efectivo extends MetodoPago {
        Efectivo() {
            super("Efectivo");
        }

        @Override
        protected String prefijo() {
            return "EFE";
        }
    }

    public static class Tarjeta extends MetodoPago {
        private final String ultimos4;

        Tarjeta(String dato) {
            super("Tarjeta");
            if (!dato.matches("\\d{4}")) {
                throw new PagoInvalidoException("Para tarjeta indique los últimos 4 dígitos (solo números).");
            }
            this.ultimos4 = dato;
        }

        @Override
        public String detalle() {
            return " ****" + ultimos4;
        }

        @Override
        protected String prefijo() {
            return "TRJ";
        }
    }

    public static class Sinpe extends MetodoPago {
        private final String telefono;

        Sinpe(String dato) {
            super("SINPE Móvil");
            if (!dato.matches("\\d{8}")) {
                throw new PagoInvalidoException("Para SINPE indique el teléfono de 8 dígitos.");
            }
            this.telefono = dato;
        }

        @Override
        public String detalle() {
            return " " + telefono;
        }

        @Override
        protected String prefijo() {
            return "SNP";
        }
    }
}
