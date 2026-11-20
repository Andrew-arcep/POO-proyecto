package restaurante.modelo;

import java.util.List;

// Interfaz del patron Strategy.
// Cada algoritmo busca las comidas completas (una proteina + una opcion de cada categoria)
// que no pasen del limite de calorias.
public interface EstrategiaCombinacion {

    String nombre();

    List<Combinacion> generar(OpcionMenu proteina, List<List<OpcionMenu>> opcionesPorCategoria, int maxCalorias);

    long nodosExplorados();
}
