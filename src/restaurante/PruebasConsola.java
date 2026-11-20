package restaurante;

import java.util.List;
import restaurante.excepciones.*;
import restaurante.modelo.*;

// Pruebas hechas a mano (sin librerias). Para correrlas:
// java -Dfile.encoding=UTF-8 -cp out restaurante.PruebasConsola
// Cada caso provoca un error y revisa que la mesa, la cuenta y los pedidos no quedaran a medias.
public class PruebasConsola {

    static int correctas = 0;
    static int fallas = 0;

    static void verificar(String nombre, boolean condicion) {
        if (condicion) {
            correctas++;
            System.out.println("  [OK]    " + nombre);
        } else {
            fallas++;
            System.out.println("  [FALLA] " + nombre);
        }
    }

    // ejecuta la accion y revisa que lance justo la excepcion que se espera
    static void esperar(Class<? extends RuntimeException> tipo, Runnable accion, String nombre) {
        try {
            accion.run();
            verificar(nombre + " (no lanzó nada)", false);
        } catch (RuntimeException e) {
            verificar(nombre + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage(), tipo.isInstance(e));
        }
    }

    public static void main(String[] args) {
        Restaurante r = Restaurante.demo();

        System.out.println("== MesaNoDisponibleException ==");
        r.abrirVisita(1, "Carlos", List.of("Ana", "Luis"), true);
        Visita v1 = r.mesa(1).getVisita();
        esperar(MesaNoDisponibleException.class, () -> r.abrirVisita(1, "Otro", List.of("X"), false), "Abrir en mesa ocupada");
        verificar("La visita original sigue intacta", r.mesa(1).getVisita() == v1 && v1.getMesero().equals("Carlos"));
        esperar(MesaNoDisponibleException.class, () -> r.abrirVisita(99, "Carlos", List.of("X"), false), "Abrir en mesa inexistente");

        System.out.println("== Pedidos sucesivos y validaciones ==");
        r.pedir(1, "Ana", 5, 1);
        r.pedir(1, "Luis", 6, 1);
        r.pedir(1, "Ana", 13, 2);
        long total = v1.totalMesa();
        verificar("Pedidos sucesivos acumulan (3 líneas)", v1.getLineas().size() == 3);
        esperar(IllegalArgumentException.class, () -> r.pedir(1, "Fantasma", 5, 1), "Comensal que no pertenece a la visita");
        esperar(IllegalArgumentException.class, () -> r.pedir(1, "Ana", 5, 0), "Cantidad cero");
        verificar("Total sin cambios tras rechazos", v1.totalMesa() == total && v1.getLineas().size() == 3);

        System.out.println("== PagoInvalidoException ==");
        esperar(PagoInvalidoException.class, () -> r.pagar(1, "Ana", "Efectivo", "", 100), "Pagar sin solicitar cuenta");
        r.solicitarCuenta(1);
        esperar(IllegalStateException.class, () -> r.pedir(1, "Ana", 5, 1), "Pedir con la cuenta solicitada");
        long saldoAna = v1.saldoDe("Ana");
        esperar(PagoInvalidoException.class, () -> r.pagar(1, "Ana", "Efectivo", "", -5), "Monto negativo");
        esperar(PagoInvalidoException.class, () -> r.pagar(1, "Ana", "Efectivo", "", saldoAna + 1), "Monto mayor al saldo");
        esperar(PagoInvalidoException.class, () -> r.pagar(1, "Ana", "Bitcoin", "", 100), "Método no disponible");
        esperar(PagoInvalidoException.class, () -> r.pagar(1, "Ana", "Tarjeta", "12", 100), "Tarjeta con dato inválido");
        verificar("Saldo y pagos intactos tras rechazos", v1.saldoDe("Ana") == saldoAna && v1.getPagos().isEmpty());
        esperar(PagoInvalidoException.class, () -> r.cerrarVisita(1), "Cerrar con saldo pendiente");
        verificar("La mesa sigue pendiente de pago", r.mesa(1).getEstado() == EstadoMesa.PENDIENTE_PAGO && r.getHistorial().isEmpty());

        System.out.println("== Pago por persona y cierre ==");
        r.pagar(1, "Ana", "Tarjeta", "1234", saldoAna);
        r.pagar(1, "Luis", "SINPE Móvil", "88887777", v1.saldoDe("Luis"));
        verificar("Saldo total en cero", v1.saldo() == 0);
        r.cerrarVisita(1);
        verificar("Mesa disponible y visita en historial", r.mesa(1).estaDisponible() && r.getHistorial().size() == 1);

        System.out.println("== PedidoEnVisitaCerradaException ==");
        esperar(PedidoEnVisitaCerradaException.class, () -> r.pedir(1, "Ana", 5, 1), "Pedir sin visita abierta");
        verificar("Historial conserva pedidos y pagos", v1.getLineas().size() == 3 && v1.getPagos().size() == 2);
        r.abrirVisita(1, "Carlos", List.of("Nuevo"), false);
        Visita v2 = r.mesa(1).getVisita();
        verificar("La mesa puede reutilizarse", v2 != v1);

        System.out.println("== Anulación con auditoría ==");
        r.pedir(1, "Nuevo", 5, 1);
        r.anularPedido(1, 1, "Se equivocó");
        verificar("Línea anulada se conserva pero no suma", v2.getLineas().size() == 1 && v2.getLineas().get(0).isAnulada() && v2.totalMesa() == 0);
        esperar(IllegalArgumentException.class, () -> r.anularPedido(1, 1, "otra vez"), "Anular dos veces");

        System.out.println("== LimiteCaloricoSinResultadosException y algoritmo ==");
        esperar(LimiteCaloricoSinResultadosException.class, () -> r.buscarCombinaciones(5, 100, new BacktrackingConPoda()), "Límite 100 kcal imposible");

        BacktrackingConPoda poda = new BacktrackingConPoda();
        FuerzaBruta bruta = new FuerzaBruta();
        List<Combinacion> conPoda = r.buscarCombinaciones(5, 900, poda);
        List<Combinacion> conBruta = r.buscarCombinaciones(5, 900, bruta);

        // los dos algoritmos tienen que dar exactamente lo mismo
        boolean iguales = conPoda.size() == conBruta.size();
        for (int i = 0; iguales && i < conPoda.size(); i++) {
            if (!conPoda.get(i).clave().equals(conBruta.get(i).clave())) {
                iguales = false;
            }
        }
        verificar("Ambas estrategias devuelven lo mismo (" + conPoda.size() + " combinaciones)", iguales);

        boolean validas = true;
        int esperadas = 1 + r.getMenu().categoriasRequeridas().size();
        for (Combinacion c : conPoda) {
            if (c.getCalorias() > 900 || c.getOpciones().size() != esperadas) {
                validas = false;
            }
        }
        verificar("Todas respetan X y tienen una opción por categoría", validas);
        System.out.println("      nodos poda=" + poda.nodosExplorados() + " vs fuerza bruta=" + bruta.nodosExplorados());

        // se desactivan todos los postres y la categoria requerida queda vacia
        for (OpcionMenu o : r.getMenu().listarPorCategoria("Postres", false)) {
            r.setDisponible(o.getId(), false);
        }
        esperar(LimiteCaloricoSinResultadosException.class, () -> r.buscarCombinaciones(5, 900, poda), "Categoría requerida sin opciones activas");

        System.out.println("== Integridad del menú ==");
        esperar(IllegalArgumentException.class, () -> r.eliminarOpcion(5), "Eliminar opción con pedidos");
        esperar(IllegalArgumentException.class, () -> r.agregarOpcion("Mala", "Postres", "", -1, 100), "Calorías negativas");

        System.out.println("\nResultado: " + correctas + " correctas, " + fallas + " fallas");
        if (fallas > 0) {
            System.exit(1);
        }
    }
}
