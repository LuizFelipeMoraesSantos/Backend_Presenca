package br.com.chamada.estudante.controller;

import br.com.chamada.estudante.model.EstudanteModel;
import br.com.chamada.estudante.model.PresencaModel;
import br.com.chamada.estudante.repository.PresencaRepository;
import br.com.chamada.estudante.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/estudantes")
public class EstudanteController {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PresencaRepository presencaRepository;

    private static String ultimoUidCapturado = "";
    
    // Lista segura para gerenciar as abas web abertas no Frontend Next.js
    private static final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter conectarFrontend() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 minutos de timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        
        return emitter;
    }

    @GetMapping("/ultimo-uid")
    public ResponseEntity<Map<String, String>> obterUltimoUid() {
        return ResponseEntity.ok(Map.of("uid", ultimoUidCapturado));
    }

    @PostMapping("/enviar-uid-temporario")
    public ResponseEntity<?> salvarUidTemporario(@RequestBody Map<String, String> body) {
        ultimoUidCapturado = body.get("uid");
        System.out.println("Digital temporária detectada no sensor biométrico: " + ultimoUidCapturado);
        
        // Empurra o Frontend Next.js diretamente para a página de Cadastro
        notificarFrontend("CADASTRO", ultimoUidCapturado);
        return ResponseEntity.ok().build();
    }

    @GetMapping 
    public ResponseEntity<Iterable<EstudanteModel>> listarEstudantes() {
        return ResponseEntity.ok(this.tagRepository.findAll());
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarEstudante(@RequestParam String uid, @RequestParam String nome) {
        if (uid == null || uid.isEmpty() || nome == null || nome.isEmpty()) {
            return ResponseEntity.badRequest().body("UID ou Nome não fornecidos.");
        }

        // Verifica por similaridade de caracteres se essa biometria já foi adicionada
        if (buscarEstudantePorBiometriaAproximada(uid).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Esta digital já está associada a um aluno cadastrado.");
        }

        EstudanteModel novoEstudante = new EstudanteModel();
        novoEstudante.setUid(uid);
        novoEstudante.setNome(nome);
        
        return new ResponseEntity<>(tagRepository.save(novoEstudante), HttpStatus.CREATED);
    }

    // =========================================================================
    // REGISTRAR CHAMADA: Processa a leitura enviada do hardware ESP32
    // =========================================================================
    @PostMapping("/chamada")
    public ResponseEntity<?> registrarPresenca(@RequestParam String uid) {
        if (uid == null || uid.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "message", "UID biométrico não fornecido."));
        }

        // Faz a varredura e comparação aproximada da digital no banco de dados
        Optional<EstudanteModel> estudanteOptional = buscarEstudantePorBiometriaAproximada(uid);

        // 1. Caso a digital NÃO exista no banco de dados
        if (estudanteOptional.isEmpty()) {
            ultimoUidCapturado = uid;
            
            // Força o Frontend Next.js a abrir instantaneamente a tela de Cadastro preenchendo o UID
            notificarFrontend("CADASTRO", uid);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "cadastro_pendente", "uid", uid, "message", "Digital não cadastrada."));
        }

        EstudanteModel estudante = estudanteOptional.get();

        // 2. Filtro do dia atual utilizando LocalDateTime (00:00:00 até 23:59:59)
        LocalDateTime inicioDia = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fimDia = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        
        long presencasHoje = presencaRepository.countByEstudanteAndDataPresencaBetween(estudante, inicioDia, fimDia);

        if (presencasHoje > 0) {
            // Se já tem ponto hoje, mantém o front atualizado na rota de presença
            notificarFrontend("PRESENCA", estudante.getUid());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "erro", "nome", estudante.getNome(), "message", "Já registrado hoje."));
        }

        // 3. Sucesso: Cria e salva a nova folha de presença atrelando o objeto Aluno completo
        PresencaModel presenca = new PresencaModel();
        presenca.setEstudante(estudante);
        presenca.setDataPresenca(LocalDateTime.now()); 
        presencaRepository.save(presenca);

        // Dispara o comando via SSE para o Next.js pular para a página de chamada com sucesso
        notificarFrontend("PRESENCA", estudante.getUid());

        return ResponseEntity.ok(Map.of(
            "status", "sucesso", 
            "nome", estudante.getNome(),
            "uid", estudante.getUid()
        ));
    }
   
    @PutMapping("/atualizar")
    public ResponseEntity<?> atualizarEstudante(@RequestParam Long id, @RequestParam String uid, @RequestParam String nome) { 
        return this.tagRepository.findById(id).map(estudante -> {
            estudante.setUid(uid);
            estudante.setNome(nome);
            this.tagRepository.save(estudante);
            return ResponseEntity.ok(estudante); 
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletarEstudante(@PathVariable Long id) {
        if (this.tagRepository.existsById(id)) {
            this.tagRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // =========================================================================
    // LÓGICA DE SIMILARIDADE BIOMÉTRICA (Algoritmo Levenshtein Distance)
    // =========================================================================
    private Optional<EstudanteModel> buscarEstudantePorBiometriaAproximada(String uidAlvo) {
        Iterable<EstudanteModel> todosAlunos = tagRepository.findAll();
        
        for (EstudanteModel aluno : todosAlunos) {
            String uidCadastrado = aluno.getUid();
            if (uidCadastrado == null) continue;
            
            // Mede a compatibilidade entre o código lido pelo sensor e o gravado na tabela
            double similaridade = calcularPorcentagemSimilaridade(uidAlvo, uidCadastrado);
            
            // Se as strings apresentarem mais de 85% de correspondência, valida como o mesmo usuário
            if (similaridade >= 0.85) {
                return Optional.of(aluno);
            }
        }
        return Optional.empty();
    }

    private double calcularPorcentagemSimilaridade(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        if (m == 0 && n == 0) return 1.0;
        if (m == 0 || n == 0) return 0.0;
        
        int[][] d = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) d[i][0] = i;
        for (int j = 0; j <= n; j++) d[0][j] = j;
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return 1.0 - ((double) d[m][n] / Math.max(s1.length(), s2.length()));
    }

    // Método que transmite os gatilhos assíncronos de troca de tela para o Frontend
    private void notificarFrontend(String acao, String uid) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("fluxo-biometria")
                    .data(Map.of("action", acao, "uid", uid)));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
