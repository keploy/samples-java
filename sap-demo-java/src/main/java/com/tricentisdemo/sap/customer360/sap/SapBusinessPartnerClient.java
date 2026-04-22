package com.tricentisdemo.sap.customer360.sap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricentisdemo.sap.customer360.model.BusinessPartner;
import com.tricentisdemo.sap.customer360.model.BusinessPartnerAddress;
import com.tricentisdemo.sap.customer360.model.BusinessPartnerRole;
import com.tricentisdemo.sap.customer360.model.ODataCollectionResponse;
import com.tricentisdemo.sap.customer360.model.ODataEntityResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Low-level gateway to SAP's {@code API_BUSINESS_PARTNER} OData service.
 *
 * <p>All outbound SAP calls go through here. Each method:
 * <ul>
 *   <li>Has a Resilience4j {@link Retry @Retry} and
 *       {@link CircuitBreaker @CircuitBreaker} annotation so transient
 *       5xx / timeout failures don't blow up the caller</li>
 *   <li>Translates HTTP layer exceptions into domain-level
 *       {@link SapApiException}</li>
 *   <li>Logs every call at INFO with method + path + correlation id,
 *       including final status for traceability</li>
 * </ul>
 *
 * <p>The service path is fixed; only the sub-path varies. Base URL is set
 * on the RestTemplate's rootUri (see
 * {@link com.tricentisdemo.sap.customer360.config.SapClientConfig}).
 */
@Component
public class SapBusinessPartnerClient {

    private static final Logger log = LoggerFactory.getLogger(SapBusinessPartnerClient.class);

    private static final String BP_SERVICE = "/sap/opu/odata/sap/API_BUSINESS_PARTNER";
    private static final String ENTITY_SET_PARTNER = "/A_BusinessPartner";
    // Nav properties — preferred for fetching a partner's child collections.
    // SAP's sandbox rejects $filter on the top-level child entity sets.
    private static final String NAV_ADDRESSES = "/to_BusinessPartnerAddress";
    private static final String NAV_ROLES = "/to_BusinessPartnerRole";

    private final RestTemplate sapRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${sap.api.default-top:10}")
    private int defaultTop;

    public SapBusinessPartnerClient(RestTemplate sapRestTemplate, ObjectMapper objectMapper) {
        this.sapRestTemplate = sapRestTemplate;
        this.objectMapper = objectMapper;
    }

    // -----------------------------------------------------------------------
    // Single entity reads
    // -----------------------------------------------------------------------

    @Retry(name = "sapApi")
    @CircuitBreaker(name = "sapApi")
    @Cacheable(value = "sap.partner", key = "#businessPartnerId")
    public BusinessPartner fetchPartner(String businessPartnerId) {
        String path = BP_SERVICE + ENTITY_SET_PARTNER
            + "('" + urlEncode(businessPartnerId) + "')?$format=json";
        log.info("SAP GET partner id={} path={}", businessPartnerId, path);

        String raw = exchangeForString(path);
        try {
            ODataEntityResponse<BusinessPartner> wrapper = objectMapper.readValue(
                raw, new TypeReference<ODataEntityResponse<BusinessPartner>>() {});
            if (wrapper == null || wrapper.getEntity() == null) {
                throw new SapApiException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "SAP response did not contain a d.entity element; schema drift?");
            }
            return wrapper.getEntity();
        } catch (IOException e) {
            throw new SapApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Failed to parse SAP business partner response: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Collection reads
    // -----------------------------------------------------------------------

    @Retry(name = "sapApi")
    @CircuitBreaker(name = "sapApi")
    @Cacheable(value = "sap.partners-page", key = "#top + '-' + #skip")
    public List<BusinessPartner> listPartners(int top, int skip) {
        int safeTop = top <= 0 ? defaultTop : Math.min(top, 100);
        int safeSkip = Math.max(skip, 0);

        String path = BP_SERVICE + ENTITY_SET_PARTNER
            + "?$top=" + safeTop
            + "&$skip=" + safeSkip
            + "&$format=json";
        log.info("SAP GET partners top={} skip={}", safeTop, safeSkip);

        String raw = exchangeForString(path);
        try {
            ODataCollectionResponse<BusinessPartner> wrapper = objectMapper.readValue(
                raw, new TypeReference<ODataCollectionResponse<BusinessPartner>>() {});
            return wrapper.getResults();
        } catch (IOException e) {
            throw new SapApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Failed to parse SAP business partner list: " + e.getMessage(), e);
        }
    }

    @Retry(name = "sapApi")
    @CircuitBreaker(name = "sapApi")
    @Cacheable(value = "sap.addresses", key = "#businessPartnerId")
    public List<BusinessPartnerAddress> fetchAddresses(String businessPartnerId) {
        String path = BP_SERVICE + ENTITY_SET_PARTNER
            + "('" + urlEncode(businessPartnerId) + "')"
            + NAV_ADDRESSES
            + "?$format=json&$top=50";
        log.info("SAP GET addresses for bp={}", businessPartnerId);

        String raw = exchangeForString(path);
        try {
            ODataCollectionResponse<BusinessPartnerAddress> wrapper = objectMapper.readValue(
                raw, new TypeReference<ODataCollectionResponse<BusinessPartnerAddress>>() {});
            return wrapper.getResults();
        } catch (IOException e) {
            throw new SapApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Failed to parse SAP addresses response: " + e.getMessage(), e);
        }
    }

    @Retry(name = "sapApi")
    @CircuitBreaker(name = "sapApi")
    @Cacheable(value = "sap.roles", key = "#businessPartnerId")
    public List<BusinessPartnerRole> fetchRoles(String businessPartnerId) {
        String path = BP_SERVICE + ENTITY_SET_PARTNER
            + "('" + urlEncode(businessPartnerId) + "')"
            + NAV_ROLES
            + "?$format=json&$top=50";
        log.info("SAP GET roles for bp={}", businessPartnerId);

        String raw = exchangeForString(path);
        try {
            ODataCollectionResponse<BusinessPartnerRole> wrapper = objectMapper.readValue(
                raw, new TypeReference<ODataCollectionResponse<BusinessPartnerRole>>() {});
            return wrapper.getResults();
        } catch (IOException e) {
            throw new SapApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "Failed to parse SAP roles response: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Aggregate reads
    // -----------------------------------------------------------------------

    @Retry(name = "sapApi")
    @CircuitBreaker(name = "sapApi")
    @Cacheable(value = "sap.count")
    public long fetchTotalCount() {
        String path = BP_SERVICE + ENTITY_SET_PARTNER + "/$count";
        log.info("SAP GET $count");
        String raw = exchangeForString(path);
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new SapApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "SAP $count endpoint returned non-numeric response: '" + raw + "'", e);
        }
    }

    // -----------------------------------------------------------------------
    // Shared plumbing
    // -----------------------------------------------------------------------

    private String exchangeForString(String path) {
        try {
            // NOTE: pass the path as a String (not a URI) so RestTemplateBuilder's
            // rootUri is prefixed. Passing a URI instance bypasses the root and
            // the path — being relative — blows up with "URI is not absolute".
            ResponseEntity<String> response = sapRestTemplate.getForEntity(
                path, String.class);
            HttpStatusCode status = response.getStatusCode();
            if (!status.is2xxSuccessful()) {
                throw new SapApiException(
                    org.springframework.http.HttpStatus.valueOf(status.value()),
                    "SAP returned non-2xx: " + status.value());
            }
            return response.getBody() != null ? response.getBody() : "";
        } catch (HttpStatusCodeException upstream) {
            log.warn("SAP upstream error status={} path={} body={}",
                upstream.getStatusCode(), path,
                truncate(upstream.getResponseBodyAsString(), 500));
            throw new SapApiException(
                org.springframework.http.HttpStatus.valueOf(upstream.getStatusCode().value()),
                "SAP upstream error: " + upstream.getStatusText(),
                upstream);
        } catch (ResourceAccessException transport) {
            log.warn("SAP transport error path={} cause={}", path,
                transport.getMessage());
            throw new SapApiException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "SAP transport error: " + transport.getMessage(),
                transport);
        }
    }

    private static String urlEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    /**
     * OData-compatible URL encoding for {@code $filter} values.
     *
     * <p>The stdlib {@link URLEncoder} is form-encoded: it emits {@code +}
     * for spaces and {@code %27} for single quotes. SAP's OData v2 parser
     * accepts {@code %20} for spaces but refuses {@code +} ("Invalid token
     * detected at position N"), and expects literal single quotes around
     * string literals, not percent-encoded. This method fixes both.
     */
    private static String odataEncode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("%27", "'");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }
}
