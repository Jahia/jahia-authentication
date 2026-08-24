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
package org.jahia.modules.jahiaauth.service;

import java.io.IOException;
import java.util.List;

/**
 * Service to be implemented by a connector to allow Jahia OAuth to work
 *
 * @author dgaillard
 */
public interface ConnectorService {
    /**
     * This method get the list of available properties with this connector
     *
     * @return List the list of available properties
     */
    List<ConnectorPropertyInfo> getAvailableProperties();

    void validateSettings(ConnectorConfig settings) throws IOException;

    /**
     * Names the property of this connector that carries the subject the identity provider verified.
     * <p>
     * The framework signs a user in under the name held by {@link JahiaAuthConstants#SSO_LOGIN}, and
     * it copies that name from whichever property an administrator mapped onto it. A property the end
     * user can set at the identity provider therefore decides which Jahia account is signed in. OIDC
     * carries the verified subject in {@code sub}, and SAML in the {@code NameID}.
     * <p>
     * A connector that names its property lets the framework refuse a mapping that reads any other
     * one, and the refusal lands when an administrator writes the configuration rather than when a
     * user signs in. A connector that has no such property returns {@code null}, and the framework
     * then warns and accepts the configuration, because it has nothing to check against.
     *
     * @return the name of the property carrying the verified subject, or {@code null} when the
     *         connector has none
     */
    default String getVerifiedSubjectProperty() {
        return null;
    }
}
