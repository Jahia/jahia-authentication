(function() {
    'use strict';

    angular.module('i18n', []).service('i18nService', i18nService);

    function i18nService() {
        var i18nMap = {};

        return {
            addKey: addKey,
            message: message,
            format: format
        };

        function message(key) {
            if (i18nMap && i18nMap[key]) {
                return i18nMap[key];
            } else {
                return '???' + key + '???';
            }
        }

        function format(key, params) {
            var replacer = function(params){
                return function(s, index) {
                    return params[index] ? (params[index] == '__void__' ? '' : params[index]) : '';
                };
            };

            if(params){
                if (i18nMap && i18nMap[key]) {
                    return i18nMap[key].replace(/\{(\w+)\}/g, replacer(params.split('|')));
                } else {
                    return '???' + key + '???';
                }
            } else {
                return this.message(key);
            }
        }

        function addKey(newI18nMap) {
            angular.forEach(newI18nMap, function (value, key) {
                i18nMap[key] = value;
            });
        }
    }

    angular.module('i18n').directive('messageKey', messageKeyDirective);

    messageKeyDirective.$inject = ['i18nService'];

    function messageKeyDirective(i18nService) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var i18n;
                if(!attrs.messageParams){
                    i18n = i18nService.message(attrs.messageKey);
                } else {
                    i18n = i18nService.format(attrs.messageKey, attrs.messageParams);
                }

                if(attrs.messageAttr) {
                    // store the i18n in the specified element attr
                    element.attr(attrs.messageAttr, i18n);
                } else {
                    // set the i18n as element text
                    element.text(i18n);
                }
            }
        };
    }

    angular.module('i18n').filter('translate', translateFilter);

    translateFilter.$inject = ['i18nService'];

    function translateFilter(i18nService) {
        return function(input) {
            return i18nService.message(input);
        };
    }
})();