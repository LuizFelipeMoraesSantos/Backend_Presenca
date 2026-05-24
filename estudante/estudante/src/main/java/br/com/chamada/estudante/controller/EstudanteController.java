package br.com.chamada.estudante.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import br.com.chamada.estudante.model.EstudanteModel;
import br.com.chamada.estudante.model.PresencaModel;
import br.com.chamada.estudante.repository.PresencaRepository;
import br.com.chamada.estudante.repository.TagRepository;
import jakarta.transaction.Transactional;


@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/estudantes")
public class EstudanteController {

    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private  PresencaRepository presencaRepository;

   
    @GetMapping 
    public ResponseEntity<Iterable<EstudanteModel>> listarEstudantes() {
        return ResponseEntity.ok(this.tagRepository.findAll());
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

    // src/main/java/br/com/chamada/estudante/controller/EstudanteController.java

@PostMapping("/chamada")
public ResponseEntity<?> registrarPresenca(@RequestParam String uid) {
    return tagRepository.findByUid(uid).map(estudante -> {
        
        
        LocalDateTime inicioDia = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fimDia = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

       
        long presencasHoje = presencaRepository.countByEstudanteAndDataPresencaBetween(estudante, inicioDia, fimDia);

        if (presencasHoje > 0) {
            // Retorna erro 409 (Conflict) se o aluno já passou a tag hoje
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("Presença já registrada para hoje: " + estudante.getNome());
        }

        
        PresencaModel presenca = new PresencaModel();
        presenca.setEstudante(estudante);
        presencaRepository.save(presenca);

        return ResponseEntity.ok("Presença registrada para: " + estudante.getNome());
        
    }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Estudante não encontrado."));
}
   
    @PutMapping("/atualizar")
    public ResponseEntity<?> atualizarEstudante(
            @RequestParam Long id, 
            @RequestParam String uid, 
            @RequestParam String nome) { 
        
        return this.tagRepository.findById(id).map(estudante -> {
            estudante.setUid(uid);
            estudante.setNome(nome);
            this.tagRepository.save(estudante);
            return ResponseEntity.ok(estudante); 
        }).orElse(ResponseEntity.notFound().build());
    }

   @PatchMapping("/atualizar-parcial/{id}")
    public ResponseEntity<?> atualizarParcialEstudante(@PathVariable Long id, @RequestBody String novoUid) {
        return this.tagRepository.findById(id).map(estudante -> {
            if (novoUid != null && !novoUid.isEmpty()) {
                estudante.setUid(novoUid);
                this.tagRepository.save(estudante);
                System.out.println("Tag RFID atualizada parcialmente: " + novoUid);
                return ResponseEntity.ok(estudante); 
            }
            return ResponseEntity.badRequest().body("Novo UID não fornecido");
        }).orElseGet(() -> {
            System.out.println("Estudante não encontrado: " + id);
            return ResponseEntity.notFound().build();
        });
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
