package com.apim.apibiblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(info = @Info(
		title = "Biblioteca do Campus API",
		version = "1.0",
		description = "Documentação da API de Gerenciamento de Acervo, Empréstimos, Reservas e Multas da Biblioteca do Campus"
))
@SpringBootApplication
public class ApibibliotecaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApibibliotecaApplication.class, args);
	}

}
