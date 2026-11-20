package restaurante.modelo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

// Consultas de estadisticas, se calculan con los pedidos y pagos guardados.
// El rango [desde, hasta] incluye los dos dias, y si es null no hay limite.
// Cada consulta usa su propia fecha: pedidos -> fecha del pedido, ventas -> fecha del pago,
// visitas por mesero -> fecha de cierre.
public class EstadisticasServicio {

    // lo que devuelve cada consulta: una tabla y la explicacion de como se calculo
    public static class Resultado {
        public final String titulo;
        public final String explicacion;
        public final String[] encabezados;
        public final List<String[]> filas = new ArrayList<>();

        Resultado(String titulo, String explicacion, String... encabezados) {
            this.titulo = titulo;
            this.explicacion = explicacion;
            this.encabezados = encabezados;
        }
    }

    private final Restaurante restaurante;

    public EstadisticasServicio(Restaurante restaurante) {
        this.restaurante = restaurante;
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        }
    }

    // dice si la fecha cae dentro del rango
    private static boolean enRango(LocalDateTime fechaHora, LocalDate desde, LocalDate hasta) {
        LocalDate fecha = fechaHora.toLocalDate();
        boolean despuesDeDesde = desde == null || !fecha.isBefore(desde);
        boolean antesDeHasta = hasta == null || !fecha.isAfter(hasta);
        return despuesDeDesde && antesDeHasta;
    }

    // 1. articulos mas solicitados (en general o de una categoria), sin contar los anulados
    public Resultado articulosMasSolicitados(String categoria, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        Map<String, Integer> cantidades = new HashMap<>();
        Map<String, Long> montos = new HashMap<>();
        for (Visita visita : restaurante.todasLasVisitas()) {
            for (Visita.Linea linea : visita.getLineas()) {
                if (linea.isAnulada()) {
                    continue;
                }
                if (!enRango(linea.getFechaHora(), desde, hasta)) {
                    continue;
                }
                if (categoria != null && !linea.getCategoria().equals(categoria)) {
                    continue;
                }
                cantidades.merge(linea.getProducto(), linea.getCantidad(), Integer::sum);
                montos.merge(linea.getProducto(), linea.getSubtotal(), Long::sum);
            }
        }

        Resultado resultado = new Resultado("Artículos más solicitados",
                "Suma la cantidad de cada artículo en pedidos NO anulados cuya fecha de pedido cae en el rango; orden: cantidad descendente, luego nombre.",
                "Artículo", "Unidades", "Monto");

        // se ordena por cantidad (mayor primero) y si empatan por nombre
        List<String> nombres = new ArrayList<>(cantidades.keySet());
        nombres.sort((a, b) -> {
            int cantA = cantidades.get(a);
            int cantB = cantidades.get(b);
            if (cantA != cantB) {
                return cantB - cantA;
            }
            return a.compareToIgnoreCase(b);
        });
        for (String nombre : nombres) {
            resultado.filas.add(new String[]{nombre, "" + cantidades.get(nombre), Visita.colones(montos.get(nombre))});
        }
        return resultado;
    }

    // 2. ventas cobradas: suma de los pagos, segun la fecha del pago
    public Resultado ventasCobradas(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        long total = 0;
        int cantidadPagos = 0;
        for (Visita visita : restaurante.todasLasVisitas()) {
            for (Visita.Pago pago : visita.getPagos()) {
                if (enRango(pago.getFechaHora(), desde, hasta)) {
                    total += pago.getMonto();
                    cantidadPagos++;
                }
            }
        }

        Resultado resultado = new Resultado("Ventas cobradas",
                "Suma el monto de los pagos cuya fecha de pago cae en el rango (no incluye lo consumido pero aún sin pagar).",
                "Pagos", "Total cobrado");
        if (cantidadPagos > 0) {
            resultado.filas.add(new String[]{"" + cantidadPagos, Visita.colones(total)});
        }
        return resultado;
    }

    // 3. consumo por categoria, segun la fecha del pedido
    public Resultado ventasPorCategoria(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        Map<String, Long> porCategoria = new TreeMap<>();
        for (Visita visita : restaurante.todasLasVisitas()) {
            for (Visita.Linea linea : visita.getLineas()) {
                if (!linea.isAnulada() && enRango(linea.getFechaHora(), desde, hasta)) {
                    porCategoria.merge(linea.getCategoria(), linea.getSubtotal(), Long::sum);
                }
            }
        }

        Resultado resultado = new Resultado("Consumo por categoría",
                "Suma precio aplicado × cantidad de pedidos no anulados, agrupado por categoría, según la fecha del pedido.",
                "Categoría", "Monto");
        for (Map.Entry<String, Long> e : porCategoria.entrySet()) {
            resultado.filas.add(new String[]{e.getKey(), Visita.colones(e.getValue())});
        }
        return resultado;
    }

    // 4. visitas cerradas por mesero y duracion promedio, segun la fecha de cierre
    public Resultado visitasPorMesero(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        Map<String, List<Visita>> porMesero = new TreeMap<>();
        for (Visita visita : restaurante.getHistorial()) {
            if (enRango(visita.getCierre(), desde, hasta)) {
                if (!porMesero.containsKey(visita.getMesero())) {
                    porMesero.put(visita.getMesero(), new ArrayList<>());
                }
                porMesero.get(visita.getMesero()).add(visita);
            }
        }

        Resultado resultado = new Resultado("Visitas cerradas por mesero",
                "Cuenta las visitas cuya fecha de cierre cae en el rango; duración = cierre − apertura, promediada en minutos.",
                "Mesero", "Visitas", "Total cobrado", "Duración prom. (min)");
        for (Map.Entry<String, List<Visita>> e : porMesero.entrySet()) {
            List<Visita> visitas = e.getValue();
            long totalCobrado = 0;
            long segundos = 0;
            for (Visita v : visitas) {
                totalCobrado += v.totalPagado();
                segundos += Duration.between(v.getApertura(), v.getCierre()).getSeconds();
            }
            double minutosPromedio = segundos / 60.0 / visitas.size();
            resultado.filas.add(new String[]{
                    e.getKey(),
                    "" + visitas.size(),
                    Visita.colones(totalCobrado),
                    String.format(Locale.US, "%.1f", minutosPromedio)});
        }
        return resultado;
    }
}
