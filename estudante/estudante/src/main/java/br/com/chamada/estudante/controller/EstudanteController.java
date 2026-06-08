package br.com.chamada.estudante.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.chamada.estudante.model.EstudanteModel;
import br.com.chamada.estudante.model.PresencaModel;
import br.com.chamada.estudante.repository.PresencaRepository;
import br.com.chamada.estudante.repository.TagRepository;
import jakarta.transaction.Transactional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/estudantes")
public class EstudanteController {

    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private PresencaRepository presencaRepository;

    // Armazena o último UID biométrico capturado pelo ESP32 para novos cadastros
    private static String ultimoUidCapturado = "";

    // GET para o Front-end consultar o UID que está aguardando cadastro
    @GetMapping("/ultimo-uid")
    public ResponseEntity<Map<String, String>> obterUltimoUid() {
        return ResponseEntity.ok(Map.of("uid", ultimoUidCapturado));
    }

    // POST chamado pelo ESP32 quando uma digital não cadastrada é lida (Erro 404 no fluxo do Arduino)
    @PostMapping("/enviar-uid-temporario")
    public ResponseEntity<?> salvarUidTemporario(@RequestBody Map<String, String> body) {
        ultimoUidCapturado = body.get("uid");
        System.out.println("Digital temporária detectada no sensor biométrico: " + ultimoUidCapturado);
        return ResponseEntity.ok().build();
    }

    // Listar todos os estudantes cadastrados
    @GetMapping 
    public ResponseEntity<Iterable<EstudanteModel>> listarEstudantes() {
        return ResponseEntity.ok(this.tagRepository.findAll());
    }

    // Cadastrar um novo estudante associando o nome ao UID biométrico
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarEstudante(@RequestBody Map<String, String> body) {
        String uid = body.get("uid");
        String nome = body.get("nome");

        if (uid == null || uid.isEmpty() || nome == null || nome.isEmpty()) {
            return ResponseEntity.badRequest().body("UID ou Nome não fornecidos no corpo do JSON.");
        }

        if (tagRepository.findByUid(uid).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Digital/UID já cadastrado.");
        }

        EstudanteModel novoEstudante = new EstudanteModel();
        novoEstudante.setUid(uid);
        novoEstudante.setNome(nome);
        
        return new ResponseEntity<>(tagRepository.save(novoEstudante), HttpStatus.CREATED);
    }

    // Endpoint de Chamada/Presença acionado pelo ESP32
    @PostMapping("/chamada")
    public ResponseEntity<?> registrarPresenca(@RequestParam(required = false) String uid, @RequestBody(required = false) Map<String, String> body) {
        // Captura o UID biométrico venha ele como parâmetro na URL ou dentro do corpo JSON
        final String biometriaUid = (uid != null) ? uid : (body != null ? body.get("uid") : null);

        if (biometriaUid == null || biometriaUid.isEmpty()) {
            return ResponseEntity.badRequest().body("UID biométrico não fornecido.");
        }

        return tagRepository.findByUid(biometriaUid).map(estudante -> {
            // Define o intervalo do dia atual (00:00:00 até 23:59:59)
            LocalDateTime inicioDia = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime fimDia = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            // Verifica se o estudante já registrou presença hoje
            long presencasHoje = presencaRepository.countByEstudanteAndDataPresencaBetween(estudante, inicioDia, fimDia);

            if (presencasHoje > 0) {
                // Retorna HTTP 409 Conflict se a presença já tiver sido registrada hoje
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "erro", "nome", "Ja Registrado!"));
            }

            // Cria e salva o registro de presença
            PresencaModel presenca = new PresencaModel();
            presenca.setEstudante(estudante);
            presencaRepository.save(presenca);

            // Retorna HTTP 200 OK com o nome do aluno para o display do ESP32
            return ResponseEntity.ok(Map.of("status", "sucesso", "nome", estudante.getNome()));
            
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "erro", "nome", "Nao Encontrado")));
    }
   
    // Atualizar os dados de identificação do estudante
    @PutMapping("/atualizar")
    public ResponseEntity<?> atualizarEstudante(@RequestParam Long id, @RequestParam String uid, @RequestParam String nome) { 
        return this.tagRepository.findById(id).map(estudante -> {
            estudante.setUid(uid);
            estudante.setNome(nome);
            this.tagRepository.save(estudante);
            return ResponseEntity.ok(estudante); 
        }).orElse(ResponseEntity.notFound().build());
    }

    // Excluir um estudante do sistema
    @Transactional
    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletarEstudante(@PathVariable Long id) {
        if (this.tagRepository.existsById(id)) {
            this.tagRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}