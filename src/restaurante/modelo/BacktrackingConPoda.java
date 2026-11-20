package restaurante.modelo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// Backtracking con poda.
// Se ordenan las categorias de la que tiene menos opciones a la que tiene mas,
// y las opciones de cada categoria de menos a mas calorias.
// Poda: si lo acumulado + la opcion + lo minimo que falta ya se pasa del limite,
// se corta el for (las que siguen pesan igual o mas, asi que no se pierde ninguna solucion).
public class BacktrackingConPoda implements EstrategiaCombinacion {

    private long nodos;

    public String nombre() {
        return "Backtracking con poda";
    }

    public long nodosExplorados() {
        return nodos;
    }

    public List<Combinacion> generar(OpcionMenu proteina, List<List<OpcionMenu>> porCategoria, int max) {
        nodos = 0;
        List<Combinacion> resultado = new ArrayList<>();
        int k = porCategoria.size();

        // si la proteina sola ya se pasa no hay nada que buscar
        if (proteina.getCalorias() > max) {
            return resultado;
        }

        // orden[i] guarda cual categoria original va en la posicion i
        Integer[] orden = new Integer[k];
        for (int i = 0; i < k; i++) {
            orden[i] = i;
        }
        Arrays.sort(orden, Comparator.comparingInt(i -> porCategoria.get(i).size()));

        // listas ya ordenadas por calorias
        List<List<OpcionMenu>> listas = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            List<OpcionMenu> copia = new ArrayList<>(porCategoria.get(orden[i]));
            copia.sort(Comparator.comparingInt(OpcionMenu::getCalorias));
            listas.add(copia);
        }

        // minRestante[i] = calorias minimas que todavia faltan desde la categoria i hasta el final
        int[] minRestante = new int[k + 1];
        for (int i = k - 1; i >= 0; i--) {
            minRestante[i] = minRestante[i + 1] + listas.get(i).get(0).getCalorias();
        }

        OpcionMenu[] elegidas = new OpcionMenu[k];
        explorar(0, proteina.getCalorias(), elegidas, listas, minRestante, max, orden, proteina, resultado);
        return resultado;
    }

    private void explorar(int nivel, int acumulado, OpcionMenu[] elegidas, List<List<OpcionMenu>> listas,
                          int[] minRestante, int max, Integer[] orden, OpcionMenu proteina, List<Combinacion> resultado) {
        nodos++;

        // caso base: ya se eligio una opcion de cada categoria
        if (nivel == listas.size()) {
            // se vuelven a poner en el orden original de las categorias
            OpcionMenu[] original = new OpcionMenu[elegidas.length];
            for (int i = 0; i < elegidas.length; i++) {
                original[orden[i]] = elegidas[i];
            }
            List<OpcionMenu> comida = new ArrayList<>();
            comida.add(proteina);
            comida.addAll(Arrays.asList(original));
            resultado.add(new Combinacion(comida));
            return;
        }

        for (OpcionMenu opcion : listas.get(nivel)) {
            // poda
            if (acumulado + opcion.getCalorias() + minRestante[nivel + 1] > max) {
                break;
            }
            elegidas[nivel] = opcion;
            explorar(nivel + 1, acumulado + opcion.getCalorias(), elegidas, listas, minRestante, max, orden, proteina, resultado);
        }
    }
}
