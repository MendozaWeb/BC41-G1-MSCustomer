package com.nttdata.bcp1.mscustomer.service;

import com.nttdata.bcp1.mscustomer.model.Credit;
import com.nttdata.bcp1.mscustomer.model.Customer;
import com.nttdata.bcp1.mscustomer.proxy.CustomerProxy;
import com.nttdata.bcp1.mscustomer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private static final Logger logger = LogManager.getLogger(CustomerServiceImpl.class);
    @Autowired
    CustomerRepository customerRepository;

    private CustomerProxy customerProxy = new CustomerProxy();

    @Override
    public Flux<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public Mono<Customer> findById(String id) {
        return customerRepository.findById(id);
    }

    @Override
    public Mono<Customer> save(Customer customer) {
        return checkType(customer)
                .flatMap(customerRepository::save);
    }

    @Override
    public void delete(String id) {
        customerRepository.deleteById(id).subscribe();
    }

    //CLIENT UTIL METHODS
    public Mono<Customer> checkType(Customer customer){
        List<String> types = new ArrayList<>();
        types.add("PERSONAL");
        types.add("BUSINESS");

        return types.contains(customer.getCustomerType()) ? Mono.just(customer)
                : Mono.error(() -> new IllegalArgumentException("Invalid Client type"));

    }


    public Mono<Customer> checkIfCreditCard(Customer customer){
        return customerProxy.getCredits(customer.getId())
                .filter(resp->resp.getTypeCredit().contains("CREDIT CARD"))
                .next()
                .switchIfEmpty(Mono.just(new Credit()))
                .flatMap(resp->{
                    logger.info(String.format("Tipo de cerdito: "+resp.getTypeCredit()));
                    if(resp.getId()!=null && resp.getTypeCredit().contains("CREDIT CARD")) return Mono.just(customer);
                    return Mono.error(() -> new IllegalArgumentException("Invalid Client profile, client doesn't have a credit card"));
                });
    }
}