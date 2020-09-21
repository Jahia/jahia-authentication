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
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dgaillard
 */
public class ManageMappers extends Action {
    private SettingsService settingsService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        String action = parameters.get("action").get(0);
        String connectorServiceName;
        String mapperServiceName;
        JSONObject response = new JSONObject();
        if (action.equals("getConnectorProperties")) {
            connectorServiceName = parameters.get(JahiaAuthConstants.CONNECTOR_SERVICE_NAME).get(0);
            ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=" + connectorServiceName + ")");
            if (connectorService == null) {
                response.put("error", "Cannot find connector");
                return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, response);
            }
            response.put("connectorProperties", new JSONArray(connectorService.getAvailableProperties().stream().map(JSONObject::new).collect(Collectors.toList())));
        } else if (action.equals("getMapperProperties")) {
            mapperServiceName = parameters.get(JahiaAuthConstants.MAPPER_SERVICE_NAME).get(0);
            Mapper mapperService = BundleUtils.getOsgiService(Mapper.class, "(" + JahiaAuthConstants.MAPPER_SERVICE_NAME + "=" + mapperServiceName + ")");
            if (mapperService == null) {
                response.put("error", "Cannot find connector");
                return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, response);
            }
            response.put("mapperProperties", new JSONArray(mapperService.getProperties().stream().map(JSONObject::new).collect(Collectors.toList())));
        } else if (action.equals("getMapperMapping")) {
            if (!parameters.containsKey(JahiaAuthConstants.MAPPER_SERVICE_NAME)
                    || !parameters.containsKey(JahiaAuthConstants.CONNECTOR_SERVICE_NAME)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            connectorServiceName = parameters.get(JahiaAuthConstants.CONNECTOR_SERVICE_NAME).get(0);
            mapperServiceName = parameters.get(JahiaAuthConstants.MAPPER_SERVICE_NAME).get(0);

            Settings settings = settingsService.getSettings(renderContext.getSite().getSiteKey());
            Settings.Values mapperNode = settings.getValues(connectorServiceName).getSubValues(JahiaAuthConstants.MAPPERS_NODE_NAME).getSubValues(mapperServiceName);

            if (mapperNode.isEmpty()) {
                return new ActionResult(HttpServletResponse.SC_OK, null, response);
            }

            JSONArray jsonArrayMapping = new JSONArray(mapperNode.getProperty(JahiaAuthConstants.PROPERTY_MAPPING));
            response.put(JahiaAuthConstants.PROPERTY_IS_ENABLED, mapperNode.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED));
            response.put(JahiaAuthConstants.PROPERTY_MAPPING, jsonArrayMapping);

        } else if (action.equals("setMapperMapping")) {
            if (!parameters.containsKey(JahiaAuthConstants.PROPERTY_IS_ENABLED)
                    || !parameters.containsKey(JahiaAuthConstants.MAPPER_SERVICE_NAME)
                    || !parameters.containsKey(JahiaAuthConstants.CONNECTOR_SERVICE_NAME)) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            connectorServiceName = parameters.get(JahiaAuthConstants.CONNECTOR_SERVICE_NAME).get(0);
            mapperServiceName = parameters.get(JahiaAuthConstants.MAPPER_SERVICE_NAME).get(0);
            boolean enabled = Boolean.parseBoolean(parameters.get(JahiaAuthConstants.PROPERTY_IS_ENABLED).get(0));
            if (enabled && !parameters.containsKey(JahiaAuthConstants.PROPERTY_MAPPING)) {
                response.put("error", "mapping is missing");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            Settings settings = settingsService.getSettings(renderContext.getSite().getSiteKey());

            List<String> mapping = (parameters.containsKey(JahiaAuthConstants.PROPERTY_MAPPING))?parameters.get(JahiaAuthConstants.PROPERTY_MAPPING):new ArrayList<String>();

            Settings.Values mappersNode = settings.getValues(connectorServiceName).getSubValues(JahiaAuthConstants.MAPPERS_NODE_NAME).getSubValues(mapperServiceName);

            mappersNode.setProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED, enabled);
            mappersNode.setProperty(JahiaAuthConstants.PROPERTY_MAPPING, mapping.toString());

            settingsService.storeSettings(settings);
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    public void setSettingsService(SettingsServiceImpl settingsService) {
        this.settingsService = settingsService;
    }
}
