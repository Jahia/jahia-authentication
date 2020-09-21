(function() {
    'use strict';

    angular.module('JahiaOAuthApp', ['ngMaterial', 'ngRoute', 'ngAnimate', 'ngMessages', 'i18n']);

    angular.module('JahiaOAuthApp').config(jahiaOAuthAppConfig);

    jahiaOAuthAppConfig.$inject = ['$mdThemingProvider', '$routeProvider'];

    function jahiaOAuthAppConfig($mdThemingProvider, $routeProvider) {
        $routeProvider
            .when('/connectors', {
                templateUrl: 'connectors.html'
            }).when('/mappers/:connectorServiceName', {
                templateUrl: 'mappers.html'
            }).otherwise('/connectors');

        $mdThemingProvider.theme('error-toast');
        $mdThemingProvider.theme('success-toast');

        $mdThemingProvider.theme('JahiaOAuthApp')
            .primaryPalette('blue-grey')
            .accentPalette('blue')
            .warnPalette('red');

        $mdThemingProvider.setDefaultTheme('JahiaOAuthApp');
    }

    angular.module('JahiaOAuthApp').filter('selectable', selectableFilter);

    function selectableFilter() {
        function isNotMappedOrCurrentlySelected(fieldName, params) {
            var isNotMappedOrCurrentlySelected = true;
            if (params.selected && params.selected.name == fieldName) {
                return isNotMappedOrCurrentlySelected;
            } else {
                angular.forEach(params.mapping, function (entry) {
                    if (entry[params.key] && entry[params.key].name == fieldName) {
                        isNotMappedOrCurrentlySelected = false;
                    }
                });
            }
            return isNotMappedOrCurrentlySelected;
        }

        return function (options, params) {
            if (options.length == 0 || params.mapping.length == 0) {
                return options;
            } else {
                var newOptions = [];
                angular.forEach(options, function(option) {
                    if (isNotMappedOrCurrentlySelected(option.name, params)) {
                        newOptions.push(option);
                    }
                });
                return newOptions;
            }
        }
    }

    angular.module('JahiaOAuthApp').filter('typeMatch', typeMatchFilter);

    function typeMatchFilter() {
        return function (options, type) {
            var newOptions = [];
            angular.forEach(options, function(option) {
                if (option.valueType === type || (type ==='email' && option.valueType ==='string')) {
                    newOptions.push(option);
                }
            });
            return newOptions;
        }
    }
})();