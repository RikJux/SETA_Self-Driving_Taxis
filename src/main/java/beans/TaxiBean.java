package beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TaxiBean {

    private String id;
    private String ip;
    private int port;

    public TaxiBean(){};

    public TaxiBean(String id, String ip, int port){
        this.id =  id;
        this.ip = ip;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "TaxiBean{" +
                "id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
