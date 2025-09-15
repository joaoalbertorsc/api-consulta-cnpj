package com.cnpjfinder.service;

import com.cnpjfinder.client.BrasilApiClient;
import com.cnpjfinder.client.ReceitaWsClient;
import com.cnpjfinder.exception.CnpjApiException;
import com.cnpjfinder.exception.CnpjNotFoundException;
import com.cnpjfinder.model.Empresa;
import com.cnpjfinder.model.Estatisticas;
import com.cnpjfinder.repository.EmpresaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CnpjService {

    private final BrasilApiClient brasilApiClient;
    private final ReceitaWsClient receitaWsClient;
    private final EmpresaRepository empresaRepository;

    public CnpjService(BrasilApiClient brasilApiClient,
                       ReceitaWsClient receitaWsClient,
                       EmpresaRepository empresaRepository) {
        this.brasilApiClient = brasilApiClient;
        this.receitaWsClient = receitaWsClient;
        this.empresaRepository = empresaRepository;
    }

    @Cacheable(value = "cnpjCache", key = "#cnpj")
    public Empresa consultarCnpj(String cnpj) {
        Optional<Empresa> empresaOptional = empresaRepository.findById(cnpj);
        if (empresaOptional.isPresent()) {
            Empresa empresa = empresaOptional.get();
            empresa.setQuantidadeConsultas(empresa.getQuantidadeConsultas() + 1);
            empresa.setDataUltimaConsulta(new Date());
            return empresaRepository.save(empresa);
        }

        Empresa empresaExterna = buscarNasApisExternas(cnpj);
        return salvarNovaEmpresa(empresaExterna);
    }

    private Empresa buscarNasApisExternas(String cnpj) {
        try {
            return brasilApiClient.consultarCnpj(cnpj)
                    .orElseThrow(() -> new CnpjNotFoundException(cnpj));
        } catch (CnpjNotFoundException e) {
            throw e;
        } catch (CnpjApiException e) {
            return receitaWsClient.consultarCnpj(cnpj)
                    .orElseThrow(() -> new CnpjNotFoundException(cnpj));
        }
    }

    private Empresa salvarNovaEmpresa(Empresa empresa) {
        empresa.setQuantidadeConsultas(1);
        empresa.setDataUltimaConsulta(new Date());
        return empresaRepository.save(empresa);
    }

    public List<Empresa> buscarPorRazaoSocial(String razaoSocial) {
        return empresaRepository.findByRazaoSocialContainingIgnoreCase(razaoSocial);
    }

    public List<Empresa> buscarPorMunicipio(String municipio) {
        return empresaRepository.findByMunicipioContainingIgnoreCase(municipio);
    }

    public List<Empresa> buscarPorUf(String uf) {
        return empresaRepository.findByUfContainingIgnoreCase(uf);
    }

    public Estatisticas getEstatisticas() {
        Long totalEmpresas = empresaRepository.count();
        Long totalConsultas = empresaRepository.sumQuantidadeConsultas();
        List<Empresa> empresasMaisConsultadas = empresaRepository.findTop10ByOrderByQuantidadeConsultasDesc();

        return new Estatisticas(totalEmpresas, totalConsultas, empresasMaisConsultadas);
    }
}
