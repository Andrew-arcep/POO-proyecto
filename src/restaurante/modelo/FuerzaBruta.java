package restaurante.modelo;

import java.util.ArrayList;
import java.util.List;

// Fuerza bruta: arma todas las combinaciones posibles y al final
// se queda solo con las que no pasan del limite.
// Sirve para comparar contra el backtracking con poda.
public class FuerzaBruta implements EstrategiaCombinacion {

    private long nodos;

    public String nombre() {
        return "Fuerza bruta (producto cartesiano)";
    }

    public long nodosExplorados() {
        return nodos;
    }

    public List<Combinacion> generar(OpcionMenu proteina, List<List<OpcionMenu>> porCategoria, int max) {
        nodos = 0;

        // empieza solo con la proteina y en cada categoria va agregando una opcion
        List<List<OpcionMenu>> parciales = new ArrayList<>();
        List<OpcionMenu> inicio = new ArrayList<>();
        inicio.add(proteina);
        parciales.add(inicio);

        for (List<OpcionMenu> categoria : porCategoria) {
            List<List<OpcionMenu>> siguientes = new ArrayList<>();
            for (List<OpcionMenu> parcial : parciales) {
                for (OpcionMenu opcion : categoria) {
                    nodos++;
                    List<OpcionMenu> nueva = new ArrayList<>(parcial);
                    nueva.add(opcion);
                    siguientes.add(nueva);
                }
            }
            parciales = siguientes;
        }

        // filtro final por calorias
        List<Combinacion> resultado = new ArrayList<>();
        for (List<OpcionMenu> lista : parciales) {
            Combinacion c = new Combinacion(lista);
            if (c.getCalorias() <= max) {
                resultado.add(c);
            }
        }
        return resultado;
    }
}
