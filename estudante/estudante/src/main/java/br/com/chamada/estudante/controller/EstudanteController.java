package br.com.chamada.estudante.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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

    private static String ultimoUidCapturado = "";

    @GetMapping("/ultimo-uid")
    public ResponseEntity<Map<String, String>> obterUltimoUid() {
        return ResponseEntity.ok(Map.of("uid", ultimoUidCapturado));
    }

    @PostMapping("/enviar-uid-temporario")
    public ResponseEntity<?> salvarUidTemporario(@RequestBody Map<String, String> body) {
        ultimoUidCapturado = body.get("uid");
        System.out.println("Digital temporária detectada no sensor biométrico: " + ultimoUidCapturado);
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

        if (tagRepository.findByUid(uid).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Digital/UID já cadastrado.");
        }

        EstudanteModel novoEstudante = new EstudanteModel();
        novoEstudante.setUid(uid);
        novoEstudante.setNome(nome);
        
        return new ResponseEntity<>(tagRepository.save(novoEstudante), HttpStatus.CREATED);
    }

    // REGISTAR CHAMADA: Procura o ID no banco de dados, gera a presença e traz o nome do aluno
    @PostMapping("/chamada")
    public ResponseEntity<?> registrarPresenca(@RequestParam String uid) {
        if (uid == null || uid.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "message", "UID biométrico não fornecido."));
        }

        // 1. Procura o ID no banco de dados (tabela tag) baseado na digital
        Optional<EstudanteModel> estudanteOptional = tagRepository.findByUid(uid);

        if (estudanteOptional.isEmpty()) {
            // Se a digital lida não corresponder a nenhum registro do sistema
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "erro", "message", "Digital não cadastrada no sistema."));
        }

        EstudanteModel estudante = estudanteOptional.get();

        // Evitar dupla contagem de presença no mesmo dia
        LocalDateTime inicioDia = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fimDia = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        long presencasHoje = presencaRepository.countByEstudanteAndDataPresencaBetween(estudante, inicioDia, fimDia);

        if (presencasHoje > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "erro", "nome", estudante.getNome(), "message", "Já registado hoje."));
        }

        // 2. Cria e computa a chamada na tabela de presenças vinculando o objeto do aluno
        PresencaModel presenca = new PresencaModel();
        presenca.setEstudante(estudante);
        presenca.setDataPresenca(LocalDateTime.now()); 
        presencaRepository.save(presenca);

        // 3. Retorna o JSON de sucesso contendo o Nome real do aluno para a interface web
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
}