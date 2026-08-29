# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.12-eclipse-temurin-21 AS builder

ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

WORKDIR /build

# Cache de dependências: copia apenas os descritores de build primeiro
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Configura credenciais do Nexus quando fornecidas
RUN if [ -n "$NEXUS_URL" ]; then \
      mkdir -p /root/.m2 && \
      printf '<settings>\n  <servers>\n    <server><id>nexus-snapshots</id><username>%s</username><password>%s</password></server>\n    <server><id>nexus-releases</id><username>%s</username><password>%s</password></server>\n  </servers>\n</settings>\n' \
        "$NEXUS_USERNAME" "$NEXUS_PASSWORD" "$NEXUS_USERNAME" "$NEXUS_PASSWORD" \
        > /root/.m2/settings.xml; \
    fi

RUN mvn dependency:go-offline -B -q \
      -Dnexus.url=${NEXUS_URL:-https://repo.maven.apache.org/maven2}

# Copia o código-fonte e executa o build
COPY src/ src/
RUN mvn package -B -DskipTests \
      -Dnexus.url=${NEXUS_URL:-https://repo.maven.apache.org/maven2}

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24

ENV LANGUAGE='en_US:en'

COPY --from=builder --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=builder --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=builder --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

# A imagem não carrega chave alguma: o par RS256 é gerado uma única vez fora do
# build e montado em tempo de execução (Secret no Kubernetes, volume no Compose).
# Sem JWT_PRIVATE_KEY_LOCATION / JWT_PUBLIC_KEY_LOCATION, o JwtKeyStartupGuard
# bloqueia a inicialização em produção.

EXPOSE 8080
USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT ["/opt/jboss/container/java/run/run-java.sh"]