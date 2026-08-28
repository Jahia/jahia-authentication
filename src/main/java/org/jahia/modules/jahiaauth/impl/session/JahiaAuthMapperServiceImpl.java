package org.jahia.modules.jahiaauth.impl.session;

import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.jahiaauth.impl.AccountNameCheck;
import org.jahia.modules.jahiaauth.impl.LogSafeValue;
import org.jahia.modules.jahiaauth.impl.ServiceFilter;
import org.jahia.modules.jahiaauth.impl.SubjectCheck;
import org.jahia.modules.jahiaauth.impl.VerifiedSubjectCheck;
import org.jahia.modules.jahiaauth.service.*;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dgaillard
 */
@Component(service = JahiaAuthMapperService.class, immediate = true)
public class JahiaAuthMapperServiceImpl implements JahiaAuthMapperService {

    private static final Logger logger = LoggerFactory.getLogger(JahiaAuthMapperServiceImpl.class);

    private BundleContext bundleContext;

    @Activate
    public void init(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void executeMapper(HttpServletRequest request, MapperConfig mapperConfig, Map<String, Object> connectorProperties) throws JahiaAuthException {
        Mapper mapper = BundleUtils.getOsgiService(Mapper.class,
                ServiceFilter.byName(JahiaAuthConstants.MAPPER_SERVICE_NAME, mapperConfig.getMapperName()));
        String subject = assertedSubject(mapperConfig.getConnectorName(), connectorProperties);
        Map<String, MappedProperty> mapperResult = getMapperResults(connectorProperties, mapper, mapperConfig);
        if (subject != null) {
            // The mapper reads the subject the same way it reads the login id, and neither is a mapping.
            mapperResult.put(JahiaAuthConstants.SSO_SUBJECT,
                    new MappedProperty(new MappedPropertyInfo(JahiaAuthConstants.SSO_SUBJECT), subject));
        }
        if (mapper != null) {
            mapper.executeMapper(mapperResult, mapperConfig);
        }
        record(request, mapperConfig.getMapperName(),
                toResult(mapperResult, mapperConfig.getSiteKey(), mapperConfig.getConnectorName()));
    }

    /**
     * Reads the subject the identity provider asserted, out of the property the connector declares.
     * <p>
     * This is where the identity a sign-in resolves an account by is read, so this is where the rule on
     * its source is enforced. The connector names the property, and a configuration names the property a
     * mapping reads, so a mapping states nothing this method reads. A connector that declares no such
     * property states no identity, and the sign-in then resolves no account.
     *
     * @return the subject, or {@code null} when the connector declares none, when the identity provider
     *         returned none, or when the value describes no single identity
     */
    private static String assertedSubject(String connectorName, Map<String, Object> connectorProperties) {
        String declared = verifiedSubjectProperty(connectorName);
        String refusal = VerifiedSubjectCheck.refusalReason(connectorName, declared);
        if (refusal != null) {
            logger.error("Read no asserted identity, so this sign-in resolves no account. {}", refusal);
            return null;
        }
        Object asserted = connectorProperties.get(declared);
        String unusable = SubjectCheck.refusalReason(asserted);
        if (unusable != null) {
            logger.error("Read no asserted identity from connector {}, so this sign-in resolves no"
                    + " account. Its verified subject property '{}' is the one read, and {}.",
                    LogSafeValue.of(connectorName), LogSafeValue.of(declared), unusable);
            return null;
        }
        return asserted.toString();
    }

    @Override
    public void recordConnectorProperties(HttpServletRequest request, String connectorName,
            Map<String, MappedProperty> properties) {
        // No login id and no site key. Both halves of an identity come from a mapping, which is the one
        // place that states where the name was read from.
        SessionMapperResultsStore.put(request, connectorName, new MapperResult(null, null, properties));
    }

    private static void record(HttpServletRequest request, String mapperName, MapperResult result) {
        SessionMapperResultsStore.put(request, mapperName, result);
    }

    @Override
    public Map<String, MapperResult> getMapperResults(HttpServletRequest request) {
        return SessionMapperResultsStore.getAll(request);
    }

    @Override
    public MapperResult getMapperResult(HttpServletRequest request, String mapperName) {
        return SessionMapperResultsStore.get(request, mapperName);
    }

    @Override
    public void executeConnectorResultProcessors(HttpServletRequest httpRequest, ConnectorConfig connectorConfig, Map<String, Object> results) {
        try {
            ServiceReference[] refs = bundleContext.getAllServiceReferences(ConnectorResultProcessor.class.getName(), null);
            if (refs != null && refs.length > 0) {
                for (ServiceReference ref : refs) {
                    ConnectorResultProcessor connectorResultProcessor = (ConnectorResultProcessor) bundleContext.getService(ref);
                    connectorResultProcessor.execute(httpRequest, connectorConfig, results);
                }
            }
        } catch (InvalidSyntaxException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    /**
     * Splits what a mapper produced into the shape a result carries: the account, the site it is
     * resolved against, the identity that resolves it, and the profile properties. Those four travel as
     * fields of their own, so a consumer reads them without filtering the properties.
     */
    private static MapperResult toResult(Map<String, MappedProperty> mapperResult, String siteKey,
            String connectorName) {
        Map<String, MappedProperty> properties = new LinkedHashMap<>(mapperResult);
        MappedProperty login = properties.remove(JahiaAuthConstants.SSO_LOGIN);
        MappedProperty asserted = properties.remove(JahiaAuthConstants.SSO_SUBJECT);
        // The site of the connector is the only one that stands. A mapping may feed the siteKey
        // property, and a connector may pass one along, and neither decides where an account resolves.
        properties.remove(JahiaAuthConstants.SITE_KEY);
        String loginId = login == null || login.getValue() == null ? null : String.valueOf(login.getValue());
        String subject = asserted == null || asserted.getValue() == null ? null : String.valueOf(asserted.getValue());
        // A result with no subject states no identity, so it names no connector either. The valve reads
        // both or neither, and a half-stated identity would resolve an account by its name alone.
        return new MapperResult(loginId, siteKey, subject == null ? null : connectorName, subject, properties);
    }

    private Map<String, MappedProperty> getMapperResults(Map<String, Object> propertiesResult, Mapper mapper, MapperConfig mapperConfig) throws JahiaAuthException {
        Map<String, MappedPropertyInfo> m = mapper != null ? mapper.getProperties().stream().collect(Collectors.toMap(MappedPropertyInfo::getName, p -> p)) : null;
        return resolveMappings(propertiesResult, m, mapperConfig);
    }

    /**
     * Reads what a connector returned into the properties a mapper declares.
     * <p>
     * This is where a login id becomes an account name, so this is where a value that cannot be a name
     * is refused. The name is chosen by the deployment and takes no part in resolving the account, so a
     * mapping may read it from any property the connector returned. The subject is what resolves the
     * account, and {@code assertedSubject} reads it from the property the connector declares.
     * <p>
     * A login id this repository cannot hold as a name is dropped, and the result then names no account,
     * so the valve signs nobody in. The other mappings of the mapper stand, because they feed profile
     * properties.
     */
    static Map<String, MappedProperty> resolveMappings(Map<String, Object> propertiesResult,
            Map<String, MappedPropertyInfo> m, MapperConfig mapperConfig)
            throws JahiaAuthException {
        Map<String, MappedProperty> mapperResult = new HashMap<>();
        for (Mapping mapping : mapperConfig.getMappings()) {
            if (m != null && m.containsKey(mapping.getMappedProperty()) && m.get(mapping.getMappedProperty()).isMandatory() && !propertiesResult.containsKey(mapping.getConnectorProperty())) {
                throw new JahiaAuthException("Could not execute mapper: missing mandatory property");
            }
            if (propertiesResult.containsKey(mapping.getConnectorProperty())) {
                Object returned = propertiesResult.get(mapping.getConnectorProperty());
                if (JahiaAuthConstants.SSO_LOGIN.equals(mapping.getMappedProperty())) {
                    String unusable = AccountNameCheck.refusalReason(returned);
                    if (unusable != null) {
                        logger.error("Read no login id from mapper {}, so this sign-in resolves no account."
                                + " Mapper {} reads {} from '{}', and {}.",
                                LogSafeValue.of(mapperConfig.getMapperName()),
                                LogSafeValue.of(mapperConfig.getMapperName()), JahiaAuthConstants.SSO_LOGIN,
                                LogSafeValue.of(mapping.getConnectorProperty()), unusable);
                        continue;
                    }
                }
                mapperResult.put(mapping.getMappedProperty(), new MappedProperty(m != null ? m.get(mapping.getMappedProperty()) : new MappedPropertyInfo(mapping.getMappedProperty()),
                        // to avoid serialization error of null json object property
                        String.valueOf(returned)));
            }
        }

        return mapperResult;
    }

    /**
     * @param connectorName the connector a mapper reads from
     * @return the property carrying the subject that connector's identity provider verified, or
     *         {@code null} when the connector names none or is not deployed
     */
    private static String verifiedSubjectProperty(String connectorName) {
        ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class,
                ServiceFilter.byName(JahiaAuthConstants.CONNECTOR_SERVICE_NAME, connectorName));
        return connectorService == null ? null : connectorService.getVerifiedSubjectProperty();
    }
}
