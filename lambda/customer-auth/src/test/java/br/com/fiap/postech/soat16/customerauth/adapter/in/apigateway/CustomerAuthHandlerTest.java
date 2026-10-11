package br.com.fiap.postech.soat16.customerauth.adapter.in.apigateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.customerauth.adapter.out.persistence.CustomerLookupException;
import br.com.fiap.postech.soat16.customerauth.application.CustomerAuthenticationService;
import br.com.fiap.postech.soat16.customerauth.application.IssuedToken;
import br.com.fiap.postech.soat16.customerauth.domain.exception.CustomerNotAuthorizedException;
import br.com.fiap.postech.soat16.customerauth.domain.exception.InvalidCpfException;
import br.com.fiap.postech.soat16.customerauth.support.FakeLambdaContext;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAuthHandler — Testes unitários")
class CustomerAuthHandlerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CPF = "529.982.247-25";
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Mock
    private CustomerAuthenticationService authentication;

    private CustomerAuthHandler handler;
    private FakeLambdaContext context;

    @BeforeEach
    void setUp() {
        handler = new CustomerAuthHandler(authentication);
        context = new FakeLambdaContext();
    }

    private static APIGatewayV2HTTPEvent request(String body) {
        return APIGatewayV2HTTPEvent.builder().withBody(body).build();
    }

    private static JsonNode body(APIGatewayV2HTTPResponse response) throws Exception {
        return JSON.readTree(response.getBody());
    }

    @Nested
    @DisplayName("sucesso")
    class Success {

        @Test
        @DisplayName("deve responder 200 com o token, o id do cliente, o papel e a validade")
        void shouldReturnToken() throws Exception {
            when(authentication.authenticate(CPF))
                    .thenReturn(new IssuedToken("signed.jwt.token", CUSTOMER_ID, "CUSTOMER", 3600));

            APIGatewayV2HTTPResponse response = handler.handleRequest(request("{\"cpf\":\"" + CPF + "\"}"), context);

            assertEquals(200, response.getStatusCode());
            assertEquals("application/json", response.getHeaders().get("Content-Type"));
            JsonNode body = body(response);
            assertEquals("signed.jwt.token", body.get("token").asText());
            assertEquals(CUSTOMER_ID.toString(), body.get("customerId").asText());
            assertEquals("CUSTOMER", body.get("role").asText());
            assertEquals(3600, body.get("expiresIn").asLong());
        }

        @Test
        @DisplayName("deve decodificar o corpo que o API Gateway entrega em base64")
        void shouldDecodeBase64Body() {
            when(authentication.authenticate(CPF))
                    .thenReturn(new IssuedToken("signed.jwt.token", CUSTOMER_ID, "CUSTOMER", 3600));
            String encoded = Base64.getEncoder()
                    .encodeToString(("{\"cpf\":\"" + CPF + "\"}").getBytes(StandardCharsets.UTF_8));
            APIGatewayV2HTTPEvent event = APIGatewayV2HTTPEvent.builder()
                    .withBody(encoded)
                    .withIsBase64Encoded(true)
                    .build();

            assertEquals(200, handler.handleRequest(event, context).getStatusCode());
        }

        @Test
        @DisplayName("deve registrar o login em JSON sem o CPF")
        void shouldLogWithoutCpf() {
            when(authentication.authenticate(CPF))
                    .thenReturn(new IssuedToken("signed.jwt.token", CUSTOMER_ID, "CUSTOMER", 3600));

            handler.handleRequest(request("{\"cpf\":\"" + CPF + "\"}"), context);

            String line = context.logLines().getFirst();
            assertTrue(line.contains("\"event\":\"customer_authenticated\""));
            assertTrue(line.contains(CUSTOMER_ID.toString()));
            assertTrue(line.contains(FakeLambdaContext.REQUEST_ID));
            assertFalse(line.contains("52998224725"));
            assertFalse(line.contains(CPF));
        }
    }

    @Nested
    @DisplayName("erros")
    class Errors {

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "não é json", "{}", "{\"documento\":\"52998224725\"}", "null"})
        @DisplayName("deve responder 400 INVALID_REQUEST ao corpo ausente ou sem cpf")
        void shouldRejectMalformedBody(String rawBody) throws Exception {
            APIGatewayV2HTTPResponse response = handler.handleRequest(request(rawBody), context);

            assertEquals(400, response.getStatusCode());
            assertEquals("INVALID_REQUEST", body(response).get("code").asText());
            verifyNoInteractions(authentication);
        }

        @Test
        @DisplayName("deve responder 400 INVALID_REQUEST quando não há corpo algum")
        void shouldRejectMissingBody() throws Exception {
            APIGatewayV2HTTPResponse response = handler.handleRequest(request(null), context);

            assertEquals(400, response.getStatusCode());
            assertEquals("INVALID_REQUEST", body(response).get("code").asText());
        }

        @Test
        @DisplayName("deve responder 400 INVALID_CPF no formato de erro da API")
        void shouldRejectInvalidCpf() throws Exception {
            when(authentication.authenticate("123")).thenThrow(new InvalidCpfException());

            APIGatewayV2HTTPResponse response = handler.handleRequest(request("{\"cpf\":\"123\"}"), context);

            assertEquals(400, response.getStatusCode());
            JsonNode body = body(response);
            assertEquals(400, body.get("status").asInt());
            assertEquals("VALIDATION", body.get("type").asText());
            assertEquals("INVALID_CPF", body.get("code").asText());
            assertEquals("/v1/auth/customer", body.get("path").asText());
            assertEquals(FakeLambdaContext.REQUEST_ID, body.get("traceId").asText());
            assertTrue(body.hasNonNull("timestamp"));
        }

        @Test
        @DisplayName("deve responder 401 sem dizer se o CPF é de cliente inexistente ou inativo")
        void shouldReturnUnauthorized() throws Exception {
            when(authentication.authenticate(CPF)).thenThrow(new CustomerNotAuthorizedException("inactive"));

            APIGatewayV2HTTPResponse response = handler.handleRequest(request("{\"cpf\":\"" + CPF + "\"}"), context);

            assertEquals(401, response.getStatusCode());
            JsonNode body = body(response);
            assertEquals("UNAUTHORIZED", body.get("type").asText());
            assertEquals("CUSTOMER_NOT_AUTHORIZED", body.get("code").asText());
            assertFalse(response.getBody().contains("inactive"));
            assertTrue(context.logLines().getFirst().contains("\"reason\":\"inactive\""));
        }

        @Test
        @DisplayName("deve responder 500 sem vazar a causa quando o banco falha")
        void shouldReturnInternalErrorOnDatabaseFailure() throws Exception {
            when(authentication.authenticate(CPF))
                    .thenThrow(new CustomerLookupException(new IllegalStateException("connection refused")));

            APIGatewayV2HTTPResponse response = handler.handleRequest(request("{\"cpf\":\"" + CPF + "\"}"), context);

            assertEquals(500, response.getStatusCode());
            JsonNode body = body(response);
            assertEquals("INTERNAL_ERROR", body.get("code").asText());
            assertFalse(body.has("type"));
            assertFalse(response.getBody().contains("connection refused"));
            assertTrue(context.logLines().getFirst().contains("CustomerLookupException"));
        }
    }
}
