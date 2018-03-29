/**
 * valdr - v1.1.6 - 2016-09-14
 * https://github.com/netceteragroup/valdr
 * Copyright (c) 2016 Netcetera AG
 * License: MIT
 */
(function (window, document) {
'use strict';

angular.module('valdr', ['ng'])
  .constant('valdrEvents', {
    'revalidate': 'valdr-revalidate'
  })
  .value('valdrConfig', {
    addFormGroupClass: true
  })
  .value('valdrClasses', {
    formGroup: 'form-group',
    valid: 'ng-valid',
    invalid: 'ng-invalid',
    dirty: 'ng-dirty',
    pristine: 'ng-pristine',
    touched: 'ng-touched',
    untouched: 'ng-untouched',
    invalidDirtyTouchedGroup: 'valdr-invalid-dirty-touched-group'
  });
angular.module('valdr')

/**
 * Exposes utility functions used in validators and valdr core.
 */
  .factory('valdrUtil', [function () {

    var substringAfterDot = function (string) {
      if (string.lastIndexOf('.') === -1) {
        return string;
      } else {
        return string.substring(string.lastIndexOf('.') + 1, string.length);
      }
    };

    var SLUG_CASE_REGEXP = /[A-Z]/g;
    var slugCase = function (string) {
      return string.replace(SLUG_CASE_REGEXP, function(letter, pos) {
        return (pos ? '-' : '') + letter.toLowerCase();
      });
    };

    /**
     * Converts the given validator name to a validation token. Uses the last part of the validator name after the
     * dot (if present) and converts camel case to slug case (fooBar -> foo-bar).
     * @param validatorName the validator name
     * @returns {string} the validation token
     */
    var validatorNameToToken = function (validatorName) {
      if (angular.isString(validatorName)) {
        var name = substringAfterDot(validatorName);
        name = slugCase(name);
        return 'valdr-' + name;
      } else {
        return validatorName;
      }
    };

    return {
      validatorNameToToken: validatorNameToToken,

      isNaN: function (value) {
        // `NaN` as a primitive is the only value that is not equal to itself
        // (perform the [[Class]] check first to avoid errors with some host objects in IE)
        return this.isNumber(value) && value !== +value;
      },

      isNumber: function (value) {
        var type = typeof value;
        return type === 'number' ||
          value && type === 'object' && Object.prototype.toString.call(value) === '[object Number]' || false;
      },

      has: function (object, key) {
        return object ? Object.prototype.hasOwnProperty.call(object, key) : false;
      },

      /**
       * @param value the value
       * @returns {boolean} true if the given value is not null, not undefined, not an empty string, NaN returns false
       */
      notEmpty: function (value) {
        if (this.isNaN(value)) {
          return false;
        }
        if (angular.isArray(value) && value.length === 0){
          return false;
        }
        return angular.isDefined(value) && value !== '' && value !== null;
      },

      /**
       * @param value the value to validate
       * @returns {boolean} true if the given value is null, undefined, an empty string, NaN returns false
       */
      isEmpty: function (value) {
        if (this.isNaN(value)) {
          return false;
        }
        return !this.notEmpty(value);
      },

      /**
       * Checks if a string value starts with a given prefix.
       *
       * @param value the value
       * @param prefix the prefix
       * @returns {boolean} true if the given value starts with the given prefix.
       */
      startsWith: function (value, prefix) {
        return angular.isString(value)  &&
          angular.isString(prefix) &&
          value.lastIndexOf(prefix, 0) === 0;
      }
    };
  }])
;

angular.module('valdr')

  .factory('valdrRequiredValidator', ['valdrUtil', function (valdrUtil) {
    return {
      name: 'required',

      /**
       * Checks if the value is not empty.
       *
       * @param value the value to validate
       * @returns {boolean} true if the value is not empty
       */
      validate: function (value) {
        return valdrUtil.notEmpty(value);
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrMinValidator', ['valdrUtil', function (valdrUtil) {

    return {
      name: 'min',

      /**
       * Checks if the value is a number and higher or equal as the value specified in the constraint.
       *
       * @param value the value to validate
       * @param constraint the validation constraint
       * @returns {boolean} true if valid
       */
      validate: function (value, constraint) {
        var minValue = Number(constraint.value),
          valueAsNumber = Number(value);


        if (valdrUtil.isNaN(value)) {
          return false;
        }

        return valdrUtil.isEmpty(value) || valueAsNumber >= minValue;
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrMaxValidator', ['valdrUtil', function (valdrUtil) {

    return {
      name: 'max',

      /**
       * Checks if the value is a number and lower or equal as the value specified in the constraint.
       *
       * @param value the value to validate
       * @param constraint the validation constraint
       * @returns {boolean} true if valid
       */
      validate: function (value, constraint) {
        var maxValue = Number(constraint.value),
          valueAsNumber = Number(value);

        if (valdrUtil.isNaN(value)) {
          return false;
        }

        return valdrUtil.isEmpty(value) || valueAsNumber <= maxValue;
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrSizeValidator', ['valdrUtil', function (valdrUtil) {
    return {
      name: 'size',

      /**
       * Checks if the values length is in the range specified by the constraints min and max properties.
       *
       * @param value the value to validate
       * @param constraint with optional values: min, max
       * @returns {boolean} true if valid
       */
      validate: function (value, constraint) {
        var minLength = constraint.min || 0,
          maxLength = constraint.max;

        value = value || '';

        if (valdrUtil.isEmpty(value)) {
          return true;
        }

        return value.length >= minLength &&
          (maxLength === undefined || value.length <= maxLength);
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrEmailValidator', ['valdrUtil', function (valdrUtil) {

    // the e-mail pattern used in angular.js
    var EMAIL_REGEXP = /^[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$/i;

    return {
      name: 'email',

      /**
       * Checks if the value is a valid email address.
       *
       * @param value the value to validate
       * @returns {boolean} true if valid
       */
      validate: function (value) {
        return valdrUtil.isEmpty(value) || EMAIL_REGEXP.test(value);
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrUrlValidator', ['valdrUtil', function (valdrUtil) {

    // the url pattern used in angular.js
    var URL_REGEXP = /^(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?$/;

    return {
      name: 'url',

      /**
       * Checks if the value is a valid url.
       *
       * @param value the value to validate
       * @returns {boolean} true if valid
       */
      validate: function (value) {
        return valdrUtil.isEmpty(value) || URL_REGEXP.test(value);
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrDigitsValidator', ['valdrUtil', function (valdrUtil) {

    // matches everything except digits and '.' as decimal separator
    var regexp = new RegExp('[^.\\d]', 'g');

    /**
     * By converting to number and back to string using toString(), we make sure that '.' is used as decimal separator
     * and not the locale specific decimal separator.
     * As we already checked for NaN at this point, we can do this safely.
     */
    var toStringWithoutThousandSeparators = function (value) {
      return Number(value).toString().replace(regexp, '');
    };

    var isNotLongerThan = function (valueAsString, maxLengthConstraint) {
      return !valueAsString ? true : valueAsString.length <= maxLengthConstraint;
    };

    var doValidate = function (value, constraint) {
      var integerConstraint = constraint.integer,
        fractionConstraint = constraint.fraction,
        cleanValueAsString, integerAndFraction;

      cleanValueAsString = toStringWithoutThousandSeparators(value);
      integerAndFraction = cleanValueAsString.split('.');

      return isNotLongerThan(integerAndFraction[0], integerConstraint) &&
        isNotLongerThan(integerAndFraction[1], fractionConstraint);
    };

    return {
      name: 'digits',

      /**
       * Checks if the value is a number within accepted range.
       *
       * @param value the value to validate
       * @param constraint the validation constraint, it is expected to have integer and fraction properties (maximum
       *                   number of integral/fractional digits accepted for this number)
       * @returns {boolean} true if valid
       */
      validate: function (value, constraint) {

        if (valdrUtil.isEmpty(value)) {
          return true;
        }
        if (valdrUtil.isNaN(Number(value))) {
          return false;
        }

        return doValidate(value, constraint);
      }
    };
  }]);

angular.module('valdr')

  .factory('futureAndPastSharedValidator', ['valdrUtil', function (valdrUtil) {

    var someAlternativeDateFormats = ['D-M-YYYY', 'D.M.YYYY', 'D/M/YYYY', 'D. M. YYYY', 'YYYY.M.D'];

    return {
      validate: function (value, comparison) {
        var now = moment(), valueAsMoment;

        if (valdrUtil.isEmpty(value)) {
          return true;
        }

        valueAsMoment = moment(value);

        for (var i = 0; i < someAlternativeDateFormats.length && !valueAsMoment.isValid(); i++) {
          valueAsMoment = moment(value, someAlternativeDateFormats[i], true);
        }

        return valueAsMoment.isValid() && comparison(valueAsMoment, now);
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrPastValidator', ['futureAndPastSharedValidator', function (futureAndPastSharedValidator) {

    return {
      name: 'past',

      /**
       * Checks if the value is a date in the past.
       *
       * @param value the value to validate
       * @returns {boolean} true if empty, null, undefined or a date in the past, false otherwise
       */
      validate: function (value) {
        return futureAndPastSharedValidator.validate(value, function (valueAsMoment, now) {
          return valueAsMoment.isBefore(now);
        });
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrFutureValidator', ['futureAndPastSharedValidator', function (futureAndPastSharedValidator) {

    return {
      name: 'future',

      /**
       * Checks if the value is a date in the future.
       *
       * @param value the value to validate
       * @returns {boolean} true if empty, null, undefined or a date in the future, false otherwise
       */
      validate: function (value) {

        return futureAndPastSharedValidator.validate(value, function (valueAsMoment, now) {
          return valueAsMoment.isAfter(now);
        });
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrPatternValidator', ['valdrUtil', function (valdrUtil) {

    var REGEXP_PATTERN = /^\/(.*)\/([gim]*)$/;

    /**
     * Converts the given pattern to a RegExp.
     * The pattern can either be a RegExp object or a string containing a regular expression (`/regexp/`).
     * This implementation is based on the AngularJS ngPattern validator.
     * @param pattern the pattern
     * @returns {RegExp} the RegExp
     */
    var asRegExp = function (pattern) {
      var match;

      if (pattern.test) {
        return pattern;
      } else {
        match = pattern.match(REGEXP_PATTERN);
        if (match) {
          return new RegExp(match[1], match[2]);
        } else {
          throw ('Expected ' + pattern + ' to be a RegExp');
        }
      }
    };

    return {
      name: 'pattern',

      /**
       * Checks if the value matches the pattern defined in the constraint.
       *
       * @param value the value to validate
       * @param constraint the constraint with the regexp as value
       * @returns {boolean} true if valid
       */
      validate: function (value, constraint) {
        var pattern = asRegExp(constraint.value);
        return valdrUtil.isEmpty(value) || pattern.test(value);
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrMinLengthValidator', ['valdrUtil', function (valdrUtil) {
    return {
      name: 'minLength',

      /**
       * Checks if the value is a string and if it's at least 'constraint.number' of characters long.
       *
       * @param value the value to validate
       * @param constraint with property 'number'
       * @returns {boolean} true if valid
       */
      validate: function (value, constraint) {
        var minLength = constraint.number;

        if (valdrUtil.isEmpty(value)) {
          return true;
        }

        if (typeof value === 'string') {
          return value.length >= minLength;
        } else {
          return false;
        }
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrMaxLengthValidator', ['valdrUtil', function (valdrUtil) {
    return {
      name: 'maxLength',

      /**
       * Checks if the value is a string and if it's at most 'constraint.number' of characters long.
       *
       * @param value the value to validate
       * @param constraint with property 'number'
       * @returns {boolean} true if valid
       */
      validate: function (value, constraint) {
        var maxLength = constraint.number;

        if (valdrUtil.isEmpty(value)) {
          return true;
        }

        if (typeof value === 'string') {
          return value.length <= maxLength;
        } else {
          return false;
        }
      }
    };
  }]);

angular.module('valdr')

  .factory('valdrHibernateEmailValidator', ['valdrUtil', function (valdrUtil) {
    var ATOM = '[a-z0-9!#$%&\'*+/=?^_`{|}~-]';
    var DOMAIN = '^' + ATOM + '+(\\.' + ATOM + '+)*$';
    var IP_DOMAIN = '^\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\]$';

    var localPattern = new RegExp('^' + ATOM + '+(\\.' + ATOM + '+)*$', 'i');
    var domainPattern = new RegExp(DOMAIN + '|' + IP_DOMAIN, 'i');

    return {
      name: 'hibernateEmail',

      /**
       * Checks if the value is a valid email address using the same patterns as Hibernate uses in its bean validation
       * implementation.
       *
       * @param value the value to validate
       * @returns {boolean} true if valid
       */
      validate: function (value) {
        if (valdrUtil.isEmpty(value)) {
          return true;
        }

        // split email at '@' and consider local and domain part separately
        var emailParts = value.split('@');
        if (emailParts.length !== 2) {
          return false;
        }

        if (!localPattern.test(emailParts[0])) {
          return false;
        }

        return domainPattern.test(emailParts[1]);
      }
    };
  }]);

angular.module('valdr')

  .provider('valdr', function () {

    var constraints = {}, validators = {}, constraintUrl, constraintsLoading, constraintAliases = {},
      validatorNames = [
        'valdrRequiredValidator',
        'valdrSizeValidator',
        'valdrMinLengthValidator',
        'valdrMaxLengthValidator',
        'valdrMinValidator',
        'valdrMaxValidator',
        'valdrEmailValidator',
        'valdrUrlValidator',
        'valdrDigitsValidator',
        'valdrFutureValidator',
        'valdrPastValidator',
        'valdrPatternValidator',
        'valdrHibernateEmailValidator'
      ];

    var addConstraints = function (newConstraints) {
      angular.extend(constraints, newConstraints);
    };

    this.addConstraints = addConstraints;

    var removeConstraints = function (constraintNames) {
      if (angular.isArray(constraintNames)) {
        angular.forEach(constraintNames, function (name) {
          delete constraints[name];
        });
      } else if (angular.isString(constraintNames)) {
        delete constraints[constraintNames];
      }
    };

    this.removeConstraints = removeConstraints;

    this.setConstraintUrl = function (url) {
      constraintUrl = url;
    };

    this.addValidator = function (validatorName) {
      validatorNames.push(validatorName);
    };

    this.addConstraintAlias = function (valdrName, alias) {
      if(!angular.isArray(constraintAliases[valdrName])) {
        constraintAliases[valdrName] = [];
      }
      constraintAliases[valdrName].push(alias);
    };

    this.$get =
      ['$log', '$injector', '$rootScope', '$http', 'valdrEvents', 'valdrUtil', 'valdrClasses',
        function ($log, $injector, $rootScope, $http, valdrEvents, valdrUtil, valdrClasses) {

          // inject all validators
          angular.forEach(validatorNames, function (validatorName) {
            var validator = $injector.get(validatorName);
            validators[validator.name] = validator;

            // register validator with aliases
            if(angular.isArray(constraintAliases[validator.name])) {
              angular.forEach(constraintAliases[validator.name], function (alias) {
                validators[alias] = validator;
              });
            }

          });

          // load constraints via $http if constraintUrl is configured
          if (constraintUrl) {
            constraintsLoading = true;
            $http.get(constraintUrl).then(function (response) {
              constraintsLoading = false;
              addConstraints(response.data);
              $rootScope.$broadcast(valdrEvents.revalidate);
            })['finally'](function () {
              constraintsLoading = false;
            });
          }

          var constraintsForType = function (type) {
            if (valdrUtil.has(constraints, type)) {
              return constraints[type];
            } else if (!constraintsLoading) {
              $log.warn('No constraints for type \'' + type + '\' available.');
            }
          };

          return {
            /**
             * Validates the value of the given type with the constraints for the given field name.
             *
             * @param typeName the type name
             * @param fieldName the field name
             * @param value the value to validate
             * @returns {*}
             */
            validate: function (typeName, fieldName, value) {

              var validResult = { valid: true },
                typeConstraints = constraintsForType(typeName);

              if (valdrUtil.has(typeConstraints, fieldName)) {
                var fieldConstraints = typeConstraints[fieldName],
                  fieldIsValid = true,
                  validationResults = [],
                  violations = [];

                angular.forEach(fieldConstraints, function (constraint, validatorName) {
                  var validator = validators[validatorName];

                  if (angular.isUndefined(validator)) {
                    $log.warn('No validator defined for \'' + validatorName +
                      '\'. Can not validate field \'' + fieldName + '\'');
                    return validResult;
                  }

                  var valid = validator.validate(value, constraint);
                  var validationResult = {
                    valid: valid,
                    value: value,
                    field: fieldName,
                    type: typeName,
                    validator: validatorName
                  };
                  angular.extend(validationResult, constraint);

                  validationResults.push(validationResult);
                  if (!valid) {
                    violations.push(validationResult);
                  }
                  fieldIsValid = fieldIsValid && valid;
                });

                return {
                  valid: fieldIsValid,
                  violations: violations.length === 0 ? undefined : violations,
                  validationResults: validationResults.length === 0 ? undefined : validationResults
                };
              } else {
                return validResult;
              }
            },
            addConstraints: function (newConstraints) {
              addConstraints(newConstraints);
              $rootScope.$broadcast(valdrEvents.revalidate);
            },
            removeConstraints: function (constraintNames) {
              removeConstraints(constraintNames);
              $rootScope.$broadcast(valdrEvents.revalidate);
            },
            getConstraints: function () {
              return constraints;
            },
            setClasses: function (newClasses) {
              angular.extend(valdrClasses, newClasses);
              $rootScope.$broadcast(valdrEvents.revalidate);
            }
          };
        }];
  });
/**
 * This directive adds the validity state to a form group element surrounding valdr validated input fields.
 * If valdr-messages is loaded, it also adds the validation messages as last element to the element this this
 * directive is applied on.
 */
var valdrFormGroupDirectiveDefinition =
  ['valdrClasses', 'valdrConfig', function (valdrClasses, valdrConfig) {
    return  {
      restrict: 'EA',
      link: function (scope, element) {
        if (valdrConfig.addFormGroupClass) {
          element.addClass(valdrClasses.formGroup);
        }
      },
      controller: ['$scope', '$element', function ($scope, $element) {

        var formItems = [],
          messageElements = {};

        /**
         * Checks the state of all valdr validated form items below this element.
         * @returns {Object} an object containing the states of all form items in this form group
         */
        var getFormGroupState = function () {

          var formGroupState = {
            // true if an item in this form group is currently dirty, touched and invalid
            invalidDirtyTouchedGroup: false,
            // true if all form items in this group are currently valid
            valid: true,
            // contains the validity states of all form items in this group
            itemStates: []
          };

          angular.forEach(formItems, function (formItem) {
            if (formItem.$touched && formItem.$dirty && formItem.$invalid) {
              formGroupState.invalidDirtyTouchedGroup = true;
            }

            if (formItem.$invalid) {
              formGroupState.valid = false;
            }

            var itemState = {
              name: formItem.$name,
              touched: formItem.$touched,
              dirty: formItem.$dirty,
              valid: formItem.$valid
            };

            formGroupState.itemStates.push(itemState);
          });

          return formGroupState;
        };

        /**
         * Updates the classes on this element and the valdr message elements based on the validity states
         * of the items in this form group.
         * @param formGroupState the current state of this form group and its items
         */
        var updateClasses = function (formGroupState) {
          // form group state
          $element.toggleClass(valdrClasses.invalidDirtyTouchedGroup, formGroupState.invalidDirtyTouchedGroup);
          $element.toggleClass(valdrClasses.valid, formGroupState.valid);
          $element.toggleClass(valdrClasses.invalid, !formGroupState.valid);

          // valdr message states
          angular.forEach(formGroupState.itemStates, function (itemState) {
            var messageElement = messageElements[itemState.name];
            if (messageElement) {
              messageElement.toggleClass(valdrClasses.valid, itemState.valid);
              messageElement.toggleClass(valdrClasses.invalid, !itemState.valid);
              messageElement.toggleClass(valdrClasses.dirty, itemState.dirty);
              messageElement.toggleClass(valdrClasses.pristine, !itemState.dirty);
              messageElement.toggleClass(valdrClasses.touched, itemState.touched);
              messageElement.toggleClass(valdrClasses.untouched, !itemState.touched);
            }
          });
        };

        $scope.$watch(getFormGroupState, updateClasses, true);

        this.addFormItem = function (ngModelController) {
          formItems.push(ngModelController);
        };

        this.removeFormItem = function (ngModelController) {
          var index = formItems.indexOf(ngModelController);
          if (index >= 0) {
            formItems.splice(index, 1);
          }
        };

        this.addMessageElement = function (ngModelController, messageElement) {
          $element.append(messageElement);
          messageElements[ngModelController.$name] = messageElement;
        };

        this.removeMessageElement = function (ngModelController) {
          if (messageElements[ngModelController.$name]) {
            messageElements[ngModelController.$name].remove();
            delete messageElements[ngModelController.$name];
          }
        };

      }]
    };
  }];

angular.module('valdr')
  .directive('valdrFormGroup', valdrFormGroupDirectiveDefinition);

angular.module('valdr')

/**
 * The valdrType directive defines the type of the model to be validated.
 * The directive exposes the type through the controller to allow access to it by wrapped directives.
 */
  .directive('valdrType', function () {
    return  {
      priority: 1,
      controller: ['$attrs', function ($attrs) {

        this.getType = function () {
          return $attrs.valdrType;
        };

      }]
    };
  });

/**
 * This controller is used if no valdrEnabled parent directive is available.
 */
var nullValdrEnabledController = {
  isEnabled: function () {
    return true;
  }
};

/**
 * This controller is used if no valdrFormGroup parent directive is available.
 */
var nullValdrFormGroupController = {
  addFormItem: angular.noop,
  removeFormItem: angular.noop
};

/**
 * This directive adds validation to all input and select fields as well as to explicitly enabled elements which are
 * bound to an ngModel and are surrounded by a valdrType directive. To prevent adding validation to specific fields,
 * the attribute 'valdr-no-validate' can be added to those fields.
 */
var valdrFormItemDirectiveDefinitionFactory = function (restrict) {
    return ['valdrEvents', 'valdr', 'valdrUtil', function (valdrEvents, valdr, valdrUtil) {
      return {
        restrict: restrict,
        require: ['?^valdrType', '?^ngModel', '?^valdrFormGroup', '?^valdrEnabled'],
        link: function (scope, element, attrs, controllers) {

          var valdrTypeController = controllers[0],
            ngModelController = controllers[1],
            valdrFormGroupController = controllers[2] || nullValdrFormGroupController,
            valdrEnabled = controllers[3] || nullValdrEnabledController,
            valdrNoValidate = attrs.valdrNoValidate,
            fieldName = attrs.name;

          /**
           * Don't do anything if
           * - this is an <input> that's not inside of a valdr-type block
           * - there is no ng-model bound to input
           * - there is the 'valdr-no-validate' attribute present
           */
          if (!valdrTypeController || !ngModelController || angular.isDefined(valdrNoValidate)) {
            return;
          }

          valdrFormGroupController.addFormItem(ngModelController);

          if (valdrUtil.isEmpty(fieldName) && valdrEnabled.isEnabled()) {
            console.warn('Form element with ID "' + attrs.id + '" is not bound to a field name.');
          }

          var updateNgModelController = function (validationResult) {

            if (valdrEnabled.isEnabled()) {
              var validatorTokens = ['valdr'];

              // set validity state for individual valdr validators
              angular.forEach(validationResult.validationResults, function (result) {
                var validatorToken = valdrUtil.validatorNameToToken(result.validator);
                ngModelController.$setValidity(validatorToken, result.valid);
                validatorTokens.push(validatorToken);
              });

              // set overall validity state of this form item
              ngModelController.$setValidity('valdr', validationResult.valid);
              ngModelController.valdrViolations = validationResult.violations;

              // remove errors for valdr validators which no longer exist
              angular.forEach(ngModelController.$error, function (value, validatorToken) {
                if (validatorTokens.indexOf(validatorToken) === -1 && valdrUtil.startsWith(validatorToken, 'valdr')) {
                  ngModelController.$setValidity(validatorToken, true);
                }
              });
            } else {
              angular.forEach(ngModelController.$error, function (value, validatorToken) {
                if (valdrUtil.startsWith(validatorToken, 'valdr')) {
                  ngModelController.$setValidity(validatorToken, true);
                }
              });
              ngModelController.valdrViolations = undefined;
            }
          };

          var validate = function (modelValue) {
            var validationResult = valdr.validate(valdrTypeController.getType(), fieldName, modelValue);
            updateNgModelController(validationResult);
            return valdrEnabled.isEnabled() ? validationResult.valid : true;
          };

          ngModelController.$validators.valdr = validate;

          scope.$on(valdrEvents.revalidate, function () {
            validate(ngModelController.$modelValue);
          });

          scope.$on('$destroy', function () {
            valdrFormGroupController.removeFormItem(ngModelController);
          });

        }
      };
    }];
  },
  valdrFormItemElementDirectiveDefinition = valdrFormItemDirectiveDefinitionFactory('E'),
  valdrFormItemAttributeDirectiveDefinition = valdrFormItemDirectiveDefinitionFactory('A');

angular.module('valdr')
  .directive('input', valdrFormItemElementDirectiveDefinition)
  .directive('select', valdrFormItemElementDirectiveDefinition)
  .directive('textarea', valdrFormItemElementDirectiveDefinition)
  .directive('enableValdrValidation', valdrFormItemAttributeDirectiveDefinition);

angular.module('valdr')

/**
 * This directive allows to dynamically enable and disable the validation with valdr.
 * All form elements in a child node of an element with the 'valdr-enabled' directive will be affected by this.
 *
 * Usage:
 *
 * <div valdr-enabled="isValidationEnabled()">
 *   <input type="text" name="name" ng-model="mymodel.field">
 * </div>
 *
 * If multiple valdr-enabled directives are nested, the one nearest to the validated form element
 * will take precedence.
 */
  .directive('valdrEnabled', ['valdrEvents', function (valdrEvents) {
    return  {
      controller: ['$scope', '$attrs', function($scope, $attrs) {
        $scope.$watch($attrs.valdrEnabled, function () {
          $scope.$broadcast(valdrEvents.revalidate);
        });

        this.isEnabled = function () {
          var evaluatedExpression = $scope.$eval($attrs.valdrEnabled);
          return evaluatedExpression === undefined ? true : evaluatedExpression;
        };
      }]
    };
  }]);

})(window, document);