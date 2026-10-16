package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.emprestimo.EmprestimoResponseDTO;
import com.apim.apibiblioteca.dto.reserva.ReservaRequestDTO;
import com.apim.apibiblioteca.dto.reserva.ReservaResponseDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.exception.ResourceNotFoundException;
import com.apim.apibiblioteca.exception.UnauthorizedOperationException;
import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Livro;
import com.apim.apibiblioteca.model.Reserva;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.Role;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.model.enumerations.StatusReserva;
import com.apim.apibiblioteca.repository.EmprestimoRepository;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.LivroRepository;
import com.apim.apibiblioteca.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final EmprestimoService emprestimoService;

    public ReservaService(
            ReservaRepository reservaRepository,
            LivroRepository livroRepository,
            ExemplarRepository exemplarRepository,
            EmprestimoRepository emprestimoRepository,
            EmprestimoService emprestimoService) {
        this.reservaRepository = reservaRepository;
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.emprestimoService = emprestimoService;
    }

    @Transactional
    public ReservaResponseDTO criarReserva(ReservaRequestDTO request, Usuario leitorLogado) {
        Livro livro = livroRepository.findById(request.getLivroId())
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + request.getLivroId()));

        long exemplaresCirculaveis = exemplarRepository.countByLivroIdAndStatusNot(
                livro.getId(), StatusExemplar.INDISPONIVEL);
        if (exemplaresCirculaveis == 0) {
            throw new BusinessRuleException("Este livro não possui exemplares válidos para circulação no acervo");
        }

        long exemplaresDisponiveis = exemplarRepository.countByLivroIdAndStatus(
                livro.getId(), StatusExemplar.DISPONIVEL);
        if (exemplaresDisponiveis > 0) {
            throw new BusinessRuleException("Não é permitido reservar um livro que possui exemplares disponíveis para empréstimo imediato");
        }

        emprestimoService.validarLeitorNaoBloqueado(leitorLogado.getId());

        boolean jaPossuiReservaAtiva = reservaRepository.existsByLeitorIdAndLivroIdAndStatusIn(
                leitorLogado.getId(),
                livro.getId(),
                List.of(StatusReserva.AGUARDANDO, StatusReserva.PRONTA_PARA_RETIRADA));
        if (jaPossuiReservaAtiva) {
            throw new BusinessRuleException("O leitor já possui uma reserva ativa para este livro");
        }

        boolean jaPossuiEmprestimoAtivoDoLivro = emprestimoRepository.existsByLeitorIdAndExemplarLivroIdAndStatus(
                leitorLogado.getId(),
                livro.getId(),
                StatusEmprestimo.ATIVO);
        if (jaPossuiEmprestimoAtivoDoLivro) {
            throw new BusinessRuleException("O leitor já está com um exemplar deste livro emprestado no momento");
        }

        Reserva reserva = Reserva.builder()
                .livro(livro)
                .leitor(leitorLogado)
                .dataReserva(LocalDateTime.now())
                .status(StatusReserva.AGUARDANDO)
                .build();

        Reserva salva = reservaRepository.save(reserva);
        Integer posicao = calcularPosicaoNaFila(salva);
        return toResponseDTO(salva, posicao);
    }

    @Transactional
    public EmprestimoResponseDTO retirarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com ID: " + reservaId));

        if (reserva.getStatus() != StatusReserva.PRONTA_PARA_RETIRADA) {
            throw new BusinessRuleException("Apenas reservas com status PRONTA_PARA_RETIRADA podem ser retiradas (status atual: " + reserva.getStatus() + ")");
        }

        LocalDate hoje = LocalDate.now();
        if (reserva.getDataLimiteRetirada() != null && hoje.isAfter(reserva.getDataLimiteRetirada())) {
            throw new BusinessRuleException("O prazo de retirada desta reserva já expirou em " + reserva.getDataLimiteRetirada());
        }

        Usuario leitor = reserva.getLeitor();
        emprestimoService.validarLeitorNaoBloqueado(leitor.getId());
        emprestimoService.validarLimiteEmprestimosAtivos(leitor.getId());

        Exemplar exemplar = reserva.getExemplarSeparado();
        if (exemplar == null) {
            throw new BusinessRuleException("Não há exemplar separado vinculado a esta reserva");
        }

        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        exemplarRepository.save(exemplar);

        reserva.setStatus(StatusReserva.ATENDIDA);
        reservaRepository.save(reserva);

        Emprestimo emprestimo = Emprestimo.builder()
                .exemplar(exemplar)
                .leitor(leitor)
                .dataEmprestimo(hoje)
                .dataPrevistaDevolucao(hoje.plusDays(EmprestimoService.PRAZO_EMPRESTIMO_DIAS))
                .renovado(false)
                .status(StatusEmprestimo.ATIVO)
                .build();

        Emprestimo emprestimoSalvo = emprestimoRepository.save(emprestimo);
        return emprestimoService.toResponseDTO(emprestimoSalvo);
    }

    @Transactional
    public ReservaResponseDTO cancelarReserva(Long reservaId, Usuario usuarioLogado) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com ID: " + reservaId));

        if (usuarioLogado.getRole() == Role.LEITOR && !reserva.getLeitor().getId().equals(usuarioLogado.getId())) {
            throw new UnauthorizedOperationException("Você não tem permissão para cancelar a reserva de outro leitor");
        }

        if (reserva.getStatus() != StatusReserva.AGUARDANDO
                && reserva.getStatus() != StatusReserva.PRONTA_PARA_RETIRADA) {
            throw new BusinessRuleException("Apenas reservas ativas (AGUARDANDO ou PRONTA_PARA_RETIRADA) podem ser canceladas");
        }

        if (reserva.getStatus() == StatusReserva.PRONTA_PARA_RETIRADA && reserva.getExemplarSeparado() != null) {
            Exemplar exemplar = reserva.getExemplarSeparado();
            repassarExemplarParaProximoDaFilaOuDisponibilizar(exemplar, reserva.getLivro().getId());
            reserva.setExemplarSeparado(null);
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        Reserva cancelada = reservaRepository.save(reserva);
        return toResponseDTO(cancelada, null);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listarFilaPorLivro(Long livroId) {
        if (!livroRepository.existsById(livroId)) {
            throw new ResourceNotFoundException("Livro não encontrado com ID: " + livroId);
        }

        List<Reserva> fila = reservaRepository.findByLivroIdAndStatusInOrderByDataReservaAsc(
                livroId,
                List.of(StatusReserva.PRONTA_PARA_RETIRADA, StatusReserva.AGUARDANDO));

        List<ReservaResponseDTO> resposta = new ArrayList<>(fila.size());
        for (int i = 0; i < fila.size(); i++) {
            resposta.add(toResponseDTO(fila.get(i), i + 1));
        }
        return resposta;
    }

    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listarMinhasReservas(Usuario leitorLogado) {
        List<Reserva> reservas = reservaRepository.findByLeitorIdOrderByDataReservaDesc(leitorLogado.getId());
        return reservas.stream()
                .map(r -> toResponseDTO(r, calcularPosicaoNaFila(r)))
                .toList();
    }

    @Transactional
    public List<ReservaResponseDTO> processarReservasExpiradas() {
        LocalDate hoje = LocalDate.now();
        List<Reserva> vencidas = reservaRepository.findByStatusAndDataLimiteRetiradaBeforeOrderByDataReservaAsc(
                StatusReserva.PRONTA_PARA_RETIRADA, hoje);

        List<ReservaResponseDTO> processadas = new ArrayList<>(vencidas.size());
        for (Reserva reserva : vencidas) {
            if (reserva.getExemplarSeparado() != null) {
                Exemplar exemplar = reserva.getExemplarSeparado();
                repassarExemplarParaProximoDaFilaOuDisponibilizar(exemplar, reserva.getLivro().getId());
                reserva.setExemplarSeparado(null);
            }
            reserva.setStatus(StatusReserva.EXPIRADA);
            Reserva salva = reservaRepository.save(reserva);
            processadas.add(toResponseDTO(salva, null));
        }

        return processadas;
    }

    private void repassarExemplarParaProximoDaFilaOuDisponibilizar(Exemplar exemplar, Long livroId) {
        Optional<Reserva> proximaOpt = reservaRepository.findFirstByLivroIdAndStatusOrderByDataReservaAsc(
                livroId, StatusReserva.AGUARDANDO);

        if (proximaOpt.isPresent()) {
            Reserva proxima = proximaOpt.get();
            exemplar.setStatus(StatusExemplar.SEPARADO);
            proxima.setExemplarSeparado(exemplar);
            proxima.setStatus(StatusReserva.PRONTA_PARA_RETIRADA);
            proxima.setDataLimiteRetirada(LocalDate.now().plusDays(EmprestimoService.PRAZO_RETIRADA_RESERVA_DIAS));
            reservaRepository.save(proxima);
        } else {
            exemplar.setStatus(StatusExemplar.DISPONIVEL);
        }

        exemplarRepository.save(exemplar);
    }

    private Integer calcularPosicaoNaFila(Reserva reserva) {
        if (reserva.getStatus() != StatusReserva.AGUARDANDO
                && reserva.getStatus() != StatusReserva.PRONTA_PARA_RETIRADA) {
            return null;
        }
        List<Reserva> fila = reservaRepository.findByLivroIdAndStatusInOrderByDataReservaAsc(
                reserva.getLivro().getId(),
                List.of(StatusReserva.PRONTA_PARA_RETIRADA, StatusReserva.AGUARDANDO));
        for (int i = 0; i < fila.size(); i++) {
            if (fila.get(i).getId().equals(reserva.getId())) {
                return i + 1;
            }
        }
        return null;
    }

    private ReservaResponseDTO toResponseDTO(Reserva reserva, Integer posicaoFila) {
        Long exemplarSeparadoId = reserva.getExemplarSeparado() != null ? reserva.getExemplarSeparado().getId() : null;
        String codigoPatrimonio = reserva.getExemplarSeparado() != null
                ? reserva.getExemplarSeparado().getCodigoPatrimonio()
                : null;

        return ReservaResponseDTO.builder()
                .id(reserva.getId())
                .livroId(reserva.getLivro().getId())
                .livroTitulo(reserva.getLivro().getTitulo())
                .leitorId(reserva.getLeitor().getId())
                .leitorNome(reserva.getLeitor().getNome())
                .leitorEmail(reserva.getLeitor().getEmail())
                .exemplarSeparadoId(exemplarSeparadoId)
                .codigoPatrimonioSeparado(codigoPatrimonio)
                .dataReserva(reserva.getDataReserva())
                .dataLimiteRetirada(reserva.getDataLimiteRetirada())
                .status(reserva.getStatus())
                .posicaoFila(posicaoFila)
                .build();
    }
}
