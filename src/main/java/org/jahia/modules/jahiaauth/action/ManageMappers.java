/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jahiaauth.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaauth.impl.SettingsServiceImpl;
import org.jahia.modules.jahiaauth.service.*;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dgaillard
 */
public class ManageMappers extends Action {
    public static final String ERROR = "error";
    private SettingsService settingsService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        JsonNode params = new ObjectMapper().readTree(req.getInputStream());
        String action = params.get("action").asText();
        switch (action) {
            case "getConnectorProperties":
                return getConnectorProperties(params);
            case "getMapperProperties":
                return getMapperProperties(params);
            case "getMapperMapping":
                return getMapperMapping(renderContext, params);
            case "setMapperMapping":
                return setMapperMapping(renderContext, params);
            default:
                return new ActionResult(HttpServletResponse.SC_OK, null, new JSONObject());
        }
    }

    private ActionResult setMapperMapping(RenderContext renderContext, JsonNode parameters) throws JSONException, IOException {
        JSONObject response = new JSONObject();
        if (!parameters.hasNonNull(JahiaAuthConstants.PROPERTIES)
                || !parameters.hasNonNull(JahiaAuthConstants.MAPPER_SERVICE_NAME)
                || !parameters.hasNonNull(JahiaAuthConstants.CONNECTOR_SERVICE_NAME)) {
            response.put(ERROR, "required properties are missing in the request");
            return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
        }

        String connectorServiceName = parameters.get(JahiaAuthConstants.CONNECTOR_SERVICE_NAME).asText();
        String mapperServiceName = parameters.get(JahiaAuthConstants.MAPPER_SERVICE_NAME).asText();
        JsonNode properties = parameters.get(JahiaAuthConstants.PROPERTIES);

        boolean enabled = properties.get(JahiaAuthConstants.PROPERTY_IS_ENABLED).asBoolean();
        if (enabled && !parameters.hasNonNull(JahiaAuthConstants.PROPERTY_MAPPING)) {
            response.put(ERROR, "mapping is missing");
            return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
        }

        Settings settings = settingsService.getSettings(renderContext.getSite().getSiteKey());

        JsonNode mapping = (parameters.hasNonNull(JahiaAuthConstants.PROPERTY_MAPPING)) ? parameters.get(JahiaAuthConstants.PROPERTY_MAPPING) : new ArrayNode(JsonNodeFactory.instance);

        Settings.Values mappersNode = settings.getValues(connectorServiceName).getSubValues(JahiaAuthConstants.MAPPERS_NODE_NAME).getSubValues(mapperServiceName);

        mappersNode.setProperty(JahiaAuthConstants.PROPERTY_MAPPING, mapping.toString());
        for (Iterator<String> it = properties.fieldNames(); it.hasNext(); ) {
            String entry = it.next();
            if (properties.get(entry).isArray()) {
                mappersNode.setListProperty(entry, new ObjectMapper().treeToValue(properties.get(entry), ArrayList.class));
            } else {
                mappersNode.setProperty(entry, properties.get(entry).asText());
            }
        }

        settingsService.storeSettings(settings);
        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    private ActionResult getMapperMapping(RenderContext renderContext, JsonNode parameters) throws JSONException {
        JSONObject response = new JSONObject();
        if (!parameters.hasNonNull(JahiaAuthConstants.MAPPER_SERVICE_NAME)
                || !parameters.hasNonNull(JahiaAuthConstants.CONNECTOR_SERVICE_NAME)) {
            response.put(ERROR, "required properties are missing in the request");
            return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
        }

        String connectorServiceName = parameters.get(JahiaAuthConstants.CONNECTOR_SERVICE_NAME).asText();
        String mapperServiceName = parameters.get(JahiaAuthConstants.MAPPER_SERVICE_NAME).asText();

        Settings settings = settingsService.getSettings(renderContext.getSite().getSiteKey());
        Settings.Values mapperNode = settings.getValues(connectorServiceName).getSubValues(JahiaAuthConstants.MAPPERS_NODE_NAME).getSubValues(mapperServiceName);

        if (mapperNode.isEmpty()) {
            return new ActionResult(HttpServletResponse.SC_OK, null, response);
        }

        JSONArray jsonArrayMapping = new JSONArray(mapperNode.getProperty(JahiaAuthConstants.PROPERTY_MAPPING));
        response.put(JahiaAuthConstants.PROPERTY_IS_ENABLED, mapperNode.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED));
        response.put(JahiaAuthConstants.PROPERTY_MAPPING, jsonArrayMapping);

        if (parameters.get(JahiaAuthConstants.PROPERTIES) != null) {
            for (Iterator<String> it = parameters.get(JahiaAuthConstants.PROPERTIES).fieldNames(); it.hasNext(); ) {
                String property = it.next();
                if (!mapperNode.getListProperty(property).isEmpty()) {
                    JSONArray array = new JSONArray();
                    mapperNode.getListProperty(property).forEach(array::put);
                    response.put(property, array);
                } else if (mapperNode.getProperty(property) != null) {
                    if (property.equals(JahiaAuthConstants.PROPERTY_IS_ENABLED)) {
                        response.put(property, Boolean.valueOf(mapperNode.getProperty(property)));
                    } else {
                        response.put(property, mapperNode.getProperty(property));
                    }
                }
            }
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    private ActionResult getMapperProperties(JsonNode parameters) throws JSONException {
        JSONObject response = new JSONObject();
        String mapperServiceName = parameters.get(JahiaAuthConstants.MAPPER_SERVICE_NAME).asText();
        Mapper mapperService = BundleUtils.getOsgiService(Mapper.class, "(" + JahiaAuthConstants.MAPPER_SERVICE_NAME + "=" + mapperServiceName + ")");
        if (mapperService == null) {
            response.put(ERROR, "Cannot find connector");
            return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, response);
        }
        response.put("mapperProperties", new JSONArray(mapperService.getProperties().stream().map(JSONObject::new).collect(Collectors.toList())));
        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    private ActionResult getConnectorProperties(JsonNode parameters) throws JSONException {
        JSONObject response = new JSONObject();
        String connectorServiceName = parameters.get(JahiaAuthConstants.CONNECTOR_SERVICE_NAME).asText();
        ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=" + connectorServiceName + ")");
        if (connectorService == null) {
            response.put(ERROR, "Cannot find connector");
            return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, response);
        }
        response.put("connectorProperties", new JSONArray(connectorService.getAvailableProperties().stream().map(JSONObject::new).collect(Collectors.toList())));
        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    public void setSettingsService(SettingsServiceImpl settingsService) {
        this.settingsService = settingsService;
    }
}
