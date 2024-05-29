package sergirex.portadasrevistas;

import java.io.Serializable;

/**
 * Created by Sergio on 08/06/2017.
 * Nothing else to add
 */

class Portada implements Serializable{
    private final String id;
    private final String title;
    private final String webRevista;
    private final String revista;

    Portada(String id, String title, String webRevista, String revista) {
        this.id = id;
        this.title = title;
        this.webRevista = webRevista;
        this.revista = revista;
    }

    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getWebRevista() { return webRevista; }

    public String getRevista() {
        return revista;
    }
}
