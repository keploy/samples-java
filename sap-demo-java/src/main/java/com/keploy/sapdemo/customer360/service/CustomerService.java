package com.keploy.sapdemo.customer360.service;

import com.keploy.sapdemo.customer360.model.BusinessPartner;
import com.keploy.sapdemo.customer360.model.CustomerSummary;
import com.keploy.sapdemo.customer360.sap.SapBusinessPartnerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business-level facade for simple (single-endpoint) customer lookups.
 *
 * <p>The 360 aggregator lives in its own service because it has very
 * different operational characteristics (parallelism, fan-out latency,
 * partial-failure policy).
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final SapBusinessPartnerClient sapClient;

    public CustomerService(SapBusinessPartnerClient sapClient) {
        this.sapClient = sapClient;
    }

    public BusinessPartner getById(String id) {
        log.debug("getById id={}", id);
        return sapClient.fetchPartner(id);
    }

    public List<CustomerSummary> listCustomers(int top, int skip) {
        log.debug("listCustomers top={} skip={}", top, skip);
        return sapClient.listPartners(top, skip).stream()
            .map(CustomerSummary::from)
            .toList();
    }

    public long totalCount() {
        log.debug("totalCount");
        return sapClient.fetchTotalCount();
    }
}
