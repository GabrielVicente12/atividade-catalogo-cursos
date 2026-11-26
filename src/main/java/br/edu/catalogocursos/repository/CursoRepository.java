package br.edu.catalogocursos.repository;

import br.edu.catalogocursos.domain.Curso;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepository
        extends JpaRepository<Curso, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Curso> {
    List<Curso> findByInstrutorIdOrderByTituloAsc(Long instrutorId);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Curso c where c.id = :id")
    java.util.Optional<Curso> buscarComLock(
            @org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByInstrutorId(Long id);
}
