package iwish.common;
import java.io.Serializable;
public class Response implements Serializable { public boolean ok; public String message; public Object payload; public Response(boolean ok,String message,Object payload){this.ok=ok;this.message=message;this.payload=payload;} public static Response ok(String m,Object p){return new Response(true,m,p);} public static Response fail(String m){return new Response(false,m,null);} }
