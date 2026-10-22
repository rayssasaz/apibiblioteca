# ==============================================================================
# Estágio 1: Build da aplicação com Maven e JDK 17
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copia os arquivos de configuração do Maven primeiro para aproveitar o cache de camadas
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copia o código-fonte e gera o pacote .jar (testes já validados no pipeline)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# Estágio 2: Imagem enxuta de execução com JRE 17
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia apenas o artefato .jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
