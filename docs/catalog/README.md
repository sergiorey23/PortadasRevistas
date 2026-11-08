# Catálogo de revistas

`revistas.json` lo descarga la app (`CatalogConfig.URL`) y lo usa en lugar del que lleva dentro, si es más nuevo.
Se publica con GitHub Pages desde la carpeta `docs/`.

## Cómo añadir, quitar o renombrar una revista

1. Edita `app/src/main/assets/revistas.json` (la copia de la app).
2. **Sube `catalogVersion`** en 1. La app solo adopta un catálogo con versión igual o mayor a la que ya tiene.
3. Copia el archivo tal cual a `docs/catalog/revistas.json` (un test falla si las dos copias difieren).
4. Haz commit y push a `master`; Pages lo publica en 1-2 minutos y la app lo recoge en su siguiente arranque o al volver al primer plano (como mucho cada 6 horas).

Cada entrada necesita `id` (el de kioskoymas.com), `name` (lo que ve el usuario) y `category` (`CORAZON`, `CULTURA`,
`DEPORTES`, `ESTILO_DE_VIDA`, `FEMENINAS`, `MASCULINAS`, `MOTOR`, `TECNOLOGIA`, `COCINA` o `HISTORIA`). Opcionales:
`web` (sin `https://www.`), `pages` (páginas del lector, 100 por defecto) y `scale` (escala de las páginas, "74" por
defecto; la app la baja sola si el servidor no la acepta). Una entrada con nombre vacío, una categoría desconocida, un
id repetido o un número de páginas no válido se ignora sin afectar a las demás. Si el archivo entero no es válido (o
tiene menos de 10 entradas) la app lo rechaza y sigue con el catálogo anterior.
