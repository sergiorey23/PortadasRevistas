package sergirex.portadasrevistas;

import java.io.Serializable;

/**
 * Created by Sergio on 08/06/2017.
 * Nothing else to add
 */

class Portada implements Serializable{
    private final String id;
    private final String title;
    private final String fecha;
    private final String webPeriodico;

    Portada(String id, String title, String fecha, String webPeriodico) {
        this.id = id;
        this.title = title;
        this.fecha = fecha;
        this.webPeriodico = webPeriodico;
    }

    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getWebPeriodico() { return webPeriodico; }
    public String getFecha() { return fecha; }

}
