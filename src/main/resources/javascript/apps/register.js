(function() {
    window.jahia.i18n.loadNamespaces('jahia-authentication');
    window.jahia.uiExtender.registry.add('adminRoute', 'jahia-authentication', {
        targets: ['administration-sites:99'],
        icon: window.jahia.moonstone.toIconComponent('Security'),
        label: 'jahia-authentication:label',
        isSelectable: true,
        requireModuleInstalledOnSite: 'jahia-authentication',
        iframeUrl: window.contextJsParameters.contextPath + '/cms/render/default/$lang/sites/$site-key.auth-connector-site-settings.html?redirect=false'
    });
})();
