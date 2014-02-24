package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import static play.data.Form.*;

import models.*;
import views.html.*;
import models.User;

public class Shits extends Controller {
    
    public static Result signup() {
        return ok();//signup.render(form(Signup.class)));
    }
    
    public static Result newSignUp() {
        Form<Signup> signupForm = form(Signup.class).bindFromRequest();
        
        if(signupForm.hasErrors()) {
            return badRequest();//signup.render(signupForm));
        } else { 
            return redirect(routes.Application.index());
        }
        
        
    }    
    
    public static class Signup {
        public String email;
        public String firstName;
        public String password;
        
        
        public String validate() {
            
            System.out.println("First One: " + email);
            
            if(User.isNewUser(email) != null) {
                return "User already exists";
            }
            
            return null;
                
        }
    }
}
    
    
    
    
    