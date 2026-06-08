package br.com.chamada.estudante.repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.chamada.estudante.model.EstudanteModel;
import br.com.chamada.estudante.model.PresencaModel;

@Repository
public interface PresencaRepository extends JpaRepository<PresencaModel, Long> {

    // Força o Hibernate/Spring a executar a consulta JPQL exata sem erros de sintaxe
    @Query("SELECT COUNT(p) FROM PresencaModel p WHERE p.estudante = :estudante AND p.dataPresenca BETWEEN :inicio AND :fim")
    long countByEstudanteAndDataPresencaBetween(
        @Param("estudante") EstudanteModel estudante, 
        @Param("inicio") LocalDateTime inicio, 
        @Param("fim") LocalDateTime fim
    );
}