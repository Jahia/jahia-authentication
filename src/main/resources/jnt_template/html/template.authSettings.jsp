<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<html lang="${fn:substring(renderContext.request.locale,0,2)}">
    <head>
        <meta charset="UTF-8">
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="jcr:description" inherited="true" var="description"/>
        <jcr:nodeProperty node="${renderContext.mainResource.node}" name="jcr:createdBy" inherited="true" var="author"/>
        <c:set var="keywords" value="${jcr:getKeywords(renderContext.mainResource.node, true)}"/>
        <c:if test="${!empty description}"><meta name="description" content="${fn:escapeXml(description.string)}" /></c:if>
        <c:if test="${!empty author}"><meta name="author" content="${fn:escapeXml(author.string)}" /></c:if>
        <c:if test="${!empty keywords}"><meta name="keywords" content="${fn:escapeXml(keywords)}" /></c:if>
        <title>${fn:escapeXml(renderContext.mainResource.node.displayableName)}</title>
        <template:addResources type="javascript" resources="i18n/jahia-authentication-i18n_${currentResource.locale}.js" var="i18nJSFile"/>
        <c:if test="${empty i18nJSFile}">
            <template:addResources type="javascript" resources="i18n/jahia-authentication-i18n_en.js"/>
        </c:if>
    </head>

    <body>

    <div class=" clearfix">
        <template:area path="pagecontent"/>
    </div>

    <template:addResources type="css" resources="jahia-authentication/vendor/fontNunitoSans.css"/>
    <template:addResources type="css" resources="jahia-authentication/vendor/material-icons.css"/>

    </body>
</html>
