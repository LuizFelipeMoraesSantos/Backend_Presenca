package br.com.chamada.estudante.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.chamada.estudante.model.EstudanteModel;
import br.com.chamada.estudante.model.PresencaModel;
import br.com.chamada.estudante.repository.PresencaRepository;
import br.com.chamada.estudante.repository.TagRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RestController
public class EstudanteController {

    private final TagRepository tagRepository;
    private final PresencaRepository presencaRepository;

    @GetMapping("/Estudantes")
    public Iterable<EstudanteModel> listarEstudantes() {
        return new ResponseEntity<>(this.tagRepository.findAll(), HttpStatus.OK).getBody();
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrarEstudante(@RequestParam String uid, @RequestParam String nome) {
        if (tagRepository.findByUid(uid).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Tag já cadastrada");
        }

        EstudanteModel novoEstudante = new EstudanteModel();
        novoEstudante.setUid(uid);
        novoEstudante.setNome(nome); // Agora salva o nome
        
        return new ResponseEntity<>(tagRepository.save(novoEstudante), HttpStatus.CREATED);
    }

    @PostMapping("/chamada")
    public ResponseEntity<?> registrarPresenca(@RequestParam String uid) {
        // 1. Procura se o UID existe no banco
        return tagRepository.findByUid(uid).map(estudante -> {
            // 2. Se existe, cria um registro na tabela de presenças
            PresencaModel presenca = new PresencaModel();
            presenca.setEstudante(estudante);
            presencaRepository.save(presenca);

            System.out.println("Presença confirmada: " + estudante.getNome() + " em " + LocalDateTime.now());
            return ResponseEntity.ok("Presença registrada para: " + estudante.getNome());
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Estudante não encontrado para esta tag."));
    }

    @PutMapping("/atualizar")
    public ResponseEntity<?> atualizarEstudante(Long id, String novoUid) {
        return this.tagRepository.findById(id).map(estudante -> {
            estudante.setUid(novoUid);
            this.tagRepository.save(estudante);
            System.out.println("Tag RFID atualizada com sucesso: " + novoUid);
            return ResponseEntity.ok("Atualizado com sucesso"); // Caso sucesso
        }).orElse(ResponseEntity.notFound().build()); // Caso não encontre (404)
    }

   @PatchMapping("/atualizar-parcial/{id}")
    public ResponseEntity<?> atualizarParcialEstudante(@PathVariable Long id, @RequestBody String novoUid) {
        return this.tagRepository.findById(id).map(estudante -> {
            if (novoUid != null && !novoUid.isEmpty()) {
                estudante.setUid(novoUid);
                this.tagRepository.save(estudante);
                System.out.println("Tag RFID atualizada parcialmente: " + novoUid);
                return ResponseEntity.ok(estudante); // Retorna o objeto atualizado
            }
            return ResponseEntity.badRequest().body("Novo UID não fornecido");
        }).orElseGet(() -> {
            System.out.println("Estudante não encontrado: " + id);
            return ResponseEntity.notFound().build();
        });
}

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletarEstudante(@PathVariable Long id) {
        if (this.tagRepository.existsById(id)) {
            this.tagRepository.deleteById(id);
            System.out.println("Estudante deletado com sucesso: " + id);
            return ResponseEntity.noContent().build(); // Status 204 (Sucesso sem corpo)
        } else {
            System.out.println("Estudante não encontrado com ID: " + id);
            return ResponseEntity.notFound().build(); // Status 404
        }
}
    
}
