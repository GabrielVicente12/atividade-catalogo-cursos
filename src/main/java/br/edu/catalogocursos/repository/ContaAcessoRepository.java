package br.edu.catalogocursos.repository;

import br.edu.catalogocursos.domain.ContaAcesso;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContaAcessoRepository extends JpaRepository<ContaAcesso, Long> {
    Optional<ContaAcesso> findByLogin(String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContaAcesso c where c.id=:id")
    Optional<ContaAcesso> buscarComLock(@Param("id") Long id);
}
