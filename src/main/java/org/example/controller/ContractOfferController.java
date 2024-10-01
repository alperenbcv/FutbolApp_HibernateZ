package org.example.controller;

import org.example.entities.ContractOffer;
import org.example.service.ContractOfferService;

import java.util.List;
import java.util.Optional;

public class ContractOfferController {

    private final ContractOfferService contractOfferService;

    public ContractOfferController() {
        this.contractOfferService = ContractOfferService.getInstance();
    }

    public ContractOffer save(ContractOffer contractOffer) {
        return contractOfferService.save(contractOffer);
    }
    
    public ContractOffer update(ContractOffer contractOffer) {
        return contractOfferService.update(contractOffer);
    }
    
    public List<ContractOffer> findAcceptedByTeamId(Integer teamId) {
        return contractOfferService.findAcceptedByTeamId(teamId);
    }
    
    public Optional<ContractOffer> findById(int id) {
        return contractOfferService.findById(id);
    }
}