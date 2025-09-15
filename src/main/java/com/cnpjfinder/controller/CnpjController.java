package com.cnpjfinder.controller;

import com.cnpjfinder.exception.InvalidCnpjException;
import com.cnpjfinder.exception.InvalidSearchParameterException;
import com.cnpjfinder.model.Empresa;
import com.cnpjfinder.model.Estatisticas;
import com.cnpjfinder.service.CnpjService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cnpj")
public class CnpjController {

    private final CnpjService cnpjService;

    public CnpjController(CnpjService cnpjService) {
        this.cnpjService = cnpjService;
    }

    @GetMapping("/{cnpj}")
    public ResponseEntity<Empresa> consultarCnpj(@PathVariable String cnpj) {
        if (!isFormatoCnpjValido(cnpj)) {
            throw new InvalidCnpjException("O formato do CNPJ fornecido ('" + cnpj + "') é inválido. Por favor, forneça exatamente 14 dígitos numéricos.");
        }
        if (!isAlgoritmoCnpjValido(cnpj)) {
            throw new InvalidCnpjException("O CNPJ fornecido ('" + cnpj + "') é inválido (dígitos verificadores não correspondem).");
        }

        Empresa empresa = cnpjService.consultarCnpj(cnpj);
        return ResponseEntity.ok(empresa);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Empresa>> buscarEmpresas(
            @RequestParam(required = false) String razaoSocial,
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String uf) {

        if (razaoSocial != null) {
            return ResponseEntity.ok(cnpjService.buscarPorRazaoSocial(razaoSocial));
        }

        if (municipio != null) {
            return ResponseEntity.ok(cnpjService.buscarPorMunicipio(municipio));
        }

        if (uf != null) {
            if (uf.length() != 2) {
                throw new InvalidSearchParameterException("O parâmetro 'uf' deve conter exatamente 2 caracteres.");
            }
            return ResponseEntity.ok(cnpjService.buscarPorUf(uf));
        }

        throw new InvalidSearchParameterException("É necessário fornecer ao menos um parâmetro de busca (razaoSocial, municipio ou uf).");
    }

    @GetMapping("/estatisticas")
    public ResponseEntity<Estatisticas> getEstatisticas() {
        return ResponseEntity.ok(cnpjService.getEstatisticas());
    }

    private boolean isFormatoCnpjValido(String cnpj) {
        return cnpj != null && cnpj.matches("^\\d{14}$");
    }

    private boolean isAlgoritmoCnpjValido(String cnpj) {
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        try {
            int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int soma = 0;
            for (int i = 0; i < 12; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * pesos1[i];
            }
            int digito1 = (soma % 11) < 2 ? 0 : 11 - (soma % 11);

            int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            soma = 0;
            for (int i = 0; i < 13; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * pesos2[i];
            }
            int digito2 = (soma % 11) < 2 ? 0 : 11 - (soma % 11);

            return Character.getNumericValue(cnpj.charAt(12)) == digito1 &&
                   Character.getNumericValue(cnpj.charAt(13)) == digito2;
        } catch (Exception e) {
            return false;
        }
    }
}
