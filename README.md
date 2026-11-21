# Sistema de atención y menú inteligente para restaurantes

Proyecto de Programación Orientada a Objetos. Hecho en Java con Swing (no usa librerías externas).

## Cómo ejecutarlo

Se necesita JDK 17 o más nuevo.

La forma más fácil es abrir `Proyecto2_POO_Restaurante.jar` con doble clic, o desde la consola:

```
java -jar Proyecto2_POO_Restaurante.jar
```

Para compilarlo desde el código:

Desde la carpeta del proyecto:

```
mkdir out
javac -encoding UTF-8 -d out src/restaurante/*.java src/restaurante/modelo/*.java src/restaurante/excepciones/*.java src/restaurante/ui/*.java
java -cp out restaurante.Main
```

Para correr las pruebas de las excepciones:

```
java -Dfile.encoding=UTF-8 -cp out restaurante.PruebasConsola
```

También se puede abrir la carpeta `src` en IntelliJ, Eclipse o NetBeans y correr `restaurante.Main`.

## Cómo está organizado

- `modelo`: las clases del sistema (Restaurante, Menu, OpcionMenu, Mesa, EstadoMesa, Visita, MetodoPago, Combinacion, los dos algoritmos y las estadísticas).
- `excepciones`: las 4 excepciones propias.
- `ui`: la interfaz gráfica. Solo muestra datos y llama al modelo, las reglas están en el modelo.

## Requisitos de POO

- **Encapsulamiento:** los atributos son privados. Los métodos que cambian una visita o el estado de una mesa no son públicos, entonces la interfaz no puede cambiar el estado de una mesa directamente. Las listas se devuelven de solo lectura. Las validaciones están en el modelo (Visita, OpcionMenu, Menu, MetodoPago).
- **Interfaces:** `ObservadorRestaurante` y `EstrategiaCombinacion`.
- **Clase abstracta:** `MetodoPago`, con las subclases `Efectivo`, `Tarjeta` y `Sinpe`. Cada una valida su propio dato.
- **Polimorfismo:** `Restaurante` puede usar cualquier `EstrategiaCombinacion`, la `Mesa` le pasa las operaciones a su `EstadoMesa`, y la búsqueda del menú recibe cualquier criterio (`Predicate`).

## Patrones

1. **State:** `EstadoMesa` es un enum donde cada estado define qué se puede hacer. DISPONIBLE → OCUPADA ⇄ PENDIENTE_PAGO → DISPONIBLE. Lo que no se permite lanza error. Otra opción era un `switch` dentro de `Mesa`. La desventaja es que la lógica queda repartida entre los estados.
2. **Strategy:** `BacktrackingConPoda` y `FuerzaBruta` implementan `EstrategiaCombinacion` y se escoge en la pantalla. Para agregar otro algoritmo basta con hacer otra clase.
3. **Observer:** la `Mesa` avisa a los `ObservadorRestaurante` (los paneles y la bitácora) sin saber quiénes son. La alternativa era que cada panel revisara cada cierto tiempo.

## Excepciones

| Excepción | Cuándo pasa | Quién la lanza | Dónde se atiende |
|---|---|---|---|
| `MesaNoDisponibleException` | abrir una mesa ocupada o que no existe | `EstadoMesa`, `Restaurante.mesa` | `Ui.ejecutar` |
| `PedidoEnVisitaCerradaException` | pedir o anular sin visita abierta | `EstadoMesa`, `Visita` | `Ui.ejecutar` |
| `PagoInvalidoException` | monto ≤ 0, mayor al saldo, método o dato malo, cerrar con saldo | `Visita`, `MetodoPago` | `Ui.ejecutar` |
| `LimiteCaloricoSinResultadosException` | categoría vacía o ninguna combinación cabe en X | `Restaurante.buscarCombinaciones` | `PanelCombinaciones` (mensaje en rojo) |

En todos los casos se valida antes de cambiar algo, así que nada queda a medias. `PruebasConsola` prueba cada caso.

## Algoritmo del filtro de calorías

Regla: la proteína elegida + **una** opción de cada categoría activa (que no sea Proteínas), y la suma tiene que ser ≤ X.

1. Se sacan las categorías requeridas con sus opciones activas. Si alguna está vacía se lanza la excepción.
2. Se ordenan las categorías de menos a más opciones, y las opciones de menos a más calorías.
3. `minRestante[i]` = la suma de la opción más liviana de cada categoría desde i hasta el final.
4. Backtracking: en la categoría i, si `acumulado + opción + minRestante[i+1] > X` se hace `break`, porque las opciones que siguen pesan igual o más.
5. Al final se ordenan por calorías y después por nombre.

**Por qué es correcto:** solo se llega al final si la suma cabe, así que no hay combinaciones de más. La poda solo quita ramas donde ni lo mínimo cabe, así que no se pierde ninguna. Cada combinación sale una sola vez, entonces no hay repetidas. Además la prueba compara el resultado contra fuerza bruta y da lo mismo.

**Complejidad:** en el peor caso es el producto de las opciones de cada categoría (cuando casi todo cabe), pero con un X bajo explora mucho menos. Con el menú de ejemplo y 900 kcal: 89 nodos con poda contra 196 con fuerza bruta.

## Supuestos

- Java 17+, Swing, sin persistencia (todo está en memoria y el menú de ejemplo se carga al iniciar).
- Una opción por categoría en las combinaciones. Toda categoría activa que no sea Proteínas es requerida (se activan o desactivan en la pestaña Menú).
- Los montos son colones enteros (`long`), sin impuestos. El total es la suma de los precios.
- Un pedido se puede anular solo antes de pedir la cuenta. La línea queda guardada con el motivo pero ya no suma. No se puede anular si lo pagado quedaría mayor que el nuevo total.
- No hay vuelto: no se puede pagar más que el saldo.
- Una opción que ya tiene pedidos no se puede eliminar, solo desactivar. Puede haber varias mesas abiertas al mismo tiempo.
- Estadísticas: artículos más solicitados, ventas cobradas, consumo por categoría y visitas por mesero, con rango de fechas AAAA-MM-DD (vacío = sin límite).
