package br.edu.catalogocursos.repository;

import br.edu.catalogocursos.domain.Matricula;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    boolean existsByAlunoIdAndCursoId(Long alunoId, Long cursoId);

    boolean existsByCursoId(Long cursoId);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from Matricula m where m.id = :id")
    java.util.Optional<Matricula> buscarComLock(
            @org.springframework.data.repository.query.Param("id") Long id);
}
