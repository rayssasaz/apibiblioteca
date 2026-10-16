package com.apim.apibiblioteca.service;

import com.apim.apibiblioteca.dto.emprestimo.EmprestimoRequestDTO;
import com.apim.apibiblioteca.dto.emprestimo.EmprestimoResponseDTO;
import com.apim.apibiblioteca.exception.BusinessRuleException;
import com.apim.apibiblioteca.exception.ResourceNotFoundException;
import com.apim.apibiblioteca.exception.UnauthorizedOperationException;
import com.apim.apibiblioteca.model.Emprestimo;
import com.apim.apibiblioteca.model.Exemplar;
import com.apim.apibiblioteca.model.Multa;
import com.apim.apibiblioteca.model.Reserva;
import com.apim.apibiblioteca.model.Usuario;
import com.apim.apibiblioteca.model.enumerations.StatusEmprestimo;
import com.apim.apibiblioteca.model.enumerations.StatusExemplar;
import com.apim.apibiblioteca.model.enumerations.StatusMulta;
import com.apim.apibiblioteca.model.enumerations.StatusReserva;
import com.apim.apibiblioteca.repository.EmprestimoRepository;
import com.apim.apibiblioteca.repository.ExemplarRepository;
import com.apim.apibiblioteca.repository.MultaRepository;
import com.apim.apibiblioteca.repository.ReservaRepository;
import com.apim.apibiblioteca.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class EmprestimoService {

    public static final int LIMITE_EMPRESTIMOS_ATIVOS = 3;
    public static final int PRAZO_EMPRESTIMO_DIAS = 14;
    public static final int PRAZO_RENOVACAO_DIAS = 7;
    public static final int PRAZO_RETIRADA_RESERVA_DIAS = 3;
    public static final BigDecimal VALOR_MULTA_POR_DIA = new BigDecimal("1.00");

    private final EmprestimoRepository emprestimoRepository;
    private final ExemplarRepository exemplarRepository;
    private final UsuarioRepository usuarioRepository;
    private final MultaRepository multaRepository;
    private final ReservaRepository reservaRepository;

    public EmprestimoService(
            EmprestimoRepository emprestimoRepository,
            ExemplarRepository exemplarRepository,
            UsuarioRepository usuarioRepository,
            MultaRepository multaRepository,
            ReservaRepository reservaRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.exemplarRepository = exemplarRepository;
        this.usuarioRepository = usuarioRepository;
        this.multaRepository = multaRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional
    public EmprestimoResponseDTO registrarEmprestimo(EmprestimoRequestDTO request) {
        Usuario leitor = usuarioRepository.findById(request.getLeitorId())
                .orElseThrow(() -> new ResourceNotFoundException("Leitor não encontrado com ID: " + request.getLeitorId()));

        Exemplar exemplar = exemplarRepository.findById(request.getExemplarId())
                .orElseThrow(() -> new ResourceNotFoundException("Exemplar não encontrado com ID: " + request.getExemplarId()));

        if (exemplar.getStatus() != StatusExemplar.DISPONIVEL) {
            throw new BusinessRuleException("O exemplar informado não está disponível para empréstimo (status atual: " + exemplar.getStatus() + ")");
        }

        validarLeitorNaoBloqueado(leitor.getId());
        validarLimiteEmprestimosAtivos(leitor.getId());

        if (reservaRepository.existsByLivroIdAndStatus(exemplar.getLivro().getId(), StatusReserva.AGUARDANDO)) {
            throw new BusinessRuleException("Este livro possui fila de reserva aguardando atendimento; utilize o fluxo de reserva");
        }

        LocalDate hoje = LocalDate.now();
        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        exemplarRepository.save(exemplar);

        Emprestimo emprestimo = Emprestimo.builder()
                .exemplar(exemplar)
                .leitor(leitor)
                .dataEmprestimo(hoje)
                .dataPrevistaDevolucao(hoje.plusDays(PRAZO_EMPRESTIMO_DIAS))
                .renovado(false)
                .status(StatusEmprestimo.ATIVO)
                .build();

        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        return toResponseDTO(salvo);
    }

    @Transactional
    public EmprestimoResponseDTO registrarDevolucao(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado com ID: " + emprestimoId));

        if (emprestimo.getStatus() == StatusEmprestimo.DEVOLVIDO) {
            throw new BusinessRuleException("Este empréstimo já foi devolvido anteriormente");
        }

        LocalDate hoje = LocalDate.now();
        emprestimo.setDataDevolucao(hoje);
        emprestimo.setStatus(StatusEmprestimo.DEVOLVIDO);

        if (hoje.isAfter(emprestimo.getDataPrevistaDevolucao())) {
            long diasAtraso = ChronoUnit.DAYS.between(emprestimo.getDataPrevistaDevolucao(), hoje);
            BigDecimal valorMulta = VALOR_MULTA_POR_DIA
                    .multiply(BigDecimal.valueOf(diasAtraso))
                    .setScale(2, RoundingMode.HALF_UP);

            Multa multa = Multa.builder()
                    .emprestimo(emprestimo)
                    .valor(valorMulta)
                    .diasAtraso(diasAtraso)
                    .status(StatusMulta.PENDENTE)
                    .dataGeracao(hoje)
                    .build();

            Multa multaSalva = multaRepository.save(multa);
            emprestimo.setMulta(multaSalva);
        }

        Exemplar exemplar = emprestimo.getExemplar();
        Optional<Reserva> proximaReservaOpt = reservaRepository.findFirstByLivroIdAndStatusOrderByDataReservaAsc(
                exemplar.getLivro().getId(), StatusReserva.AGUARDANDO);

        if (proximaReservaOpt.isPresent()) {
            Reserva reserva = proximaReservaOpt.get();
            exemplar.setStatus(StatusExemplar.SEPARADO);
            reserva.setExemplarSeparado(exemplar);
            reserva.setStatus(StatusReserva.PRONTA_PARA_RETIRADA);
            reserva.setDataLimiteRetirada(hoje.plusDays(PRAZO_RETIRADA_RESERVA_DIAS));
            reservaRepository.save(reserva);
        } else {
            exemplar.setStatus(StatusExemplar.DISPONIVEL);
        }

        exemplarRepository.save(exemplar);
        Emprestimo atualizado = emprestimoRepository.save(emprestimo);
        return toResponseDTO(atualizado);
    }

    @Transactional
    public EmprestimoResponseDTO renovarEmprestimo(Long emprestimoId, Usuario usuarioLogado) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado com ID: " + emprestimoId));

        if (!emprestimo.getLeitor().getId().equals(usuarioLogado.getId())) {
            throw new UnauthorizedOperationException("Você não tem permissão para renovar o empréstimo de outro leitor");
        }

        if (emprestimo.getStatus() != StatusEmprestimo.ATIVO) {
            throw new BusinessRuleException("Apenas empréstimos ativos podem ser renovados");
        }

        if (emprestimo.isRenovado()) {
            throw new BusinessRuleException("Este empréstimo já foi renovado uma única vez e não permite nova renovação");
        }

        if (LocalDate.now().isAfter(emprestimo.getDataPrevistaDevolucao())) {
            throw new BusinessRuleException("Não é permitido renovar um empréstimo que está em atraso");
        }

        validarLeitorNaoBloqueado(usuarioLogado.getId());

        boolean possuiReservaAtiva = reservaRepository.existsByLivroIdAndStatusIn(
                emprestimo.getExemplar().getLivro().getId(),
                List.of(StatusReserva.AGUARDANDO, StatusReserva.PRONTA_PARA_RETIRADA));
        if (possuiReservaAtiva) {
            throw new BusinessRuleException("Não é permitido renovar o empréstimo pois existem reservas ativas para este livro");
        }

        emprestimo.setDataPrevistaDevolucao(emprestimo.getDataPrevistaDevolucao().plusDays(PRAZO_RENOVACAO_DIAS));
        emprestimo.setRenovado(true);

        Emprestimo atualizado = emprestimoRepository.save(emprestimo);
        return toResponseDTO(atualizado);
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarMeusEmprestimos(Usuario leitorLogado) {
        return emprestimoRepository.findByLeitorIdOrderByDataEmprestimoDesc(leitorLogado.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarTodosEmprestimos(
            Long leitorId,
            StatusEmprestimo status,
            Boolean atrasados) {
        List<Emprestimo> emprestimos;

        if (Boolean.TRUE.equals(atrasados)) {
            emprestimos = emprestimoRepository
                    .findByStatusAndDataPrevistaDevolucaoBeforeOrderByDataPrevistaDevolucaoAsc(
                            StatusEmprestimo.ATIVO, LocalDate.now());
        } else if (status != null) {
            emprestimos = emprestimoRepository.findByStatusOrderByDataEmprestimoDesc(status);
        } else {
            emprestimos = emprestimoRepository.findAll();
        }

        return emprestimos.stream()
                .filter(e -> leitorId == null || e.getLeitor().getId().equals(leitorId))
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> atrasados == null || (atrasados ? isEmprestimoEmAtraso(e) : !isEmprestimoEmAtraso(e)))
                .map(this::toResponseDTO)
                .toList();
    }

    public void validarLeitorNaoBloqueado(Long leitorId) {
        boolean possuiEmprestimoVencido = emprestimoRepository
                .existsByLeitorIdAndStatusAndDataPrevistaDevolucaoBefore(
                        leitorId, StatusEmprestimo.ATIVO, LocalDate.now());
        if (possuiEmprestimoVencido) {
            throw new BusinessRuleException("Leitor bloqueado: possui empréstimo vencido pendente de devolução");
        }

        boolean possuiMultaPendente = multaRepository
                .existsByEmprestimoLeitorIdAndStatus(leitorId, StatusMulta.PENDENTE);
        if (possuiMultaPendente) {
            throw new BusinessRuleException("Leitor bloqueado: possui multa pendente de pagamento");
        }
    }

    public void validarLimiteEmprestimosAtivos(Long leitorId) {
        long ativos = emprestimoRepository.countByLeitorIdAndStatus(leitorId, StatusEmprestimo.ATIVO);
        if (ativos >= LIMITE_EMPRESTIMOS_ATIVOS) {
            throw new BusinessRuleException("O leitor já atingiu o limite máximo de " + LIMITE_EMPRESTIMOS_ATIVOS + " empréstimos ativos simultâneos");
        }
    }

    public EmprestimoResponseDTO toResponseDTO(Emprestimo emprestimo) {
        boolean atrasado = isEmprestimoEmAtraso(emprestimo);
        BigDecimal valorMulta = emprestimo.getMulta() != null ? emprestimo.getMulta().getValor() : null;

        return EmprestimoResponseDTO.builder()
                .id(emprestimo.getId())
                .exemplarId(emprestimo.getExemplar().getId())
                .codigoPatrimonio(emprestimo.getExemplar().getCodigoPatrimonio())
                .livroId(emprestimo.getExemplar().getLivro().getId())
                .livroTitulo(emprestimo.getExemplar().getLivro().getTitulo())
                .leitorId(emprestimo.getLeitor().getId())
                .leitorNome(emprestimo.getLeitor().getNome())
                .leitorEmail(emprestimo.getLeitor().getEmail())
                .dataEmprestimo(emprestimo.getDataEmprestimo())
                .dataPrevistaDevolucao(emprestimo.getDataPrevistaDevolucao())
                .dataDevolucao(emprestimo.getDataDevolucao())
                .renovado(emprestimo.isRenovado())
                .status(emprestimo.getStatus())
                .atrasado(atrasado)
                .valorMulta(valorMulta)
                .build();
    }

    private boolean isEmprestimoEmAtraso(Emprestimo emprestimo) {
        return emprestimo.getStatus() == StatusEmprestimo.ATIVO
                && LocalDate.now().isAfter(emprestimo.getDataPrevistaDevolucao());
    }
}
