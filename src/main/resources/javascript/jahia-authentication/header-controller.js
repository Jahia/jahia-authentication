(function() {
    'use strict';

    angular.module('JahiaOAuthApp').controller('HeaderController', HeaderController);

    HeaderController.$inject = ['$location', '$routeParams', 'i18nService'];

    function HeaderController($location, $routeParams, i18nService) {
        var vm = this;

        // Functions
        vm.isMapperView = isMapperView;
        vm.goToConnectors = goToConnectors;
        vm.getConnectorName = getConnectorName;

        init();

        function isMapperView() {
            return $location.path() != '/connectors';
        }

        function goToConnectors() {
            $location.path('/connectors');
        }

        function getConnectorName() {
            return i18nService.message($routeParams.connectorServiceName + '.title');
        }

        function init() {
            i18nService.addKey(auth_i18n);
        }
    }
})();