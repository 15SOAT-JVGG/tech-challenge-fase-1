package br.com.fiap.postech.soat16.customerauth.adapter.in.apigateway;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fiap.postech.soat16.customerauth.application.CustomerAuthenticationService;
import br.com.fiap.postech.soat16.customerauth.application.IssuedToken;
import br.com.fiap.postech.soat16.customerauth.config.CustomerAuthConfiguration;
import br.com.fiap.postech.soat16.customerauth.config.CustomerAuthModule;
import br.com.fiap.postech.soat16.customerauth.domain.exception.CustomerNotAuthorizedException;
import br.com.fiap.postech.soat16.customerauth.domain.exception.InvalidCpfException;

/**
 * Integração Lambda proxy do API Gateway HTTP (payload 2.0) para {@code POST /v1/auth/customer}.
 * As respostas de erro seguem o formato do {@code ApiErrorResponseDto} da API, para que o cliente
 * trate um único contrato de erro, venha ele da função ou do cluster.
 */
public class CustomerAuthHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Map<String, String> JSON_HEADERS = Map.of("Content-Type", "application/json");
    private static final String PATH = "/v1/auth/customer";

    private final CustomerAuthenticationService authentication;

    /**
     * Construtor usado pelo runtime da Lambda.
     */
    public CustomerAuthHandler() {
        this(CustomerAuthModule.build(CustomerAuthConfiguration.from(System.getenv())));
    }

    public CustomerAuthHandler(CustomerAuthenticationService authentication) {
        this.authentication = authentication;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        String requestId = context.getAwsRequestId();
        try {
            CustomerLoginRequest request = readBody(event);
            IssuedToken issued = authentication.authenticate(request.cpf());
            log(context, "INFO", "customer_authenticated", Map.of("customerId", issued.customerId().toString()));
            return respond(200, new CustomerLoginResponse(
                    issued.token(), issued.customerId(), issued.role(), issued.expiresIn()));
        } catch (MalformedRequestException exception) {
            log(context, "WARN", "customer_auth_rejected", Map.of("reason", "malformed-request"));
            return error(400, "VALIDATION", "INVALID_REQUEST", "Corpo esperado: {\"cpf\": \"...\"}", requestId);
        } catch (InvalidCpfException exception) {
            log(context, "WARN", "customer_auth_rejected", Map.of("reason", "invalid-cpf"));
            return error(400, "VALIDATION", "INVALID_CPF", exception.getMessage(), requestId);
        } catch (CustomerNotAuthorizedException exception) {
            log(context, "WARN", "customer_auth_rejected", Map.of("reason", exception.reason()));
            return error(401, "UNAUTHORIZED", "CUSTOMER_NOT_AUTHORIZED", exception.getMessage(), requestId);
        } catch (RuntimeException exception) {
            log(context, "ERROR", "customer_auth_failed", Map.of(
                    "error", exception.getClass().getSimpleName(),
                    "message", String.valueOf(exception.getMessage())));
            return error(500, null, "INTERNAL_ERROR", "Falha ao autenticar o cliente", requestId);
        }
    }

    private CustomerLoginRequest readBody(APIGatewayV2HTTPEvent event) {
        String body = event.getBody();
        if (body == null || body.isBlank()) {
            throw new MalformedRequestException();
        }
        if (event.getIsBase64Encoded()) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        try {
            CustomerLoginRequest request = JSON.readValue(body, CustomerLoginRequest.class);
            if (request == null || request.cpf() == null) {
                throw new MalformedRequestException();
            }
            return request;
        } catch (JsonProcessingException exception) {
            throw new MalformedRequestException();
        }
    }

    private APIGatewayV2HTTPResponse error(int status, String type, String code, String message, String requestId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        if (type != null) {
            body.put("type", type);
        }
        body.put("code", code);
        body.put("message", message);
        body.put("path", PATH);
        body.put("traceId", requestId);
        body.put("timestamp", Instant.now().toString());
        return respond(status, body);
    }

    private APIGatewayV2HTTPResponse respond(int status, Object body) {
        try {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(status)
                    .withHeaders(JSON_HEADERS)
                    .withBody(JSON.writeValueAsString(body))
                    .build();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar a resposta", exception);
        }
    }

    /**
     * Uma linha JSON por evento: o CloudWatch Logs Insights e o agente de observabilidade indexam os
     * campos sem regex. O CPF nunca entra no log.
     */
    private void log(Context context, String level, String event, Map<String, String> fields) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("timestamp", Instant.now().toString());
        line.put("level", level);
        line.put("service", "customer-auth");
        line.put("event", event);
        line.put("requestId", context.getAwsRequestId());
        line.putAll(fields);
        try {
            context.getLogger().log(JSON.writeValueAsString(line) + System.lineSeparator());
        } catch (JsonProcessingException exception) {
            context.getLogger().log(level + " " + event + System.lineSeparator());
        }
    }

    private static final class MalformedRequestException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }
}
