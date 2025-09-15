package com.cnpjfinder.client;

import com.cnpjfinder.exception.CnpjApiException;
import com.cnpjfinder.exception.CnpjNotFoundException;
import com.cnpjfinder.model.Empresa;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class BrasilApiClient {

    private static final String API_NAME = "BrasilAPI";
    private static final String BRASIL_API_URL = "https://brasilapi.com.br/api/cnpj/v1/{cnpj}";

    private final RestTemplate restTemplate;

    public BrasilApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<Empresa> consultarCnpj(String cnpj) {
        try {
            ResponseEntity<Empresa> response = restTemplate.getForEntity(
                    BRASIL_API_URL, Empresa.class, cnpj);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }
            throw new CnpjNotFoundException(cnpj);

        } catch (HttpClientErrorException.NotFound e) {
            throw new CnpjNotFoundException(cnpj);
        } catch (HttpClientErrorException e) {
            throw new CnpjApiException("Erro ao consultar o CNPJ na " + API_NAME + ". Status: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new CnpjApiException("Falha na comunicação com a " + API_NAME, e);
        } catch (Exception e) {
            throw new CnpjApiException("Ocorreu um erro inesperado ao consultar a " + API_NAME, e);
        }
    }
}
