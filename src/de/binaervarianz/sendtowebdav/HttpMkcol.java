package de.binaervarianz.sendtowebdav;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;


public class HttpMkcol extends HttpEntityEnclosingRequestBase {

    public final static String METHOD_NAME = "MKCOL";
    
    public HttpMkcol() {
        super();
    }
    
    public HttpMkcol(final URI uri) {
        super();
        setURI(uri);
    }

    /**
     * @throws IllegalArgumentException if the uri is invalid. 
     */
    public HttpMkcol(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
    
}
