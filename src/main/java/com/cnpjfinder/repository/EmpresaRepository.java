package com.cnpjfinder.repository;

import com.cnpjfinder.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, String> {
    List<Empresa> findByRazaoSocialContainingIgnoreCase(String razaoSocial);
    List<Empresa> findByMunicipioContainingIgnoreCase(String municipio);
    List<Empresa> findByUfContainingIgnoreCase(String uf);

    @Query("SELECT SUM(e.quantidadeConsultas) FROM Empresa e")
    Long sumQuantidadeConsultas();

    List<Empresa> findTop10ByOrderByQuantidadeConsultasDesc();
}
