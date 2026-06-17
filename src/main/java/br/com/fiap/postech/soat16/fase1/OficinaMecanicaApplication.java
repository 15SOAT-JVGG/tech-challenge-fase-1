package br.com.fiap.postech.soat16.fase1;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@ApplicationPath("/")
@OpenAPIDefinition(
    info = @Info(
        title = "Mechanic Workshop API",
        description = "Integrated System for Service Order Handling and Execution",
        version = "1.0.0"
    )
)
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer Token — use POST /auth/login to obtain the token"
)
public class OficinaMecanicaApplication extends Application {
}
