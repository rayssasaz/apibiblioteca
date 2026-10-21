package com.apim.apibiblioteca.controller;

import com.apim.apibiblioteca.dto.reserva.ReservaRequestDTO;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Reserva;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.model.enumerations.StatusReserva;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.LivroRepository;
import com.apim.apibiblioteca.repository.ReservaRepository;
import com.apim.apibiblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservaControllerTest extends BaseControllerTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ExemplarRepository exemplarRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Test
    @DisplayName("RF11 - Leitor deve conseguir solicitar reserva quando livro não tem exemplares disponíveis (201 Created)")
    void deveCriarReservaQuandoLivroSemExemplaresDisponiveis() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Site Reliability Engineering")
                .autor("Betsy Beyer")
                .isbn("978-1491929124")
                .categoria("TI")
                .anoPublicacao(2016)
                .build());

        // Exemplar emprestado -> zero disponiveis
        exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-RES-01")
                .status(StatusExemplar.EMPRESTADO)
                .build());

        ReservaRequestDTO request = new ReservaRequestDTO(livro.getId());

        mockMvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + getLeitorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("AGUARDANDO")));
    }

    @Test
    @DisplayName("RF11 - Deve retornar 409 Conflict ao tentar reservar livro que ainda possui exemplar disponível")
    void deveRetornarConflictAoReservarLivroComExemplarDisponivel() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("The Pragmatic Programmer")
                .autor("Andy Hunt")
                .isbn("978-0201616224")
                .categoria("TI")
                .anoPublicacao(1999)
                .build());

        // Exemplar DISPONIVEL -> nao permite reserva
        exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-RES-02")
                .status(StatusExemplar.DISPONIVEL)
                .build());

        ReservaRequestDTO request = new ReservaRequestDTO(livro.getId());

        mockMvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + getLeitorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("RF11 - Bibliotecário deve receber 403 Forbidden ao tentar criar reserva")
    void deveRetornarForbiddenAoCriarReservaComoBibliotecario() throws Exception {
        ReservaRequestDTO request = new ReservaRequestDTO(1L);

        mockMvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + getBibliotecarioToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF11 - Leitor deve conseguir consultar suas próprias reservas (200 OK)")
    void deveListarMinhasReservasComoLeitor() throws Exception {
        mockMvc.perform(get("/api/reservas/minhas")
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF13 - Leitor deve conseguir cancelar sua própria reserva ativa (200 OK)")
    void deveCancelarPropriaReservaComoLeitor() throws Exception {
        Usuario leitor = usuarioRepository.findByEmail("leitor@biblioteca.edu.br").orElseThrow();

        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Patterns of Enterprise Application Architecture")
                .autor("Martin Fowler")
                .isbn("978-0321127426")
                .categoria("TI")
                .anoPublicacao(2002)
                .build());

        Reserva reserva = reservaRepository.save(Reserva.builder()
                .leitor(leitor)
                .livro(livro)
                .dataReserva(LocalDateTime.now())
                .status(StatusReserva.AGUARDANDO)
                .build());

        mockMvc.perform(patch("/api/reservas/{id}/cancelar", reserva.getId())
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELADA")));
    }

    @Test
    @DisplayName("RF12 - Bibliotecário deve conseguir registrar retirada de exemplar separado gerando empréstimo (200 OK)")
    void deveRetirarReservaQuandoBibliotecario() throws Exception {
        Usuario leitor = usuarioRepository.findByEmail("leitor@biblioteca.edu.br").orElseThrow();

        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Kubernetes in Action")
                .autor("Marko Luksa")
                .isbn("978-1617293726")
                .categoria("TI")
                .anoPublicacao(2017)
                .build());

        Exemplar exemplar = exemplarRepository.save(Exemplar.builder()
                .livro(livro)
                .codigoPatrimonio("PAT-RES-03")
                .status(StatusExemplar.SEPARADO)
                .build());

        Reserva reserva = reservaRepository.save(Reserva.builder()
                .leitor(leitor)
                .livro(livro)
                .exemplarSeparado(exemplar)
                .dataReserva(LocalDateTime.now().minusDays(1))
                .dataLimiteRetirada(LocalDate.now().plusDays(3))
                .status(StatusReserva.PRONTA_PARA_RETIRADA)
                .build());

        mockMvc.perform(post("/api/reservas/{id}/retirar", reserva.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("ATIVO")));
    }

    @Test
    @DisplayName("RF14 - Bibliotecário deve conseguir listar a fila de reservas de um livro")
    void deveListarFilaPorLivroComoBibliotecario() throws Exception {
        Livro livro = livroRepository.save(Livro.builder()
                .titulo("Database Internals")
                .autor("Alex Petrov")
                .isbn("978-1492040347")
                .categoria("TI")
                .anoPublicacao(2019)
                .build());

        mockMvc.perform(get("/api/reservas/livro/{livroId}/fila", livro.getId())
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF14 - Leitor deve receber 403 Forbidden ao tentar listar a fila de reservas de um livro")
    void deveRetornarForbiddenAoListarFilaComoLeitor() throws Exception {
        mockMvc.perform(get("/api/reservas/livro/{livroId}/fila", 1L)
                        .header("Authorization", "Bearer " + getLeitorToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF14 - Bibliotecário deve conseguir processar reservas expiradas em lote")
    void deveProcessarReservasExpiradasComoBibliotecario() throws Exception {
        mockMvc.perform(post("/api/reservas/processar-expiradas")
                        .header("Authorization", "Bearer " + getBibliotecarioToken()))
                .andExpect(status().isOk());
    }
}
