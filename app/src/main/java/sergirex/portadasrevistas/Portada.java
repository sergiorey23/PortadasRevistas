package sergirex.portadasrevistas;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sergio on 08/06/2017.
 * Nothing else to add
 */

public class Portada implements Serializable {
    private final String id;
    private final String title;
    private final String webRevista;
    private final String revista;
    private List<String> calendar;
    private String currentDate;

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

    public String getWebRevista() {
        return webRevista;
    }

    public String getRevista() {
        return revista;
    }

    public List<String> getCalendar() {
        return calendar;
    }

    public void setCalendar(String[] calendar) {
        this.calendar = new ArrayList<>(Arrays.asList(calendar));
        this.currentDate = getLastDate();
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public String getLastDate() {
        return calendar.get(0);
    }

    public String getDate(int index) {
        return calendar.get(index);
    }
}
