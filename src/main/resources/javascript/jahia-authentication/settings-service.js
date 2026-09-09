(function() {
    'use strict';

    angular.module('JahiaOAuthApp').service('settingsService', settingsService);

    settingsService.$inject = ['$http', 'jahiaContext'];

    function settingsService($http, jahiaContext) {
        return {
            getConnectorData: getConnectorData,
            setConnectorData: setConnectorData,
            getConnectorProperties: getConnectorProperties,
            getMapperProperties: getMapperProperties,
            getMapperMapping: getMapperMapping,
            setMapperMapping: setMapperMapping
        };

        function getConnectorData(nodeName, properties) {
            var propertiesAsString = '';
            angular.forEach(properties, function(property) {
                propertiesAsString += '&properties=' + property;
            });
            // POST, not GET: the fetch metadata policy only refuses a cross-site request that writes,
            // so the method is what brings this read under it. The action accepts POST only, so the
            // method cannot be downgraded back to GET by a caller outside this service.
            return $http.post(jahiaContext.basePreview + jahiaContext.sitePath + '.readConnectorsSettingsAction.do?connectorServiceName=' + nodeName + propertiesAsString);
        }

        function setConnectorData(data, options) {
            return $http({
                method: 'POST',
                url: jahiaContext.basePreview + jahiaContext.sitePath + '.writeConnectorsSettingsAction.do',
                data: data,
                headers: {'Content-Type': undefined},
                transformRequest: function (data) {
                    var formData = new FormData();
                    angular.forEach(data, function (value, key) {
                        if (!key.startsWith('file_') && typeof value === 'object') {
                            formData.append(key, JSON.stringify(value));
                        } else {
                            formData.append(key, value);
                        }
                    });
                    return formData;
                }
            });
        }

        function getConnectorProperties(data) {
            data.action = 'getConnectorProperties';
            return $http({
                method: 'POST',
                url: jahiaContext.basePreview + jahiaContext.sitePath + '.readMappersAction.do',
                data: data
            })
        }

        function getMapperProperties(data) {
            data.action = 'getMapperProperties';
            return $http({
                method: 'POST',
                url: jahiaContext.basePreview + jahiaContext.sitePath + '.readMappersAction.do',
                data: data
            })
        }

        function getMapperMapping(data) {
            data.action = 'getMapperMapping';
            return $http({
                method: 'POST',
                url: jahiaContext.basePreview + jahiaContext.sitePath + '.readMappersAction.do',
                data: data
            })
        }

        function setMapperMapping(data) {
            data.action = 'setMapperMapping';
            return $http({
                method: 'POST',
                url: jahiaContext.basePreview + jahiaContext.sitePath + '.writeMappersAction.do',
                data: data
            })
        }
    }
})();
