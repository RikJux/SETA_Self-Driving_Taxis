package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Hello {

    private String id;
    private double ciao;

    public Hello() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getCiao() {
        return ciao;
    }

    public void setCiao(double ciao) {
        this.ciao = ciao;
    }

    public Hello(String id, double ciao) {
        this.id = id;
        this.ciao = ciao;
    }
}
