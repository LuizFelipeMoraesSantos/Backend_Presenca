package br.com.chamada.estudante.repository;


import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.chamada.estudante.model.EstudanteModel;
import br.com.chamada.estudante.model.PresencaModel;

public interface PresencaRepository extends JpaRepository<PresencaModel, Long> {
    long countByEstudanteAndDataPresencaBetween(EstudanteModel estudante, LocalDateTime inicio, LocalDateTime fim);
}
