package mb.fw.atb.config.sub;

import lombok.Data;

@Data
public class Oauth2 {
    String accessTokenUri = "";
    String clientId = "";
    String clientSecret = "";
}