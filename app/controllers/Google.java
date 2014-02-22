package controllers;

import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.*;
import play.libs.WS;
import play.libs.F.Promise;
import com.fasterxml.jackson.databind.JsonNode;



import views.html.*;

public class Google extends Controller {
    
    private static String oauth2 = "https://accounts.google.com/o/oauth2/auth?";
    private static String response_type = "response_type=code";
    private static String client_id = "&client_id=909061507773-dv7c1dsk7n1cs9q368l7t5u8bqnk1u3s.apps.googleusercontent.com";
    private static String redirect_uri = "&redirect_uri=http://localhost:9000/googleauth2";
    private static String scope = "&scope=https://mail.google.com/+email";//"&scope=email";
    private static String approval_prompt = "&approval_prompt=force";
    private static String access_type = "&access_type=offline";
    
    private static String oauth3 = "https://accounts.google.com/o/oauth2/token";
    private static String client_secret = "&client_secret=_2eb28Gm6rbKAsMKYaKS25Dy";
    private static String grant_type = "&grant_type=authorization_code";
    
    private static String userinfo = "https://www.googleapis.com/oauth2/v2/userinfo";


    
    public void unAuth() {
        
    }
    
    public static Result auth () {
        return redirect(oauth2+response_type+client_id+redirect_uri+scope+approval_prompt+access_type);
    }
    
    public static Result auth2(String code) {
        
        System.out.println(code);
        String codeX = "code=" + code;
        
        Promise<WS.Response> result = WS.url(oauth3).setContentType("application/x-www-form-urlencoded").post(codeX+client_id+client_secret+redirect_uri+grant_type);
        WS.Response response = result.get();
        JsonNode json = response.asJson();

        String access_token = json.get("access_token").asText();
        String token_type = json.get("token_type").asText();
        String refresh_token = json.get("refresh_token").asText();

        System.out.println(json);
        
        System.out.println("Access Token: " + json.get("access_token").asText());
        System.out.println("Token Type: " + json.get("token_type").asText());
        System.out.println("Refresh Token: " + json.get("refresh_token").asText());
        
        Promise<WS.Response> resultUser = WS.url(userinfo).setHeader("Authorization", token_type + " " + access_token).get();
        WS.Response responseUser = resultUser.get();
        JsonNode jsonUser = responseUser.asJson();
        
        String email = jsonUser.get("email").asText();
        
        System.out.println("Email: " + email);
        
        User.addGoogleOauth(email,access_token,refresh_token);
        session("refresh_token", refresh_token);
        
        return ok(google.render());
    }
    
    public static Result auth2(String access_token, String refresh_token, String expires_in, String token_type) {
        
        System.out.println("Hit me!");
        
        return ok();
    }
}