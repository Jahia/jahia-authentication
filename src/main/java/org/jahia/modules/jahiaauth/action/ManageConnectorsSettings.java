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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.jbig2.util.log.Logger;
import org.apache.pdfbox.jbig2.util.log.LoggerFactory;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.jahiaauth.impl.SettingsServiceImpl;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorService;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.Settings;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.tools.files.FileUpload;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ManageConnectorsSettings extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ManageConnectorsSettings.class);
    private SettingsServiceImpl settingsService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        JSONObject response = new JSONObject();
        // Get registered data
        if (req.getMethod().equals(JahiaAuthConstants.METHOD_GET)) {
            if (!parameters.containsKey(JahiaAuthConstants.CONNECTOR_SERVICE_NAME) || !parameters.containsKey(JahiaAuthConstants.PROPERTIES) || parameters.get(JahiaAuthConstants.PROPERTIES).isEmpty()) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            String nodeName = parameters.get(JahiaAuthConstants.CONNECTOR_SERVICE_NAME).get(0);

            String siteKey = renderContext.getSite().getSiteKey();
            Settings settings = settingsService.getSettings(siteKey);
            Settings.Values v = settings.getValues(nodeName);
            if (v.isEmpty()) {
                return new ActionResult(HttpServletResponse.SC_NO_CONTENT, null, response);
            }

            for (String property : parameters.get(JahiaAuthConstants.PROPERTIES)) {
                if (!v.getListProperty(property).isEmpty()) {
                    JSONArray callbackUrls = new JSONArray();
                    v.getListProperty(property).forEach(callbackUrls::put);
                    response.put(property, callbackUrls);
                } else if (v.getProperty(property) != null) {
                    if (property.equals(JahiaAuthConstants.PROPERTY_IS_ENABLED)) {
                        response.put(property, Boolean.valueOf(v.getProperty(property)));
                    } else {
                        response.put(property, v.getProperty(property));
                    }
                }
            }
        }
        // Register or update data
        else {
            if (!parameters.containsKey(JahiaAuthConstants.CONNECTOR_SERVICE_NAME) || !parameters.containsKey(JahiaAuthConstants.PROPERTIES) || parameters.get(JahiaAuthConstants.PROPERTIES).isEmpty()) {
                response.put("error", "required properties are missing in the request");
                return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
            }

            try {
                String connectorServiceName = parameters.get(JahiaAuthConstants.CONNECTOR_SERVICE_NAME).get(0);
                ConnectorService connectorService = BundleUtils.getOsgiService(ConnectorService.class, "(" + JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=" + connectorServiceName + ")");

                FileUpload fup = (FileUpload) req.getAttribute("fileUpload");

                Map<String, Object> properties = new ObjectMapper().readValue(parameters.get(JahiaAuthConstants.PROPERTIES).get(0), HashMap.class);
                if (!properties.containsKey(JahiaAuthConstants.PROPERTY_IS_ENABLED)) {
                    response.put("error", "required properties are missing in the request");
                    return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, response);
                }


                Settings settings = settingsService.getSettings(renderContext.getSite().getSiteKey());
                Settings.Values v = settings.getValues(connectorServiceName);
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (fup.getFileItems().containsKey("file_" + entry.getKey())) {
                        File s = fup.getFileItems().get("file_" + entry.getKey()).getStoreLocation();
                        v.setBinaryProperty(entry.getKey(), FileUtils.readFileToByteArray(s));
                    } else if (entry.getValue() instanceof List) {
                        v.setListProperty(entry.getKey(), (List) entry.getValue());
                    } else {
                        v.setProperty(entry.getKey(), entry.getValue().toString());
                    }
                }

                connectorService.validateSettings(new ConnectorConfig(settings, connectorServiceName));

                settingsService.storeSettings(settings);
            } catch (Exception e) {
                JSONObject error = new JSONObject();
                if (logger.isDebugEnabled()) {
                    logger.debug("error while saving settings", e);
                }
                if (e.getMessage() != null) {
                    error.put("error", e.getMessage());
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        error.put("error", e.getMessage() + " - " + e.getCause().getMessage());
                    }
                } else {
                    error.put("error", "Error when saving");
                }
                error.put("type", e.getClass().getSimpleName());
                return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, error);
            }
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    public void setSettingsService(SettingsServiceImpl settingsService) {
        this.settingsService = settingsService;
    }
}
