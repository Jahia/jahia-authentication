import {registry} from '@jahia/ui-extender';

export default function () {
    const config = window.contextJsParameters.config;
    let mode = 'render';

    if (config && config.operatingMode !== 'distantPublicationServer') {
        mode = 'editframe';
    }

    window.jahia.i18n.loadNamespaces('jahia-authentication');
    registry.add('adminRoute', 'jahia-authentication', {
        targets: ['administration-sites:99'],
        requiredPermission: 'canSetupJahiaAuth',
        icon: window.jahia.moonstone.toIconComponent('Security'),
        label: 'jahia-authentication:label',
        isSelectable: true,
        requireModuleInstalledOnSite: 'jahia-authentication',
        iframeUrl: window.contextJsParameters.contextPath + '/cms/' + mode + '/default/$lang/sites/$site-key.auth-connector-site-settings.html?redirect=false'
    });
}
