package br.com.fiap.postech.soat16.fase1.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Customer API",
                version = "1.0.0",
                description = "Customer management service."
        )
)
@ApplicationScoped
public class OpenApiConfig {
}
