package ca.corefacility.bioinformatics.irida.web.assembler.resource;

import ca.corefacility.bioinformatics.irida.model.User;
import java.net.URI;
import java.util.UUID;
import org.springframework.hateoas.ResourceSupport;

/**
 * Wrapper for exposing User resources to the web with linking support.
 * 
 * @author Josh Adam <josh.adam@phac-aspc.gc.ca>
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 */
public class UserResource extends ResourceSupport {
    private User user;
    
    public UserResource(User user) {
        this.user = user;
    }
    
    public URI getURI() {
        return user.getIdentifier().getUri();
    }
    
    public String getUsername() {
        return user.getUsername();
    }
    
    public String getEmail() {
        return user.getEmail();
    }
    
    public String getFirstName() {
        return user.getFirstName();
    }
    
    public String getLastName() {
        return user.getLastName();
    }
    
    public String getPhoneNumber() {
        return user.getPhoneNumber();
    }
}
