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
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.Settings;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
@Component(
    service = Action.class,
    property = {
        "action.name=readConnectorsSettingsAction",
        "action.requiredMethods=GET",
        "action.requiredPermission=canSetupJahiaAuth"
    }
)
public class ReadConnectorsSettings extends Action {
    public static final String ERROR = "error";
    public static final String REQUIRED_PROPERTIES_ARE_MISSING_IN_THE_REQUEST = "required properties are missing in the request";
    private SettingsService settingsService;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        // Get registered data
        return getSettings(renderContext, parameters);
    }

    private ActionResult getSettings(RenderContext renderContext, Map<String, List<String>> parameters) throws JSONException {
        JSONObject response = new JSONObject();
        if (!parameters.containsKey(JahiaAuthConstants.CONNECTOR_SERVICE_NAME) || !parameters.containsKey(JahiaAuthConstants.PROPERTIES) || parameters.get(JahiaAuthConstants.PROPERTIES).isEmpty()) {
            response.put(ERROR, REQUIRED_PROPERTIES_ARE_MISSING_IN_THE_REQUEST);
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
                JSONArray array = new JSONArray();
                v.getListProperty(property).forEach(array::put);
                response.put(property, array);
            } else if (v.getProperty(property) != null) {
                if (property.equals(JahiaAuthConstants.PROPERTY_IS_ENABLED)) {
                    response.put(property, Boolean.valueOf(v.getProperty(property)));
                } else {
                    response.put(property, v.getProperty(property));
                }
            }
        }
        return new ActionResult(HttpServletResponse.SC_OK, null, response);
    }

    @Reference
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
