package restaurante.modelo;

// Interfaz del patron Observer.
// La mesa avisa los cambios sin saber quien los esta escuchando.
// mesa = 0 cuando el cambio es del menu.
public interface ObservadorRestaurante {

    void actualizar(int mesa, String detalle);
}
