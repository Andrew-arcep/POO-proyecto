package restaurante.modelo;

import java.util.Comparator;
import java.util.List;

// Una comida completa: la proteina mas una opcion de cada categoria.
public class Combinacion {

    // primero por calorias y si empatan por nombres
    public static final Comparator<Combinacion> ORDEN =
            Comparator.comparingInt(Combinacion::getCalorias).thenComparing(Combinacion::clave);

    private final List<OpcionMenu> opciones;
    private final int calorias;
    private final long precio;

    public Combinacion(List<OpcionMenu> opciones) {
        this.opciones = List.copyOf(opciones);
        int totalCalorias = 0;
        long totalPrecio = 0;
        for (OpcionMenu o : opciones) {
            totalCalorias += o.getCalorias();
            totalPrecio += o.getPrecio();
        }
        this.calorias = totalCalorias;
        this.precio = totalPrecio;
    }

    public List<OpcionMenu> getOpciones() {
        return opciones;
    }

    public int getCalorias() {
        return calorias;
    }

    public long getPrecio() {
        return precio;
    }

    // texto con los nombres, sirve para ordenar y para comparar combinaciones
    public String clave() {
        String texto = "";
        for (OpcionMenu o : opciones) {
            texto += o.getNombre().toLowerCase() + "|";
        }
        return texto;
    }
}
