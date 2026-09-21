package iwish.common;
import java.io.Serializable; import java.util.HashMap; import java.util.Map;
public class Request implements Serializable { public String action; public Map<String,Object> data=new HashMap<>(); public Request(String action){this.action=action;} public Request put(String k,Object v){data.put(k,v);return this;} }
