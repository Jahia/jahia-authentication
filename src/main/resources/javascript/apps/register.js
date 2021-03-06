// As the file is not babelized we need to use old JS for compatibility.
const config = window.contextJsParameters.config;
var mode = 'render';

if (config && config.operatingMode !== 'distantPublicationServer') {
    mode = 'editframe';
}

(function () {
    window.jahia.i18n.loadNamespaces('jahia-authentication');
    window.jahia.uiExtender.registry.add('adminRoute', 'jahia-authentication', {
        targets: ['administration-sites:99'],
        icon: window.jahia.moonstone.toIconComponent('Security'),
        label: 'jahia-authentication:label',
        isSelectable: true,
        requireModuleInstalledOnSite: 'jahia-authentication',
        iframeUrl: window.contextJsParameters.contextPath + '/cms/' + mode + '/default/$lang/sites/$site-key.auth-connector-site-settings.html?redirect=false'
    });
})();
