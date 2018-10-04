var SubscriptionsTransportWs =
  /******/ (function(modules) { // webpackBootstrap
  /******/ 	// The module cache
  /******/
  var installedModules = {};
  /******/
  /******/ 	// The require function
  /******/
  function __webpack_require__(moduleId) {
    /******/
    /******/ 		// Check if module is in cache
    /******/
    if (installedModules[ moduleId ]) {
      /******/
      return installedModules[ moduleId ].exports;
      /******/
    }
    /******/ 		// Create a new module (and put it into the cache)
    /******/
    var module = installedModules[ moduleId ] = {
      /******/      i:       moduleId,
      /******/      l:       false,
      /******/      exports: {}
      /******/
    };
    /******/
    /******/ 		// Execute the module function
    /******/
    modules[ moduleId ].call(module.exports, module, module.exports, __webpack_require__);
    /******/
    /******/ 		// Flag the module as loaded
    /******/
    module.l = true;
    /******/
    /******/ 		// Return the exports of the module
    /******/
    return module.exports;
    /******/
  }

  /******/
  /******/
  /******/ 	// expose the modules object (__webpack_modules__)
  /******/
  __webpack_require__.m = modules;
  /******/
  /******/ 	// expose the module cache
  /******/
  __webpack_require__.c = installedModules;
  /******/
  /******/ 	// define getter function for harmony exports
  /******/
  __webpack_require__.d = function(exports, name, getter) {
    /******/
    if (!__webpack_require__.o(exports, name)) {
      /******/
      Object.defineProperty(exports, name, {
        /******/        configurable: false,
        /******/        enumerable:   true,
        /******/        get:          getter
        /******/
      });
      /******/
    }
    /******/
  };
  /******/
  /******/ 	// getDefaultExport function for compatibility with non-harmony modules
  /******/
  __webpack_require__.n = function(module) {
    /******/
    var getter = module && module.__esModule ?
      /******/      function getDefault() {
        return module[ 'default' ];
      } :
      /******/      function getModuleExports() {
        return module;
      };
    /******/
    __webpack_require__.d(getter, 'a', getter);
    /******/
    return getter;
    /******/
  };
  /******/
  /******/ 	// Object.prototype.hasOwnProperty.call
  /******/
  __webpack_require__.o = function(object, property) {
    return Object.prototype.hasOwnProperty.call(object, property);
  };
  /******/
  /******/ 	// __webpack_public_path__
  /******/
  __webpack_require__.p = "";
  /******/
  /******/ 	// Load entry module and return exports
  /******/
  return __webpack_require__(__webpack_require__.s = 2);
  /******/
})
/************************************************************************/
/******/([
  /* 0 */
  /***/ (function(module, exports) {

    var g;

// This works in non-strict mode
    g = (function() {
      return this;
    })();

    try {
      // This works if eval is allowed (see CSP)
      g = g || Function("return this")() || (1, eval)("this");
    } catch (e) {
      // This works if the window reference is available
      if (typeof window === "object")
        g = window;
    }

// g can still be undefined, but nothing to do about it...
// We return undefined, instead of nothing here, so it's
// easier to handle this case. if(!global) { ...}

    module.exports = g;

    /***/
  }),
  /* 1 */
  /***/ (function(module, exports) {

// shim for using process in browser
    var process = module.exports = {};

// cached from whatever global is present so that test runners that stub it
// don't break things.  But we need to wrap it in a try catch in case it is
// wrapped in strict mode code which doesn't define any globals.  It's inside a
// function because try/catches deoptimize in certain engines.

    var cachedSetTimeout;
    var cachedClearTimeout;

    function defaultSetTimout() {
      throw new Error('setTimeout has not been defined');
    }

    function defaultClearTimeout() {
      throw new Error('clearTimeout has not been defined');
    }

    (function() {
      try {
        if (typeof setTimeout === 'function') {
          cachedSetTimeout = setTimeout;
        } else {
          cachedSetTimeout = defaultSetTimout;
        }
      } catch (e) {
        cachedSetTimeout = defaultSetTimout;
      }
      try {
        if (typeof clearTimeout === 'function') {
          cachedClearTimeout = clearTimeout;
        } else {
          cachedClearTimeout = defaultClearTimeout;
        }
      } catch (e) {
        cachedClearTimeout = defaultClearTimeout;
      }
    }())

    function runTimeout(fun) {
      if (cachedSetTimeout === setTimeout) {
        //normal enviroments in sane situations
        return setTimeout(fun, 0);
      }
      // if setTimeout wasn't available but was latter defined
      if ((cachedSetTimeout === defaultSetTimout || !cachedSetTimeout) && setTimeout) {
        cachedSetTimeout = setTimeout;
        return setTimeout(fun, 0);
      }
      try {
        // when when somebody has screwed with setTimeout but no I.E. maddness
        return cachedSetTimeout(fun, 0);
      } catch (e) {
        try {
          // When we are in I.E. but the script has been evaled so I.E. doesn't trust the global object when called normally
          return cachedSetTimeout.call(null, fun, 0);
        } catch (e) {
          // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error
          return cachedSetTimeout.call(this, fun, 0);
        }
      }

    }

    function runClearTimeout(marker) {
      if (cachedClearTimeout === clearTimeout) {
        //normal enviroments in sane situations
        return clearTimeout(marker);
      }
      // if clearTimeout wasn't available but was latter defined
      if ((cachedClearTimeout === defaultClearTimeout || !cachedClearTimeout) && clearTimeout) {
        cachedClearTimeout = clearTimeout;
        return clearTimeout(marker);
      }
      try {
        // when when somebody has screwed with setTimeout but no I.E. maddness
        return cachedClearTimeout(marker);
      } catch (e) {
        try {
          // When we are in I.E. but the script has been evaled so I.E. doesn't  trust the global object when called normally
          return cachedClearTimeout.call(null, marker);
        } catch (e) {
          // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error.
          // Some versions of I.E. have different rules for clearTimeout vs setTimeout
          return cachedClearTimeout.call(this, marker);
        }
      }

    }

    var queue = [];
    var draining = false;
    var currentQueue;
    var queueIndex = -1;

    function cleanUpNextTick() {
      if (!draining || !currentQueue) {
        return;
      }
      draining = false;
      if (currentQueue.length) {
        queue = currentQueue.concat(queue);
      } else {
        queueIndex = -1;
      }
      if (queue.length) {
        drainQueue();
      }
    }

    function drainQueue() {
      if (draining) {
        return;
      }
      var timeout = runTimeout(cleanUpNextTick);
      draining = true;

      var len = queue.length;
      while (len) {
        currentQueue = queue;
        queue = [];
        while (++queueIndex < len) {
          if (currentQueue) {
            currentQueue[ queueIndex ].run();
          }
        }
        queueIndex = -1;
        len = queue.length;
      }
      currentQueue = null;
      draining = false;
      runClearTimeout(timeout);
    }

    process.nextTick = function(fun) {
      var args = new Array(arguments.length - 1);
      if (arguments.length > 1) {
        for (var i = 1; i < arguments.length; i++) {
          args[ i - 1 ] = arguments[ i ];
        }
      }
      queue.push(new Item(fun, args));
      if (queue.length === 1 && !draining) {
        runTimeout(drainQueue);
      }
    };

// v8 likes predictible objects
    function Item(fun, array) {
      this.fun = fun;
      this.array = array;
    }

    Item.prototype.run = function() {
      this.fun.apply(null, this.array);
    };
    process.title = 'browser';
    process.browser = true;
    process.env = {};
    process.argv = [];
    process.version = ''; // empty string to avoid regexp issues
    process.versions = {};

    function noop() {
    }

    process.on = noop;
    process.addListener = noop;
    process.once = noop;
    process.off = noop;
    process.removeListener = noop;
    process.removeAllListeners = noop;
    process.emit = noop;
    process.prependListener = noop;
    process.prependOnceListener = noop;

    process.listeners = function(name) {
      return []
    }

    process.binding = function(name) {
      throw new Error('process.binding is not supported');
    };

    process.cwd = function() {
      return '/'
    };
    process.chdir = function(dir) {
      throw new Error('process.chdir is not supported');
    };
    process.umask = function() {
      return 0;
    };

    /***/
  }),
  /* 2 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";
    /* WEBPACK VAR INJECTION */
    (function(global, process) {
      var __assign = (this && this.__assign) || Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
          s = arguments[ i ];
          for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
            t[ p ] = s[ p ];
        }
        return t;
      };

      function __export(m) {
        for (var p in m) if (!exports.hasOwnProperty(p)) exports[ p ] = m[ p ];
      }

      Object.defineProperty(exports, "__esModule", { value: true });
      var _global = typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : {});
      var NativeWebSocket = _global.WebSocket || _global.MozWebSocket;
      var Backoff = __webpack_require__(3);
      var eventemitter3_1 = __webpack_require__(4);
      var isString = __webpack_require__(5);
      var isObject = __webpack_require__(6);
      var printer_1 = __webpack_require__(7);
      var getOperationAST_1 = __webpack_require__(9);
      var symbol_observable_1 = __webpack_require__(11);
      var protocol_1 = __webpack_require__(15);
      var defaults_1 = __webpack_require__(16);
      var message_types_1 = __webpack_require__(17);
      __export(__webpack_require__(18));
      var SubscriptionClient = (function() {
        function SubscriptionClient(url, options, webSocketImpl) {
          var _a = (options || {}), _b = _a.connectionCallback, connectionCallback = _b === void 0 ? undefined : _b,
            _c = _a.connectionParams, connectionParams = _c === void 0 ? {} : _c, _d = _a.timeout,
            timeout = _d === void 0 ? defaults_1.WS_TIMEOUT : _d, _e = _a.reconnect,
            reconnect = _e === void 0 ? false : _e, _f = _a.reconnectionAttempts,
            reconnectionAttempts = _f === void 0 ? Infinity : _f, _g = _a.lazy, lazy = _g === void 0 ? false : _g;
          this.wsImpl = webSocketImpl || NativeWebSocket;
          if (!this.wsImpl) {
            throw new Error('Unable to find native implementation, or alternative implementation for WebSocket!');
          }
          this.connectionParams = connectionParams;
          this.connectionCallback = connectionCallback;
          this.url = url;
          this.operations = {};
          this.nextOperationId = 0;
          this.wsTimeout = timeout;
          this.unsentMessagesQueue = [];
          this.reconnect = reconnect;
          this.reconnecting = false;
          this.reconnectionAttempts = reconnectionAttempts;
          this.lazy = !!lazy;
          this.closedByUser = false;
          this.backoff = new Backoff({ jitter: 0.5 });
          this.eventEmitter = new eventemitter3_1.EventEmitter();
          this.middlewares = [];
          this.client = null;
          this.maxConnectTimeGenerator = this.createMaxConnectTimeGenerator();
          if (!this.lazy) {
            this.connect();
          }
        }

        Object.defineProperty(SubscriptionClient.prototype, "status", {
          get:          function() {
            if (this.client === null) {
              return this.wsImpl.CLOSED;
            }
            return this.client.readyState;
          },
          enumerable:   true,
          configurable: true
        });
        SubscriptionClient.prototype.close = function(isForced, closedByUser) {
          if (isForced === void 0) {
            isForced = true;
          }
          if (closedByUser === void 0) {
            closedByUser = true;
          }
          if (this.client !== null) {
            this.closedByUser = closedByUser;
            if (isForced) {
              this.clearCheckConnectionInterval();
              this.clearMaxConnectTimeout();
              this.clearTryReconnectTimeout();
              this.unsubscribeAll();
              this.sendMessage(undefined, message_types_1.default.GQL_CONNECTION_TERMINATE, null);
            }
            this.client.close();
            this.client = null;
            this.eventEmitter.emit('disconnected');
            if (!isForced) {
              this.tryReconnect();
            }
          }
        };
        SubscriptionClient.prototype.request = function(request) {
          var getObserver = this.getObserver.bind(this);
          var executeOperation = this.executeOperation.bind(this);
          var unsubscribe = this.unsubscribe.bind(this);
          var opId;
          return _a = {},
            _a[ symbol_observable_1.default ] = function() {
              return this;
            },
            _a.subscribe = function(observerOrNext, onError, onComplete) {
              var observer = getObserver(observerOrNext, onError, onComplete);
              opId = executeOperation({
                query:         request.query,
                variables:     request.variables,
                operationName: request.operationName,
              }, function(error, result) {
                if (error === null && result === null) {
                  if (observer.complete) {
                    observer.complete();
                  }
                }
                else if (error) {
                  if (observer.error) {
                    observer.error(error[ 0 ]);
                  }
                }
                else {
                  if (observer.next) {
                    observer.next(result);
                  }
                }
              });
              return {
                unsubscribe: function() {
                  if (opId) {
                    unsubscribe(opId);
                    opId = null;
                  }
                },
              };
            },
            _a;
          var _a;
        };
        SubscriptionClient.prototype.query = function(options) {
          var _this = this;
          return new Promise(function(resolve, reject) {
            var handler = function(error, result) {
              if (result) {
                resolve(result);
              }
              else {
                reject(error);
              }
            };
            _this.executeOperation(options, handler);
          });
        };
        SubscriptionClient.prototype.subscribe = function(options, handler) {
          var legacyHandler = function(error, result) {
            var operationPayloadData = result && result.data || null;
            var operationPayloadErrors = result && result.errors || null;
            if (error) {
              operationPayloadErrors = error;
              operationPayloadData = null;
            }
            if (error !== null || result !== null) {
              handler(operationPayloadErrors, operationPayloadData);
            }
          };
          if (this.client === null) {
            this.connect();
          }
          if (!handler) {
            throw new Error('Must provide an handler.');
          }
          return this.executeOperation(options, legacyHandler);
        };
        SubscriptionClient.prototype.on = function(eventName, callback, context) {
          var handler = this.eventEmitter.on(eventName, callback, context);
          return function() {
            handler.off(eventName, callback, context);
          };
        };
        SubscriptionClient.prototype.onConnect = function(callback, context) {
          this.logWarningOnNonProductionEnv('This method will become deprecated in the next release. ' +
            'You can use onConnecting and onConnected instead.');
          return this.onConnecting(callback, context);
        };
        SubscriptionClient.prototype.onDisconnect = function(callback, context) {
          this.logWarningOnNonProductionEnv('This method will become deprecated in the next release. ' +
            'You can use onDisconnected instead.');
          return this.onDisconnected(callback, context);
        };
        SubscriptionClient.prototype.onReconnect = function(callback, context) {
          this.logWarningOnNonProductionEnv('This method will become deprecated in the next release. ' +
            'You can use onReconnecting and onReconnected instead.');
          return this.onReconnecting(callback, context);
        };
        SubscriptionClient.prototype.onConnected = function(callback, context) {
          return this.on('connected', callback, context);
        };
        SubscriptionClient.prototype.onConnecting = function(callback, context) {
          return this.on('connecting', callback, context);
        };
        SubscriptionClient.prototype.onDisconnected = function(callback, context) {
          return this.on('disconnected', callback, context);
        };
        SubscriptionClient.prototype.onReconnected = function(callback, context) {
          return this.on('reconnected', callback, context);
        };
        SubscriptionClient.prototype.onReconnecting = function(callback, context) {
          return this.on('reconnecting', callback, context);
        };
        SubscriptionClient.prototype.unsubscribe = function(opId) {
          if (this.operations[ opId ]) {
            delete this.operations[ opId ];
            this.sendMessage(opId, message_types_1.default.GQL_STOP, undefined);
          }
        };
        SubscriptionClient.prototype.unsubscribeAll = function() {
          var _this = this;
          Object.keys(this.operations).forEach(function(subId) {
            _this.unsubscribe(subId);
          });
        };
        SubscriptionClient.prototype.applyMiddlewares = function(options) {
          var _this = this;
          return new Promise(function(resolve, reject) {
            var queue = function(funcs, scope) {
              var next = function(error) {
                if (error) {
                  reject(error);
                }
                else {
                  if (funcs.length > 0) {
                    var f = funcs.shift();
                    if (f) {
                      f.applyMiddleware.apply(scope, [ options, next ]);
                    }
                  }
                  else {
                    resolve(options);
                  }
                }
              };
              next();
            };
            queue(_this.middlewares.slice(), _this);
          });
        };
        SubscriptionClient.prototype.use = function(middlewares) {
          var _this = this;
          middlewares.map(function(middleware) {
            if (typeof middleware.applyMiddleware === 'function') {
              _this.middlewares.push(middleware);
            }
            else {
              throw new Error('Middleware must implement the applyMiddleware function.');
            }
          });
          return this;
        };
        SubscriptionClient.prototype.executeOperation = function(options, handler) {
          var _this = this;
          var opId = this.generateOperationId();
          this.operations[ opId ] = { options: options, handler: handler };
          this.applyMiddlewares(options)
            .then(function(processedOptions) {
              _this.checkOperationOptions(processedOptions, handler);
              if (_this.operations[ opId ]) {
                _this.operations[ opId ] = { options: processedOptions, handler: handler };
                _this.sendMessage(opId, message_types_1.default.GQL_START, processedOptions);
              }
            })
            .catch(function(error) {
              _this.unsubscribe(opId);
              handler(_this.formatErrors(error));
            });
          return opId;
        };
        SubscriptionClient.prototype.getObserver = function(observerOrNext, error, complete) {
          if (typeof observerOrNext === 'function') {
            return {
              next:     function(v) {
                return observerOrNext(v);
              },
              error:    function(e) {
                return error && error(e);
              },
              complete: function() {
                return complete && complete();
              },
            };
          }
          return observerOrNext;
        };
        SubscriptionClient.prototype.createMaxConnectTimeGenerator = function() {
          var minValue = 1000;
          var maxValue = this.wsTimeout;
          return new Backoff({
            min:    minValue,
            max:    maxValue,
            factor: 1.2,
          });
        };
        SubscriptionClient.prototype.clearCheckConnectionInterval = function() {
          if (this.checkConnectionIntervalId) {
            clearInterval(this.checkConnectionIntervalId);
            this.checkConnectionIntervalId = null;
          }
        };
        SubscriptionClient.prototype.clearMaxConnectTimeout = function() {
          if (this.maxConnectTimeoutId) {
            clearTimeout(this.maxConnectTimeoutId);
            this.maxConnectTimeoutId = null;
          }
        };
        SubscriptionClient.prototype.clearTryReconnectTimeout = function() {
          if (this.tryReconnectTimeoutId) {
            clearTimeout(this.tryReconnectTimeoutId);
            this.tryReconnectTimeoutId = null;
          }
        };
        SubscriptionClient.prototype.logWarningOnNonProductionEnv = function(warning) {
          if (process && process.env && process.env.NODE_ENV !== 'production') {
            console.warn(warning);
          }
        };
        SubscriptionClient.prototype.checkOperationOptions = function(options, handler) {
          var query = options.query, variables = options.variables, operationName = options.operationName;
          if (!query) {
            throw new Error('Must provide a query.');
          }
          if (!handler) {
            throw new Error('Must provide an handler.');
          }
          if ((!isString(query) && !getOperationAST_1.getOperationAST(query, operationName)) ||
            (operationName && !isString(operationName)) ||
            (variables && !isObject(variables))) {
            throw new Error('Incorrect option types. query must be a string or a document,' +
              '`operationName` must be a string, and `variables` must be an object.');
          }
        };
        SubscriptionClient.prototype.buildMessage = function(id, type, payload) {
          var payloadToReturn = payload && payload.query ? __assign({}, payload, { query: typeof payload.query === 'string' ? payload.query : printer_1.print(payload.query) }) :
            payload;
          return {
            id:      id,
            type:    type,
            payload: payloadToReturn,
          };
        };
        SubscriptionClient.prototype.formatErrors = function(errors) {
          if (Array.isArray(errors)) {
            return errors;
          }
          if (errors && errors.errors) {
            return this.formatErrors(errors.errors);
          }
          if (errors && errors.message) {
            return [ errors ];
          }
          return [ {
            name:          'FormatedError',
            message:       'Unknown error',
            originalError: errors,
          } ];
        };
        SubscriptionClient.prototype.sendMessage = function(id, type, payload) {
          this.sendMessageRaw(this.buildMessage(id, type, payload));
        };
        SubscriptionClient.prototype.sendMessageRaw = function(message) {
          switch (this.status) {
            case this.wsImpl.OPEN:
              var serializedMessage = JSON.stringify(message);
              var parsedMessage = void 0;
              try {
                parsedMessage = JSON.parse(serializedMessage);
              }
              catch (e) {
                throw new Error("Message must be JSON-serializable. Got: " + message);
              }
              this.client.send(serializedMessage);
              break;
            case this.wsImpl.CONNECTING:
              this.unsentMessagesQueue.push(message);
              break;
            default:
              if (!this.reconnecting) {
                throw new Error('A message was not sent because socket is not connected, is closing or ' +
                  'is already closed. Message was: ' + JSON.stringify(message));
              }
          }
        };
        SubscriptionClient.prototype.generateOperationId = function() {
          return String(++this.nextOperationId);
        };
        SubscriptionClient.prototype.tryReconnect = function() {
          var _this = this;
          if (!this.reconnect || this.backoff.attempts >= this.reconnectionAttempts) {
            return;
          }
          if (!this.reconnecting) {
            Object.keys(this.operations).forEach(function(key) {
              _this.unsentMessagesQueue.push(_this.buildMessage(key, message_types_1.default.GQL_START, _this.operations[ key ].options));
            });
            this.reconnecting = true;
          }
          this.clearTryReconnectTimeout();
          var delay = this.backoff.duration();
          this.tryReconnectTimeoutId = setTimeout(function() {
            _this.connect();
          }, delay);
        };
        SubscriptionClient.prototype.flushUnsentMessagesQueue = function() {
          var _this = this;
          this.unsentMessagesQueue.forEach(function(message) {
            _this.sendMessageRaw(message);
          });
          this.unsentMessagesQueue = [];
        };
        SubscriptionClient.prototype.checkConnection = function() {
          if (this.wasKeepAliveReceived) {
            this.wasKeepAliveReceived = false;
            return;
          }
          if (!this.reconnecting) {
            this.close(false, true);
          }
        };
        SubscriptionClient.prototype.checkMaxConnectTimeout = function() {
          var _this = this;
          this.clearMaxConnectTimeout();
          this.maxConnectTimeoutId = setTimeout(function() {
            if (_this.status !== _this.wsImpl.OPEN) {
              _this.close(false, true);
            }
          }, this.maxConnectTimeGenerator.duration());
        };
        SubscriptionClient.prototype.connect = function() {
          var _this = this;
          this.client = new this.wsImpl(this.url, protocol_1.GRAPHQL_WS);
          this.checkMaxConnectTimeout();
          this.client.onopen = function() {
            _this.clearMaxConnectTimeout();
            _this.closedByUser = false;
            _this.eventEmitter.emit(_this.reconnecting ? 'reconnecting' : 'connecting');
            var payload = typeof _this.connectionParams === 'function' ? _this.connectionParams() : _this.connectionParams;
            _this.sendMessage(undefined, message_types_1.default.GQL_CONNECTION_INIT, payload);
            _this.flushUnsentMessagesQueue();
          };
          this.client.onclose = function() {
            if (!_this.closedByUser) {
              _this.close(false, false);
            }
          };
          this.client.onerror = function() {
          };
          this.client.onmessage = function(_a) {
            var data = _a.data;
            _this.processReceivedData(data);
          };
        };
        SubscriptionClient.prototype.processReceivedData = function(receivedData) {
          var parsedMessage;
          var opId;
          try {
            parsedMessage = JSON.parse(receivedData);
            opId = parsedMessage.id;
          }
          catch (e) {
            throw new Error("Message must be JSON-parseable. Got: " + receivedData);
          }
          if ([ message_types_1.default.GQL_DATA,
            message_types_1.default.GQL_COMPLETE,
            message_types_1.default.GQL_ERROR,
          ].indexOf(parsedMessage.type) !== -1 && !this.operations[ opId ]) {
            this.unsubscribe(opId);
            return;
          }
          switch (parsedMessage.type) {
            case message_types_1.default.GQL_CONNECTION_ERROR:
              if (this.connectionCallback) {
                this.connectionCallback(parsedMessage.payload);
              }
              break;
            case message_types_1.default.GQL_CONNECTION_ACK:
              this.eventEmitter.emit(this.reconnecting ? 'reconnected' : 'connected');
              this.reconnecting = false;
              this.backoff.reset();
              this.maxConnectTimeGenerator.reset();
              if (this.connectionCallback) {
                this.connectionCallback();
              }
              break;
            case message_types_1.default.GQL_COMPLETE:
              this.operations[ opId ].handler(null, null);
              delete this.operations[ opId ];
              break;
            case message_types_1.default.GQL_ERROR:
              this.operations[ opId ].handler(this.formatErrors(parsedMessage.payload), null);
              delete this.operations[ opId ];
              break;
            case message_types_1.default.GQL_DATA:
              var parsedPayload = !parsedMessage.payload.errors ?
                parsedMessage.payload : __assign({}, parsedMessage.payload, { errors: this.formatErrors(parsedMessage.payload.errors) });
              this.operations[ opId ].handler(null, parsedPayload);
              break;
            case message_types_1.default.GQL_CONNECTION_KEEP_ALIVE:
              var firstKA = typeof this.wasKeepAliveReceived === 'undefined';
              this.wasKeepAliveReceived = true;
              if (firstKA) {
                this.checkConnection();
              }
              if (this.checkConnectionIntervalId) {
                clearInterval(this.checkConnectionIntervalId);
                this.checkConnection();
              }
              this.checkConnectionIntervalId = setInterval(this.checkConnection.bind(this), this.wsTimeout);
              break;
            default:
              throw new Error('Invalid message type!');
          }
        };
        return SubscriptionClient;
      }());
      exports.SubscriptionClient = SubscriptionClient;
//# sourceMappingURL=client.js.map
      /* WEBPACK VAR INJECTION */
    }.call(exports, __webpack_require__(0), __webpack_require__(1)))

    /***/
  }),
  /* 3 */
  /***/ (function(module, exports) {

    /**
     * Expose `Backoff`.
     */

    module.exports = Backoff;

    /**
     * Initialize backoff timer with `opts`.
     *
     * - `min` initial timeout in milliseconds [100]
     * - `max` max timeout [10000]
     * - `jitter` [0]
     * - `factor` [2]
     *
     * @param {Object} opts
     * @api public
     */

    function Backoff(opts) {
      opts = opts || {};
      this.ms = opts.min || 100;
      this.max = opts.max || 10000;
      this.factor = opts.factor || 2;
      this.jitter = opts.jitter > 0 && opts.jitter <= 1 ? opts.jitter : 0;
      this.attempts = 0;
    }

    /**
     * Return the backoff duration.
     *
     * @return {Number}
     * @api public
     */

    Backoff.prototype.duration = function() {
      var ms = this.ms * Math.pow(this.factor, this.attempts++);
      if (this.jitter) {
        var rand = Math.random();
        var deviation = Math.floor(rand * this.jitter * ms);
        ms = (Math.floor(rand * 10) & 1) == 0 ? ms - deviation : ms + deviation;
      }
      return Math.min(ms, this.max) | 0;
    };

    /**
     * Reset the number of attempts.
     *
     * @api public
     */

    Backoff.prototype.reset = function() {
      this.attempts = 0;
    };

    /**
     * Set the minimum duration
     *
     * @api public
     */

    Backoff.prototype.setMin = function(min) {
      this.ms = min;
    };

    /**
     * Set the maximum duration
     *
     * @api public
     */

    Backoff.prototype.setMax = function(max) {
      this.max = max;
    };

    /**
     * Set the jitter
     *
     * @api public
     */

    Backoff.prototype.setJitter = function(jitter) {
      this.jitter = jitter;
    };

    /***/
  }),
  /* 4 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    var has = Object.prototype.hasOwnProperty
      , prefix = '~';

    /**
     * Constructor to create a storage for our `EE` objects.
     * An `Events` instance is a plain object whose properties are event names.
     *
     * @constructor
     * @api private
     */
    function Events() {
    }

//
// We try to not inherit from `Object.prototype`. In some engines creating an
// instance in this way is faster than calling `Object.create(null)` directly.
// If `Object.create(null)` is not supported we prefix the event names with a
// character to make sure that the built-in object properties are not
// overridden or used as an attack vector.
//
    if (Object.create) {
      Events.prototype = Object.create(null);

      //
      // This hack is needed because the `__proto__` property is still inherited in
      // some old browsers like Android 4, iPhone 5.1, Opera 11 and Safari 5.
      //
      if (!new Events().__proto__) prefix = false;
    }

    /**
     * Representation of a single event listener.
     *
     * @param {Function} fn The listener function.
     * @param {Mixed} context The context to invoke the listener with.
     * @param {Boolean} [once=false] Specify if the listener is a one-time listener.
     * @constructor
     * @api private
     */
    function EE(fn, context, once) {
      this.fn = fn;
      this.context = context;
      this.once = once || false;
    }

    /**
     * Minimal `EventEmitter` interface that is molded against the Node.js
     * `EventEmitter` interface.
     *
     * @constructor
     * @api public
     */
    function EventEmitter() {
      this._events = new Events();
      this._eventsCount = 0;
    }

    /**
     * Return an array listing the events for which the emitter has registered
     * listeners.
     *
     * @returns {Array}
     * @api public
     */
    EventEmitter.prototype.eventNames = function eventNames() {
      var names = []
        , events
        , name;

      if (this._eventsCount === 0) return names;

      for (name in (events = this._events)) {
        if (has.call(events, name)) names.push(prefix ? name.slice(1) : name);
      }

      if (Object.getOwnPropertySymbols) {
        return names.concat(Object.getOwnPropertySymbols(events));
      }

      return names;
    };

    /**
     * Return the listeners registered for a given event.
     *
     * @param {String|Symbol} event The event name.
     * @param {Boolean} exists Only check if there are listeners.
     * @returns {Array|Boolean}
     * @api public
     */
    EventEmitter.prototype.listeners = function listeners(event, exists) {
      var evt = prefix ? prefix + event : event
        , available = this._events[ evt ];

      if (exists) return !!available;
      if (!available) return [];
      if (available.fn) return [ available.fn ];

      for (var i = 0, l = available.length, ee = new Array(l); i < l; i++) {
        ee[ i ] = available[ i ].fn;
      }

      return ee;
    };

    /**
     * Calls each of the listeners registered for a given event.
     *
     * @param {String|Symbol} event The event name.
     * @returns {Boolean} `true` if the event had listeners, else `false`.
     * @api public
     */
    EventEmitter.prototype.emit = function emit(event, a1, a2, a3, a4, a5) {
      var evt = prefix ? prefix + event : event;

      if (!this._events[ evt ]) return false;

      var listeners = this._events[ evt ]
        , len = arguments.length
        , args
        , i;

      if (listeners.fn) {
        if (listeners.once) this.removeListener(event, listeners.fn, undefined, true);

        switch (len) {
          case 1:
            return listeners.fn.call(listeners.context), true;
          case 2:
            return listeners.fn.call(listeners.context, a1), true;
          case 3:
            return listeners.fn.call(listeners.context, a1, a2), true;
          case 4:
            return listeners.fn.call(listeners.context, a1, a2, a3), true;
          case 5:
            return listeners.fn.call(listeners.context, a1, a2, a3, a4), true;
          case 6:
            return listeners.fn.call(listeners.context, a1, a2, a3, a4, a5), true;
        }

        for (i = 1, args = new Array(len - 1); i < len; i++) {
          args[ i - 1 ] = arguments[ i ];
        }

        listeners.fn.apply(listeners.context, args);
      } else {
        var length = listeners.length
          , j;

        for (i = 0; i < length; i++) {
          if (listeners[ i ].once) this.removeListener(event, listeners[ i ].fn, undefined, true);

          switch (len) {
            case 1:
              listeners[ i ].fn.call(listeners[ i ].context);
              break;
            case 2:
              listeners[ i ].fn.call(listeners[ i ].context, a1);
              break;
            case 3:
              listeners[ i ].fn.call(listeners[ i ].context, a1, a2);
              break;
            case 4:
              listeners[ i ].fn.call(listeners[ i ].context, a1, a2, a3);
              break;
            default:
              if (!args) for (j = 1, args = new Array(len - 1); j < len; j++) {
                args[ j - 1 ] = arguments[ j ];
              }

              listeners[ i ].fn.apply(listeners[ i ].context, args);
          }
        }
      }

      return true;
    };

    /**
     * Add a listener for a given event.
     *
     * @param {String|Symbol} event The event name.
     * @param {Function} fn The listener function.
     * @param {Mixed} [context=this] The context to invoke the listener with.
     * @returns {EventEmitter} `this`.
     * @api public
     */
    EventEmitter.prototype.on = function on(event, fn, context) {
      var listener = new EE(fn, context || this)
        , evt = prefix ? prefix + event : event;

      if (!this._events[ evt ]) this._events[ evt ] = listener, this._eventsCount++;
      else if (!this._events[ evt ].fn) this._events[ evt ].push(listener);
      else this._events[ evt ] = [ this._events[ evt ], listener ];

      return this;
    };

    /**
     * Add a one-time listener for a given event.
     *
     * @param {String|Symbol} event The event name.
     * @param {Function} fn The listener function.
     * @param {Mixed} [context=this] The context to invoke the listener with.
     * @returns {EventEmitter} `this`.
     * @api public
     */
    EventEmitter.prototype.once = function once(event, fn, context) {
      var listener = new EE(fn, context || this, true)
        , evt = prefix ? prefix + event : event;

      if (!this._events[ evt ]) this._events[ evt ] = listener, this._eventsCount++;
      else if (!this._events[ evt ].fn) this._events[ evt ].push(listener);
      else this._events[ evt ] = [ this._events[ evt ], listener ];

      return this;
    };

    /**
     * Remove the listeners of a given event.
     *
     * @param {String|Symbol} event The event name.
     * @param {Function} fn Only remove the listeners that match this function.
     * @param {Mixed} context Only remove the listeners that have this context.
     * @param {Boolean} once Only remove one-time listeners.
     * @returns {EventEmitter} `this`.
     * @api public
     */
    EventEmitter.prototype.removeListener = function removeListener(event, fn, context, once) {
      var evt = prefix ? prefix + event : event;

      if (!this._events[ evt ]) return this;
      if (!fn) {
        if (--this._eventsCount === 0) this._events = new Events();
        else delete this._events[ evt ];
        return this;
      }

      var listeners = this._events[ evt ];

      if (listeners.fn) {
        if (
          listeners.fn === fn
          && (!once || listeners.once)
          && (!context || listeners.context === context)
        ) {
          if (--this._eventsCount === 0) this._events = new Events();
          else delete this._events[ evt ];
        }
      } else {
        for (var i = 0, events = [], length = listeners.length; i < length; i++) {
          if (
            listeners[ i ].fn !== fn
            || (once && !listeners[ i ].once)
            || (context && listeners[ i ].context !== context)
          ) {
            events.push(listeners[ i ]);
          }
        }

        //
        // Reset the array, or remove it completely if we have no more listeners.
        //
        if (events.length) this._events[ evt ] = events.length === 1 ? events[ 0 ] : events;
        else if (--this._eventsCount === 0) this._events = new Events();
        else delete this._events[ evt ];
      }

      return this;
    };

    /**
     * Remove all listeners, or those of the specified event.
     *
     * @param {String|Symbol} [event] The event name.
     * @returns {EventEmitter} `this`.
     * @api public
     */
    EventEmitter.prototype.removeAllListeners = function removeAllListeners(event) {
      var evt;

      if (event) {
        evt = prefix ? prefix + event : event;
        if (this._events[ evt ]) {
          if (--this._eventsCount === 0) this._events = new Events();
          else delete this._events[ evt ];
        }
      } else {
        this._events = new Events();
        this._eventsCount = 0;
      }

      return this;
    };

//
// Alias methods names because people roll like that.
//
    EventEmitter.prototype.off = EventEmitter.prototype.removeListener;
    EventEmitter.prototype.addListener = EventEmitter.prototype.on;

//
// This function doesn't apply anymore.
//
    EventEmitter.prototype.setMaxListeners = function setMaxListeners() {
      return this;
    };

//
// Expose the prefix.
//
    EventEmitter.prefixed = prefix;

//
// Allow `EventEmitter` to be imported as module namespace.
//
    EventEmitter.EventEmitter = EventEmitter;

//
// Expose the module.
//
    if (true) {
      module.exports = EventEmitter;
    }

    /***/
  }),
  /* 5 */
  /***/ (function(module, exports) {

    /**
     * lodash 4.0.1 (Custom Build) <https://lodash.com/>
     * Build: `lodash modularize exports="npm" -o ./`
     * Copyright 2012-2016 The Dojo Foundation <http://dojofoundation.org/>
     * Based on Underscore.js 1.8.3 <http://underscorejs.org/LICENSE>
     * Copyright 2009-2016 Jeremy Ashkenas, DocumentCloud and Investigative Reporters & Editors
     * Available under MIT license <https://lodash.com/license>
     */

    /** `Object#toString` result references. */
    var stringTag = '[object String]';

    /** Used for built-in method references. */
    var objectProto = Object.prototype;

    /**
     * Used to resolve the [`toStringTag`](http://ecma-international.org/ecma-262/6.0/#sec-object.prototype.tostring)
     * of values.
     */
    var objectToString = objectProto.toString;

    /**
     * Checks if `value` is classified as an `Array` object.
     *
     * @static
     * @memberOf _
     * @type Function
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is correctly classified, else `false`.
     * @example
     *
     * _.isArray([1, 2, 3]);
     * // => true
     *
     * _.isArray(document.body.children);
     * // => false
     *
     * _.isArray('abc');
     * // => false
     *
     * _.isArray(_.noop);
     * // => false
     */
    var isArray = Array.isArray;

    /**
     * Checks if `value` is object-like. A value is object-like if it's not `null`
     * and has a `typeof` result of "object".
     *
     * @static
     * @memberOf _
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is object-like, else `false`.
     * @example
     *
     * _.isObjectLike({});
     * // => true
     *
     * _.isObjectLike([1, 2, 3]);
     * // => true
     *
     * _.isObjectLike(_.noop);
     * // => false
     *
     * _.isObjectLike(null);
     * // => false
     */
    function isObjectLike(value) {
      return !!value && typeof value == 'object';
    }

    /**
     * Checks if `value` is classified as a `String` primitive or object.
     *
     * @static
     * @memberOf _
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is correctly classified, else `false`.
     * @example
     *
     * _.isString('abc');
     * // => true
     *
     * _.isString(1);
     * // => false
     */
    function isString(value) {
      return typeof value == 'string' ||
        (!isArray(value) && isObjectLike(value) && objectToString.call(value) == stringTag);
    }

    module.exports = isString;

    /***/
  }),
  /* 6 */
  /***/ (function(module, exports) {

    /**
     * lodash 3.0.2 (Custom Build) <https://lodash.com/>
     * Build: `lodash modern modularize exports="npm" -o ./`
     * Copyright 2012-2015 The Dojo Foundation <http://dojofoundation.org/>
     * Based on Underscore.js 1.8.3 <http://underscorejs.org/LICENSE>
     * Copyright 2009-2015 Jeremy Ashkenas, DocumentCloud and Investigative Reporters & Editors
     * Available under MIT license <https://lodash.com/license>
     */

    /**
     * Checks if `value` is the [language type](https://es5.github.io/#x8) of `Object`.
     * (e.g. arrays, functions, objects, regexes, `new Number(0)`, and `new String('')`)
     *
     * @static
     * @memberOf _
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is an object, else `false`.
     * @example
     *
     * _.isObject({});
     * // => true
     *
     * _.isObject([1, 2, 3]);
     * // => true
     *
     * _.isObject(1);
     * // => false
     */
    function isObject(value) {
      // Avoid a V8 JIT bug in Chrome 19-20.
      // See https://code.google.com/p/v8/issues/detail?id=2291 for more details.
      var type = typeof value;
      return !!value && (type == 'object' || type == 'function');
    }

    module.exports = isObject;

    /***/
  }),
  /* 7 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    Object.defineProperty(exports, "__esModule", {
      value: true
    });
    exports.print = print;

    var _visitor = __webpack_require__(8);

    /**
     * Converts an AST into a string, using one set of reasonable
     * formatting rules.
     */
    function print(ast) {
      return (0, _visitor.visit)(ast, { leave: printDocASTReducer });
    }

    /**
     *  Copyright (c) 2015, Facebook, Inc.
     *  All rights reserved.
     *
     *  This source code is licensed under the BSD-style license found in the
     *  LICENSE file in the root directory of this source tree. An additional grant
     *  of patent rights can be found in the PATENTS file in the same directory.
     */

    var printDocASTReducer = {
      Name:     function Name(node) {
        return node.value;
      },
      Variable: function Variable(node) {
        return '$' + node.name;
      },

      // Document

      Document: function Document(node) {
        return join(node.definitions, '\n\n') + '\n';
      },

      OperationDefinition: function OperationDefinition(node) {
        var op = node.operation;
        var name = node.name;
        var varDefs = wrap('(', join(node.variableDefinitions, ', '), ')');
        var directives = join(node.directives, ' ');
        var selectionSet = node.selectionSet;
        // Anonymous queries with no directives or variable definitions can use
        // the query short form.
        return !name && !directives && !varDefs && op === 'query' ? selectionSet : join([ op, join([ name, varDefs ]), directives, selectionSet ], ' ');
      },

      VariableDefinition: function VariableDefinition(_ref) {
        var variable = _ref.variable,
          type = _ref.type,
          defaultValue = _ref.defaultValue;
        return variable + ': ' + type + wrap(' = ', defaultValue);
      },

      SelectionSet: function SelectionSet(_ref2) {
        var selections = _ref2.selections;
        return block(selections);
      },

      Field: function Field(_ref3) {
        var alias = _ref3.alias,
          name = _ref3.name,
          args = _ref3.arguments,
          directives = _ref3.directives,
          selectionSet = _ref3.selectionSet;
        return join([ wrap('', alias, ': ') + name + wrap('(', join(args, ', '), ')'), join(directives, ' '), selectionSet ], ' ');
      },

      Argument: function Argument(_ref4) {
        var name = _ref4.name,
          value = _ref4.value;
        return name + ': ' + value;
      },

      // Fragments

      FragmentSpread: function FragmentSpread(_ref5) {
        var name = _ref5.name,
          directives = _ref5.directives;
        return '...' + name + wrap(' ', join(directives, ' '));
      },

      InlineFragment: function InlineFragment(_ref6) {
        var typeCondition = _ref6.typeCondition,
          directives = _ref6.directives,
          selectionSet = _ref6.selectionSet;
        return join([ '...', wrap('on ', typeCondition), join(directives, ' '), selectionSet ], ' ');
      },

      FragmentDefinition: function FragmentDefinition(_ref7) {
        var name = _ref7.name,
          typeCondition = _ref7.typeCondition,
          directives = _ref7.directives,
          selectionSet = _ref7.selectionSet;
        return 'fragment ' + name + ' on ' + typeCondition + ' ' + wrap('', join(directives, ' '), ' ') + selectionSet;
      },

      // Value

      IntValue:     function IntValue(_ref8) {
        var value = _ref8.value;
        return value;
      },
      FloatValue:   function FloatValue(_ref9) {
        var value = _ref9.value;
        return value;
      },
      StringValue:  function StringValue(_ref10) {
        var value = _ref10.value;
        return JSON.stringify(value);
      },
      BooleanValue: function BooleanValue(_ref11) {
        var value = _ref11.value;
        return JSON.stringify(value);
      },
      NullValue:    function NullValue() {
        return 'null';
      },
      EnumValue:    function EnumValue(_ref12) {
        var value = _ref12.value;
        return value;
      },
      ListValue:    function ListValue(_ref13) {
        var values = _ref13.values;
        return '[' + join(values, ', ') + ']';
      },
      ObjectValue:  function ObjectValue(_ref14) {
        var fields = _ref14.fields;
        return '{' + join(fields, ', ') + '}';
      },
      ObjectField:  function ObjectField(_ref15) {
        var name = _ref15.name,
          value = _ref15.value;
        return name + ': ' + value;
      },

      // Directive

      Directive: function Directive(_ref16) {
        var name = _ref16.name,
          args = _ref16.arguments;
        return '@' + name + wrap('(', join(args, ', '), ')');
      },

      // Type

      NamedType:   function NamedType(_ref17) {
        var name = _ref17.name;
        return name;
      },
      ListType:    function ListType(_ref18) {
        var type = _ref18.type;
        return '[' + type + ']';
      },
      NonNullType: function NonNullType(_ref19) {
        var type = _ref19.type;
        return type + '!';
      },

      // Type System Definitions

      SchemaDefinition: function SchemaDefinition(_ref20) {
        var directives = _ref20.directives,
          operationTypes = _ref20.operationTypes;
        return join([ 'schema', join(directives, ' '), block(operationTypes) ], ' ');
      },

      OperationTypeDefinition: function OperationTypeDefinition(_ref21) {
        var operation = _ref21.operation,
          type = _ref21.type;
        return operation + ': ' + type;
      },

      ScalarTypeDefinition: function ScalarTypeDefinition(_ref22) {
        var name = _ref22.name,
          directives = _ref22.directives;
        return join([ 'scalar', name, join(directives, ' ') ], ' ');
      },

      ObjectTypeDefinition: function ObjectTypeDefinition(_ref23) {
        var name = _ref23.name,
          interfaces = _ref23.interfaces,
          directives = _ref23.directives,
          fields = _ref23.fields;
        return join([ 'type', name, wrap('implements ', join(interfaces, ', ')), join(directives, ' '), block(fields) ], ' ');
      },

      FieldDefinition: function FieldDefinition(_ref24) {
        var name = _ref24.name,
          args = _ref24.arguments,
          type = _ref24.type,
          directives = _ref24.directives;
        return name + wrap('(', join(args, ', '), ')') + ': ' + type + wrap(' ', join(directives, ' '));
      },

      InputValueDefinition: function InputValueDefinition(_ref25) {
        var name = _ref25.name,
          type = _ref25.type,
          defaultValue = _ref25.defaultValue,
          directives = _ref25.directives;
        return join([ name + ': ' + type, wrap('= ', defaultValue), join(directives, ' ') ], ' ');
      },

      InterfaceTypeDefinition: function InterfaceTypeDefinition(_ref26) {
        var name = _ref26.name,
          directives = _ref26.directives,
          fields = _ref26.fields;
        return join([ 'interface', name, join(directives, ' '), block(fields) ], ' ');
      },

      UnionTypeDefinition: function UnionTypeDefinition(_ref27) {
        var name = _ref27.name,
          directives = _ref27.directives,
          types = _ref27.types;
        return join([ 'union', name, join(directives, ' '), '= ' + join(types, ' | ') ], ' ');
      },

      EnumTypeDefinition: function EnumTypeDefinition(_ref28) {
        var name = _ref28.name,
          directives = _ref28.directives,
          values = _ref28.values;
        return join([ 'enum', name, join(directives, ' '), block(values) ], ' ');
      },

      EnumValueDefinition: function EnumValueDefinition(_ref29) {
        var name = _ref29.name,
          directives = _ref29.directives;
        return join([ name, join(directives, ' ') ], ' ');
      },

      InputObjectTypeDefinition: function InputObjectTypeDefinition(_ref30) {
        var name = _ref30.name,
          directives = _ref30.directives,
          fields = _ref30.fields;
        return join([ 'input', name, join(directives, ' '), block(fields) ], ' ');
      },

      TypeExtensionDefinition: function TypeExtensionDefinition(_ref31) {
        var definition = _ref31.definition;
        return 'extend ' + definition;
      },

      DirectiveDefinition: function DirectiveDefinition(_ref32) {
        var name = _ref32.name,
          args = _ref32.arguments,
          locations = _ref32.locations;
        return 'directive @' + name + wrap('(', join(args, ', '), ')') + ' on ' + join(locations, ' | ');
      }
    };

    /**
     * Given maybeArray, print an empty string if it is null or empty, otherwise
     * print all items together separated by separator if provided
     */
    function join(maybeArray, separator) {
      return maybeArray ? maybeArray.filter(function(x) {
        return x;
      }).join(separator || '') : '';
    }

    /**
     * Given array, print each item on its own line, wrapped in an
     * indented "{ }" block.
     */
    function block(array) {
      return array && array.length !== 0 ? indent('{\n' + join(array, '\n')) + '\n}' : '{}';
    }

    /**
     * If maybeString is not null or empty, then wrap with start and end, otherwise
     * print an empty string.
     */
    function wrap(start, maybeString, end) {
      return maybeString ? start + maybeString + (end || '') : '';
    }

    function indent(maybeString) {
      return maybeString && maybeString.replace(/\n/g, '\n  ');
    }

    /***/
  }),
  /* 8 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    Object.defineProperty(exports, "__esModule", {
      value: true
    });
    exports.visit = visit;
    exports.visitInParallel = visitInParallel;
    exports.visitWithTypeInfo = visitWithTypeInfo;
    exports.getVisitFn = getVisitFn;
    /**
     *  Copyright (c) 2015, Facebook, Inc.
     *  All rights reserved.
     *
     *  This source code is licensed under the BSD-style license found in the
     *  LICENSE file in the root directory of this source tree. An additional grant
     *  of patent rights can be found in the PATENTS file in the same directory.
     */

    var QueryDocumentKeys = exports.QueryDocumentKeys = {
      Name: [],

      Document:            [ 'definitions' ],
      OperationDefinition: [ 'name', 'variableDefinitions', 'directives', 'selectionSet' ],
      VariableDefinition:  [ 'variable', 'type', 'defaultValue' ],
      Variable:            [ 'name' ],
      SelectionSet:        [ 'selections' ],
      Field:               [ 'alias', 'name', 'arguments', 'directives', 'selectionSet' ],
      Argument:            [ 'name', 'value' ],

      FragmentSpread:     [ 'name', 'directives' ],
      InlineFragment:     [ 'typeCondition', 'directives', 'selectionSet' ],
      FragmentDefinition: [ 'name', 'typeCondition', 'directives', 'selectionSet' ],

      IntValue:     [],
      FloatValue:   [],
      StringValue:  [],
      BooleanValue: [],
      NullValue:    [],
      EnumValue:    [],
      ListValue:    [ 'values' ],
      ObjectValue:  [ 'fields' ],
      ObjectField:  [ 'name', 'value' ],

      Directive: [ 'name', 'arguments' ],

      NamedType:   [ 'name' ],
      ListType:    [ 'type' ],
      NonNullType: [ 'type' ],

      SchemaDefinition:        [ 'directives', 'operationTypes' ],
      OperationTypeDefinition: [ 'type' ],

      ScalarTypeDefinition:      [ 'name', 'directives' ],
      ObjectTypeDefinition:      [ 'name', 'interfaces', 'directives', 'fields' ],
      FieldDefinition:           [ 'name', 'arguments', 'type', 'directives' ],
      InputValueDefinition:      [ 'name', 'type', 'defaultValue', 'directives' ],
      InterfaceTypeDefinition:   [ 'name', 'directives', 'fields' ],
      UnionTypeDefinition:       [ 'name', 'directives', 'types' ],
      EnumTypeDefinition:        [ 'name', 'directives', 'values' ],
      EnumValueDefinition:       [ 'name', 'directives' ],
      InputObjectTypeDefinition: [ 'name', 'directives', 'fields' ],

      TypeExtensionDefinition: [ 'definition' ],

      DirectiveDefinition: [ 'name', 'arguments', 'locations' ]
    };

    var BREAK = exports.BREAK = {};

    /**
     * visit() will walk through an AST using a depth first traversal, calling
     * the visitor's enter function at each node in the traversal, and calling the
     * leave function after visiting that node and all of its child nodes.
     *
     * By returning different values from the enter and leave functions, the
     * behavior of the visitor can be altered, including skipping over a sub-tree of
     * the AST (by returning false), editing the AST by returning a value or null
     * to remove the value, or to stop the whole traversal by returning BREAK.
     *
     * When using visit() to edit an AST, the original AST will not be modified, and
     * a new version of the AST with the changes applied will be returned from the
     * visit function.
     *
     *     const editedAST = visit(ast, {
 *       enter(node, key, parent, path, ancestors) {
 *         // @return
 *         //   undefined: no action
 *         //   false: skip visiting this node
 *         //   visitor.BREAK: stop visiting altogether
 *         //   null: delete this node
 *         //   any value: replace this node with the returned value
 *       },
 *       leave(node, key, parent, path, ancestors) {
 *         // @return
 *         //   undefined: no action
 *         //   false: no action
 *         //   visitor.BREAK: stop visiting altogether
 *         //   null: delete this node
 *         //   any value: replace this node with the returned value
 *       }
 *     });
     *
     * Alternatively to providing enter() and leave() functions, a visitor can
     * instead provide functions named the same as the kinds of AST nodes, or
     * enter/leave visitors at a named key, leading to four permutations of
     * visitor API:
     *
     * 1) Named visitors triggered when entering a node a specific kind.
     *
     *     visit(ast, {
 *       Kind(node) {
 *         // enter the "Kind" node
 *       }
 *     })
     *
     * 2) Named visitors that trigger upon entering and leaving a node of
     *    a specific kind.
     *
     *     visit(ast, {
 *       Kind: {
 *         enter(node) {
 *           // enter the "Kind" node
 *         }
 *         leave(node) {
 *           // leave the "Kind" node
 *         }
 *       }
 *     })
     *
     * 3) Generic visitors that trigger upon entering and leaving any node.
     *
     *     visit(ast, {
 *       enter(node) {
 *         // enter any node
 *       },
 *       leave(node) {
 *         // leave any node
 *       }
 *     })
     *
     * 4) Parallel visitors for entering and leaving nodes of a specific kind.
     *
     *     visit(ast, {
 *       enter: {
 *         Kind(node) {
 *           // enter the "Kind" node
 *         }
 *       },
 *       leave: {
 *         Kind(node) {
 *           // leave the "Kind" node
 *         }
 *       }
 *     })
     */
    function visit(root, visitor, keyMap) {
      var visitorKeys = keyMap || QueryDocumentKeys;

      var stack = void 0;
      var inArray = Array.isArray(root);
      var keys = [ root ];
      var index = -1;
      var edits = [];
      var parent = void 0;
      var path = [];
      var ancestors = [];
      var newRoot = root;

      do {
        index++;
        var isLeaving = index === keys.length;
        var key = void 0;
        var node = void 0;
        var isEdited = isLeaving && edits.length !== 0;
        if (isLeaving) {
          key = ancestors.length === 0 ? undefined : path.pop();
          node = parent;
          parent = ancestors.pop();
          if (isEdited) {
            if (inArray) {
              node = node.slice();
            } else {
              var clone = {};
              for (var k in node) {
                if (node.hasOwnProperty(k)) {
                  clone[ k ] = node[ k ];
                }
              }
              node = clone;
            }
            var editOffset = 0;
            for (var ii = 0; ii < edits.length; ii++) {
              var editKey = edits[ ii ][ 0 ];
              var editValue = edits[ ii ][ 1 ];
              if (inArray) {
                editKey -= editOffset;
              }
              if (inArray && editValue === null) {
                node.splice(editKey, 1);
                editOffset++;
              } else {
                node[ editKey ] = editValue;
              }
            }
          }
          index = stack.index;
          keys = stack.keys;
          edits = stack.edits;
          inArray = stack.inArray;
          stack = stack.prev;
        } else {
          key = parent ? inArray ? index : keys[ index ] : undefined;
          node = parent ? parent[ key ] : newRoot;
          if (node === null || node === undefined) {
            continue;
          }
          if (parent) {
            path.push(key);
          }
        }

        var result = void 0;
        if (!Array.isArray(node)) {
          if (!isNode(node)) {
            throw new Error('Invalid AST Node: ' + JSON.stringify(node));
          }
          var visitFn = getVisitFn(visitor, node.kind, isLeaving);
          if (visitFn) {
            result = visitFn.call(visitor, node, key, parent, path, ancestors);

            if (result === BREAK) {
              break;
            }

            if (result === false) {
              if (!isLeaving) {
                path.pop();
                continue;
              }
            } else if (result !== undefined) {
              edits.push([ key, result ]);
              if (!isLeaving) {
                if (isNode(result)) {
                  node = result;
                } else {
                  path.pop();
                  continue;
                }
              }
            }
          }
        }

        if (result === undefined && isEdited) {
          edits.push([ key, node ]);
        }

        if (!isLeaving) {
          stack = { inArray: inArray, index: index, keys: keys, edits: edits, prev: stack };
          inArray = Array.isArray(node);
          keys = inArray ? node : visitorKeys[ node.kind ] || [];
          index = -1;
          edits = [];
          if (parent) {
            ancestors.push(parent);
          }
          parent = node;
        }
      } while (stack !== undefined);

      if (edits.length !== 0) {
        newRoot = edits[ edits.length - 1 ][ 1 ];
      }

      return newRoot;
    }

    function isNode(maybeNode) {
      return maybeNode && typeof maybeNode.kind === 'string';
    }

    /**
     * Creates a new visitor instance which delegates to many visitors to run in
     * parallel. Each visitor will be visited for each node before moving on.
     *
     * If a prior visitor edits a node, no following visitors will see that node.
     */
    function visitInParallel(visitors) {
      var skipping = new Array(visitors.length);

      return {
        enter: function enter(node) {
          for (var i = 0; i < visitors.length; i++) {
            if (!skipping[ i ]) {
              var fn = getVisitFn(visitors[ i ], node.kind, /* isLeaving */false);
              if (fn) {
                var result = fn.apply(visitors[ i ], arguments);
                if (result === false) {
                  skipping[ i ] = node;
                } else if (result === BREAK) {
                  skipping[ i ] = BREAK;
                } else if (result !== undefined) {
                  return result;
                }
              }
            }
          }
        },
        leave: function leave(node) {
          for (var i = 0; i < visitors.length; i++) {
            if (!skipping[ i ]) {
              var fn = getVisitFn(visitors[ i ], node.kind, /* isLeaving */true);
              if (fn) {
                var result = fn.apply(visitors[ i ], arguments);
                if (result === BREAK) {
                  skipping[ i ] = BREAK;
                } else if (result !== undefined && result !== false) {
                  return result;
                }
              }
            } else if (skipping[ i ] === node) {
              skipping[ i ] = null;
            }
          }
        }
      };
    }

    /**
     * Creates a new visitor instance which maintains a provided TypeInfo instance
     * along with visiting visitor.
     */
    function visitWithTypeInfo(typeInfo, visitor) {
      return {
        enter: function enter(node) {
          typeInfo.enter(node);
          var fn = getVisitFn(visitor, node.kind, /* isLeaving */false);
          if (fn) {
            var result = fn.apply(visitor, arguments);
            if (result !== undefined) {
              typeInfo.leave(node);
              if (isNode(result)) {
                typeInfo.enter(result);
              }
            }
            return result;
          }
        },
        leave: function leave(node) {
          var fn = getVisitFn(visitor, node.kind, /* isLeaving */true);
          var result = void 0;
          if (fn) {
            result = fn.apply(visitor, arguments);
          }
          typeInfo.leave(node);
          return result;
        }
      };
    }

    /**
     * Given a visitor instance, if it is leaving or not, and a node kind, return
     * the function the visitor runtime should call.
     */
    function getVisitFn(visitor, kind, isLeaving) {
      var kindVisitor = visitor[ kind ];
      if (kindVisitor) {
        if (!isLeaving && typeof kindVisitor === 'function') {
          // { Kind() {} }
          return kindVisitor;
        }
        var kindSpecificVisitor = isLeaving ? kindVisitor.leave : kindVisitor.enter;
        if (typeof kindSpecificVisitor === 'function') {
          // { Kind: { enter() {}, leave() {} } }
          return kindSpecificVisitor;
        }
      } else {
        var specificVisitor = isLeaving ? visitor.leave : visitor.enter;
        if (specificVisitor) {
          if (typeof specificVisitor === 'function') {
            // { enter() {}, leave() {} }
            return specificVisitor;
          }
          var specificKindVisitor = specificVisitor[ kind ];
          if (typeof specificKindVisitor === 'function') {
            // { enter: { Kind() {} }, leave: { Kind() {} } }
            return specificKindVisitor;
          }
        }
      }
    }

    /***/
  }),
  /* 9 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    Object.defineProperty(exports, "__esModule", {
      value: true
    });
    exports.getOperationAST = getOperationAST;

    var _kinds = __webpack_require__(10);

    /**
     * Returns an operation AST given a document AST and optionally an operation
     * name. If a name is not provided, an operation is only returned if only one is
     * provided in the document.
     */
    function getOperationAST(documentAST, operationName) {
      var operation = null;
      for (var i = 0; i < documentAST.definitions.length; i++) {
        var definition = documentAST.definitions[ i ];
        if (definition.kind === _kinds.OPERATION_DEFINITION) {
          if (!operationName) {
            // If no operation name was provided, only return an Operation if there
            // is one defined in the document. Upon encountering the second, return
            // null.
            if (operation) {
              return null;
            }
            operation = definition;
          } else if (definition.name && definition.name.value === operationName) {
            return definition;
          }
        }
      }
      return operation;
    }

    /**
     *  Copyright (c) 2015, Facebook, Inc.
     *  All rights reserved.
     *
     *  This source code is licensed under the BSD-style license found in the
     *  LICENSE file in the root directory of this source tree. An additional grant
     *  of patent rights can be found in the PATENTS file in the same directory.
     */

    /***/
  }),
  /* 10 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    Object.defineProperty(exports, "__esModule", {
      value: true
    });

    /**
     *  Copyright (c) 2015, Facebook, Inc.
     *  All rights reserved.
     *
     *  This source code is licensed under the BSD-style license found in the
     *  LICENSE file in the root directory of this source tree. An additional grant
     *  of patent rights can be found in the PATENTS file in the same directory.
     */

// Name

    var NAME = exports.NAME = 'Name';

// Document

    var DOCUMENT = exports.DOCUMENT = 'Document';
    var OPERATION_DEFINITION = exports.OPERATION_DEFINITION = 'OperationDefinition';
    var VARIABLE_DEFINITION = exports.VARIABLE_DEFINITION = 'VariableDefinition';
    var VARIABLE = exports.VARIABLE = 'Variable';
    var SELECTION_SET = exports.SELECTION_SET = 'SelectionSet';
    var FIELD = exports.FIELD = 'Field';
    var ARGUMENT = exports.ARGUMENT = 'Argument';

// Fragments

    var FRAGMENT_SPREAD = exports.FRAGMENT_SPREAD = 'FragmentSpread';
    var INLINE_FRAGMENT = exports.INLINE_FRAGMENT = 'InlineFragment';
    var FRAGMENT_DEFINITION = exports.FRAGMENT_DEFINITION = 'FragmentDefinition';

// Values

    var INT = exports.INT = 'IntValue';
    var FLOAT = exports.FLOAT = 'FloatValue';
    var STRING = exports.STRING = 'StringValue';
    var BOOLEAN = exports.BOOLEAN = 'BooleanValue';
    var NULL = exports.NULL = 'NullValue';
    var ENUM = exports.ENUM = 'EnumValue';
    var LIST = exports.LIST = 'ListValue';
    var OBJECT = exports.OBJECT = 'ObjectValue';
    var OBJECT_FIELD = exports.OBJECT_FIELD = 'ObjectField';

// Directives

    var DIRECTIVE = exports.DIRECTIVE = 'Directive';

// Types

    var NAMED_TYPE = exports.NAMED_TYPE = 'NamedType';
    var LIST_TYPE = exports.LIST_TYPE = 'ListType';
    var NON_NULL_TYPE = exports.NON_NULL_TYPE = 'NonNullType';

// Type System Definitions

    var SCHEMA_DEFINITION = exports.SCHEMA_DEFINITION = 'SchemaDefinition';
    var OPERATION_TYPE_DEFINITION = exports.OPERATION_TYPE_DEFINITION = 'OperationTypeDefinition';

// Type Definitions

    var SCALAR_TYPE_DEFINITION = exports.SCALAR_TYPE_DEFINITION = 'ScalarTypeDefinition';
    var OBJECT_TYPE_DEFINITION = exports.OBJECT_TYPE_DEFINITION = 'ObjectTypeDefinition';
    var FIELD_DEFINITION = exports.FIELD_DEFINITION = 'FieldDefinition';
    var INPUT_VALUE_DEFINITION = exports.INPUT_VALUE_DEFINITION = 'InputValueDefinition';
    var INTERFACE_TYPE_DEFINITION = exports.INTERFACE_TYPE_DEFINITION = 'InterfaceTypeDefinition';
    var UNION_TYPE_DEFINITION = exports.UNION_TYPE_DEFINITION = 'UnionTypeDefinition';
    var ENUM_TYPE_DEFINITION = exports.ENUM_TYPE_DEFINITION = 'EnumTypeDefinition';
    var ENUM_VALUE_DEFINITION = exports.ENUM_VALUE_DEFINITION = 'EnumValueDefinition';
    var INPUT_OBJECT_TYPE_DEFINITION = exports.INPUT_OBJECT_TYPE_DEFINITION = 'InputObjectTypeDefinition';

// Type Extensions

    var TYPE_EXTENSION_DEFINITION = exports.TYPE_EXTENSION_DEFINITION = 'TypeExtensionDefinition';

// Directive Definitions

    var DIRECTIVE_DEFINITION = exports.DIRECTIVE_DEFINITION = 'DirectiveDefinition';

    /***/
  }),
  /* 11 */
  /***/ (function(module, exports, __webpack_require__) {

    module.exports = __webpack_require__(12);

    /***/
  }),
  /* 12 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";
    /* WEBPACK VAR INJECTION */
    (function(global, module) {

      Object.defineProperty(exports, "__esModule", {
        value: true
      });

      var _ponyfill = __webpack_require__(14);

      var _ponyfill2 = _interopRequireDefault(_ponyfill);

      function _interopRequireDefault(obj) {
        return obj && obj.__esModule ? obj : { 'default': obj };
      }

      var root;
      /* global window */

      if (typeof self !== 'undefined') {
        root = self;
      } else if (typeof window !== 'undefined') {
        root = window;
      } else if (typeof global !== 'undefined') {
        root = global;
      } else if (true) {
        root = module;
      } else {
        root = Function('return this')();
      }

      var result = (0, _ponyfill2[ 'default' ])(root);
      exports[ 'default' ] = result;
      /* WEBPACK VAR INJECTION */
    }.call(exports, __webpack_require__(0), __webpack_require__(13)(module)))

    /***/
  }),
  /* 13 */
  /***/ (function(module, exports) {

    module.exports = function(module) {
      if (!module.webpackPolyfill) {
        module.deprecate = function() {
        };
        module.paths = [];
        // module.parent = undefined by default
        if (!module.children) module.children = [];
        Object.defineProperty(module, "loaded", {
          enumerable: true,
          get:        function() {
            return module.l;
          }
        });
        Object.defineProperty(module, "id", {
          enumerable: true,
          get:        function() {
            return module.i;
          }
        });
        module.webpackPolyfill = 1;
      }
      return module;
    };

    /***/
  }),
  /* 14 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    Object.defineProperty(exports, "__esModule", {
      value: true
    });
    exports[ 'default' ] = symbolObservablePonyfill;

    function symbolObservablePonyfill(root) {
      var result;
      var _Symbol = root.Symbol;

      if (typeof _Symbol === 'function') {
        if (_Symbol.observable) {
          result = _Symbol.observable;
        } else {
          result = _Symbol('observable');
          _Symbol.observable = result;
        }
      } else {
        result = '@@observable';
      }

      return result;
    };

    /***/
  }),
  /* 15 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    Object.defineProperty(exports, "__esModule", { value: true });
    var GRAPHQL_WS = 'graphql-ws';
    exports.GRAPHQL_WS = GRAPHQL_WS;
    var GRAPHQL_SUBSCRIPTIONS = 'graphql-subscriptions';
    exports.GRAPHQL_SUBSCRIPTIONS = GRAPHQL_SUBSCRIPTIONS;
//# sourceMappingURL=protocol.js.map

    /***/
  }),
  /* 16 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    Object.defineProperty(exports, "__esModule", { value: true });
    var WS_TIMEOUT = 10000;
    exports.WS_TIMEOUT = WS_TIMEOUT;
//# sourceMappingURL=defaults.js.map

    /***/
  }),
  /* 17 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";

    Object.defineProperty(exports, "__esModule", { value: true });
    var MessageTypes = (function() {
      function MessageTypes() {
        throw new Error('Static Class');
      }

      MessageTypes.GQL_CONNECTION_INIT = 'connection_init';
      MessageTypes.GQL_CONNECTION_ACK = 'connection_ack';
      MessageTypes.GQL_CONNECTION_ERROR = 'connection_error';
      MessageTypes.GQL_CONNECTION_KEEP_ALIVE = 'ka';
      MessageTypes.GQL_CONNECTION_TERMINATE = 'connection_terminate';
      MessageTypes.GQL_START = 'start';
      MessageTypes.GQL_DATA = 'data';
      MessageTypes.GQL_ERROR = 'error';
      MessageTypes.GQL_COMPLETE = 'complete';
      MessageTypes.GQL_STOP = 'stop';
      MessageTypes.SUBSCRIPTION_START = 'subscription_start';
      MessageTypes.SUBSCRIPTION_DATA = 'subscription_data';
      MessageTypes.SUBSCRIPTION_SUCCESS = 'subscription_success';
      MessageTypes.SUBSCRIPTION_FAIL = 'subscription_fail';
      MessageTypes.SUBSCRIPTION_END = 'subscription_end';
      MessageTypes.INIT = 'init';
      MessageTypes.INIT_SUCCESS = 'init_success';
      MessageTypes.INIT_FAIL = 'init_fail';
      return MessageTypes;
    }());
    exports.default = MessageTypes;
//# sourceMappingURL=message-types.js.map

    /***/
  }),
  /* 18 */
  /***/ (function(module, exports, __webpack_require__) {

    "use strict";
    /* WEBPACK VAR INJECTION */
    (function(process) {
      Object.defineProperty(exports, "__esModule", { value: true });
      var assign = __webpack_require__(19);

      function addGraphQLSubscriptions(networkInterface, wsClient) {
        if (process && process.env && process.env.NODE_ENV !== 'production') {
          console.warn('Notice that addGraphQLSubscriptions method will become deprecated in the new package ' +
            'graphql-transport-ws that will be released soon. Keep track for the new hybrid network release here: ' +
            'https://github.com/apollographql/subscriptions-transport-ws/issues/169');
        }
        return assign(networkInterface, {
          subscribe:   function(request, handler) {
            return wsClient.subscribe(request, handler);
          },
          unsubscribe: function(id) {
            wsClient.unsubscribe(id);
          },
        });
      }

      exports.addGraphQLSubscriptions = addGraphQLSubscriptions;
//# sourceMappingURL=helpers.js.map
      /* WEBPACK VAR INJECTION */
    }.call(exports, __webpack_require__(1)))

    /***/
  }),
  /* 19 */
  /***/ (function(module, exports) {

    /**
     * lodash (Custom Build) <https://lodash.com/>
     * Build: `lodash modularize exports="npm" -o ./`
     * Copyright jQuery Foundation and other contributors <https://jquery.org/>
     * Released under MIT license <https://lodash.com/license>
     * Based on Underscore.js 1.8.3 <http://underscorejs.org/LICENSE>
     * Copyright Jeremy Ashkenas, DocumentCloud and Investigative Reporters & Editors
     */

    /** Used as references for various `Number` constants. */
    var MAX_SAFE_INTEGER = 9007199254740991;

    /** `Object#toString` result references. */
    var argsTag = '[object Arguments]',
      funcTag = '[object Function]',
      genTag = '[object GeneratorFunction]';

    /** Used to detect unsigned integer values. */
    var reIsUint = /^(?:0|[1-9]\d*)$/;

    /**
     * A faster alternative to `Function#apply`, this function invokes `func`
     * with the `this` binding of `thisArg` and the arguments of `args`.
     *
     * @private
     * @param {Function} func The function to invoke.
     * @param {*} thisArg The `this` binding of `func`.
     * @param {Array} args The arguments to invoke `func` with.
     * @returns {*} Returns the result of `func`.
     */
    function apply(func, thisArg, args) {
      switch (args.length) {
        case 0:
          return func.call(thisArg);
        case 1:
          return func.call(thisArg, args[ 0 ]);
        case 2:
          return func.call(thisArg, args[ 0 ], args[ 1 ]);
        case 3:
          return func.call(thisArg, args[ 0 ], args[ 1 ], args[ 2 ]);
      }
      return func.apply(thisArg, args);
    }

    /**
     * The base implementation of `_.times` without support for iteratee shorthands
     * or max array length checks.
     *
     * @private
     * @param {number} n The number of times to invoke `iteratee`.
     * @param {Function} iteratee The function invoked per iteration.
     * @returns {Array} Returns the array of results.
     */
    function baseTimes(n, iteratee) {
      var index = -1,
        result = Array(n);

      while (++index < n) {
        result[ index ] = iteratee(index);
      }
      return result;
    }

    /**
     * Creates a unary function that invokes `func` with its argument transformed.
     *
     * @private
     * @param {Function} func The function to wrap.
     * @param {Function} transform The argument transform.
     * @returns {Function} Returns the new function.
     */
    function overArg(func, transform) {
      return function(arg) {
        return func(transform(arg));
      };
    }

    /** Used for built-in method references. */
    var objectProto = Object.prototype;

    /** Used to check objects for own properties. */
    var hasOwnProperty = objectProto.hasOwnProperty;

    /**
     * Used to resolve the
     * [`toStringTag`](http://ecma-international.org/ecma-262/7.0/#sec-object.prototype.tostring)
     * of values.
     */
    var objectToString = objectProto.toString;

    /** Built-in value references. */
    var propertyIsEnumerable = objectProto.propertyIsEnumerable;

    /* Built-in method references for those with the same name as other `lodash` methods. */
    var nativeKeys = overArg(Object.keys, Object),
      nativeMax = Math.max;

    /** Detect if properties shadowing those on `Object.prototype` are non-enumerable. */
    var nonEnumShadows = !propertyIsEnumerable.call({ 'valueOf': 1 }, 'valueOf');

    /**
     * Creates an array of the enumerable property names of the array-like `value`.
     *
     * @private
     * @param {*} value The value to query.
     * @param {boolean} inherited Specify returning inherited property names.
     * @returns {Array} Returns the array of property names.
     */
    function arrayLikeKeys(value, inherited) {
      // Safari 8.1 makes `arguments.callee` enumerable in strict mode.
      // Safari 9 makes `arguments.length` enumerable in strict mode.
      var result = (isArray(value) || isArguments(value))
        ? baseTimes(value.length, String)
        : [];

      var length = result.length,
        skipIndexes = !!length;

      for (var key in value) {
        if ((inherited || hasOwnProperty.call(value, key)) &&
          !(skipIndexes && (key == 'length' || isIndex(key, length)))) {
          result.push(key);
        }
      }
      return result;
    }

    /**
     * Assigns `value` to `key` of `object` if the existing value is not equivalent
     * using [`SameValueZero`](http://ecma-international.org/ecma-262/7.0/#sec-samevaluezero)
     * for equality comparisons.
     *
     * @private
     * @param {Object} object The object to modify.
     * @param {string} key The key of the property to assign.
     * @param {*} value The value to assign.
     */
    function assignValue(object, key, value) {
      var objValue = object[ key ];
      if (!(hasOwnProperty.call(object, key) && eq(objValue, value)) ||
        (value === undefined && !(key in object))) {
        object[ key ] = value;
      }
    }

    /**
     * The base implementation of `_.keys` which doesn't treat sparse arrays as dense.
     *
     * @private
     * @param {Object} object The object to query.
     * @returns {Array} Returns the array of property names.
     */
    function baseKeys(object) {
      if (!isPrototype(object)) {
        return nativeKeys(object);
      }
      var result = [];
      for (var key in Object(object)) {
        if (hasOwnProperty.call(object, key) && key != 'constructor') {
          result.push(key);
        }
      }
      return result;
    }

    /**
     * The base implementation of `_.rest` which doesn't validate or coerce arguments.
     *
     * @private
     * @param {Function} func The function to apply a rest parameter to.
     * @param {number} [start=func.length-1] The start position of the rest parameter.
     * @returns {Function} Returns the new function.
     */
    function baseRest(func, start) {
      start = nativeMax(start === undefined ? (func.length - 1) : start, 0);
      return function() {
        var args = arguments,
          index = -1,
          length = nativeMax(args.length - start, 0),
          array = Array(length);

        while (++index < length) {
          array[ index ] = args[ start + index ];
        }
        index = -1;
        var otherArgs = Array(start + 1);
        while (++index < start) {
          otherArgs[ index ] = args[ index ];
        }
        otherArgs[ start ] = array;
        return apply(func, this, otherArgs);
      };
    }

    /**
     * Copies properties of `source` to `object`.
     *
     * @private
     * @param {Object} source The object to copy properties from.
     * @param {Array} props The property identifiers to copy.
     * @param {Object} [object={}] The object to copy properties to.
     * @param {Function} [customizer] The function to customize copied values.
     * @returns {Object} Returns `object`.
     */
    function copyObject(source, props, object, customizer) {
      object || (object = {});

      var index = -1,
        length = props.length;

      while (++index < length) {
        var key = props[ index ];

        var newValue = customizer
          ? customizer(object[ key ], source[ key ], key, object, source)
          : undefined;

        assignValue(object, key, newValue === undefined ? source[ key ] : newValue);
      }
      return object;
    }

    /**
     * Creates a function like `_.assign`.
     *
     * @private
     * @param {Function} assigner The function to assign values.
     * @returns {Function} Returns the new assigner function.
     */
    function createAssigner(assigner) {
      return baseRest(function(object, sources) {
        var index = -1,
          length = sources.length,
          customizer = length > 1 ? sources[ length - 1 ] : undefined,
          guard = length > 2 ? sources[ 2 ] : undefined;

        customizer = (assigner.length > 3 && typeof customizer == 'function')
          ? (length--, customizer)
          : undefined;

        if (guard && isIterateeCall(sources[ 0 ], sources[ 1 ], guard)) {
          customizer = length < 3 ? undefined : customizer;
          length = 1;
        }
        object = Object(object);
        while (++index < length) {
          var source = sources[ index ];
          if (source) {
            assigner(object, source, index, customizer);
          }
        }
        return object;
      });
    }

    /**
     * Checks if `value` is a valid array-like index.
     *
     * @private
     * @param {*} value The value to check.
     * @param {number} [length=MAX_SAFE_INTEGER] The upper bounds of a valid index.
     * @returns {boolean} Returns `true` if `value` is a valid index, else `false`.
     */
    function isIndex(value, length) {
      length = length == null ? MAX_SAFE_INTEGER : length;
      return !!length &&
        (typeof value == 'number' || reIsUint.test(value)) &&
        (value > -1 && value % 1 == 0 && value < length);
    }

    /**
     * Checks if the given arguments are from an iteratee call.
     *
     * @private
     * @param {*} value The potential iteratee value argument.
     * @param {*} index The potential iteratee index or key argument.
     * @param {*} object The potential iteratee object argument.
     * @returns {boolean} Returns `true` if the arguments are from an iteratee call,
     *  else `false`.
     */
    function isIterateeCall(value, index, object) {
      if (!isObject(object)) {
        return false;
      }
      var type = typeof index;
      if (type == 'number'
        ? (isArrayLike(object) && isIndex(index, object.length))
        : (type == 'string' && index in object)
      ) {
        return eq(object[ index ], value);
      }
      return false;
    }

    /**
     * Checks if `value` is likely a prototype object.
     *
     * @private
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is a prototype, else `false`.
     */
    function isPrototype(value) {
      var Ctor = value && value.constructor,
        proto = (typeof Ctor == 'function' && Ctor.prototype) || objectProto;

      return value === proto;
    }

    /**
     * Performs a
     * [`SameValueZero`](http://ecma-international.org/ecma-262/7.0/#sec-samevaluezero)
     * comparison between two values to determine if they are equivalent.
     *
     * @static
     * @memberOf _
     * @since 4.0.0
     * @category Lang
     * @param {*} value The value to compare.
     * @param {*} other The other value to compare.
     * @returns {boolean} Returns `true` if the values are equivalent, else `false`.
     * @example
     *
     * var object = { 'a': 1 };
     * var other = { 'a': 1 };
     *
     * _.eq(object, object);
     * // => true
     *
     * _.eq(object, other);
     * // => false
     *
     * _.eq('a', 'a');
     * // => true
     *
     * _.eq('a', Object('a'));
     * // => false
     *
     * _.eq(NaN, NaN);
     * // => true
     */
    function eq(value, other) {
      return value === other || (value !== value && other !== other);
    }

    /**
     * Checks if `value` is likely an `arguments` object.
     *
     * @static
     * @memberOf _
     * @since 0.1.0
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is an `arguments` object,
     *  else `false`.
     * @example
     *
     * _.isArguments(function() { return arguments; }());
     * // => true
     *
     * _.isArguments([1, 2, 3]);
     * // => false
     */
    function isArguments(value) {
      // Safari 8.1 makes `arguments.callee` enumerable in strict mode.
      return isArrayLikeObject(value) && hasOwnProperty.call(value, 'callee') &&
        (!propertyIsEnumerable.call(value, 'callee') || objectToString.call(value) == argsTag);
    }

    /**
     * Checks if `value` is classified as an `Array` object.
     *
     * @static
     * @memberOf _
     * @since 0.1.0
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is an array, else `false`.
     * @example
     *
     * _.isArray([1, 2, 3]);
     * // => true
     *
     * _.isArray(document.body.children);
     * // => false
     *
     * _.isArray('abc');
     * // => false
     *
     * _.isArray(_.noop);
     * // => false
     */
    var isArray = Array.isArray;

    /**
     * Checks if `value` is array-like. A value is considered array-like if it's
     * not a function and has a `value.length` that's an integer greater than or
     * equal to `0` and less than or equal to `Number.MAX_SAFE_INTEGER`.
     *
     * @static
     * @memberOf _
     * @since 4.0.0
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is array-like, else `false`.
     * @example
     *
     * _.isArrayLike([1, 2, 3]);
     * // => true
     *
     * _.isArrayLike(document.body.children);
     * // => true
     *
     * _.isArrayLike('abc');
     * // => true
     *
     * _.isArrayLike(_.noop);
     * // => false
     */
    function isArrayLike(value) {
      return value != null && isLength(value.length) && !isFunction(value);
    }

    /**
     * This method is like `_.isArrayLike` except that it also checks if `value`
     * is an object.
     *
     * @static
     * @memberOf _
     * @since 4.0.0
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is an array-like object,
     *  else `false`.
     * @example
     *
     * _.isArrayLikeObject([1, 2, 3]);
     * // => true
     *
     * _.isArrayLikeObject(document.body.children);
     * // => true
     *
     * _.isArrayLikeObject('abc');
     * // => false
     *
     * _.isArrayLikeObject(_.noop);
     * // => false
     */
    function isArrayLikeObject(value) {
      return isObjectLike(value) && isArrayLike(value);
    }

    /**
     * Checks if `value` is classified as a `Function` object.
     *
     * @static
     * @memberOf _
     * @since 0.1.0
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is a function, else `false`.
     * @example
     *
     * _.isFunction(_);
     * // => true
     *
     * _.isFunction(/abc/);
     * // => false
     */
    function isFunction(value) {
      // The use of `Object#toString` avoids issues with the `typeof` operator
      // in Safari 8-9 which returns 'object' for typed array and other constructors.
      var tag = isObject(value) ? objectToString.call(value) : '';
      return tag == funcTag || tag == genTag;
    }

    /**
     * Checks if `value` is a valid array-like length.
     *
     * **Note:** This method is loosely based on
     * [`ToLength`](http://ecma-international.org/ecma-262/7.0/#sec-tolength).
     *
     * @static
     * @memberOf _
     * @since 4.0.0
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is a valid length, else `false`.
     * @example
     *
     * _.isLength(3);
     * // => true
     *
     * _.isLength(Number.MIN_VALUE);
     * // => false
     *
     * _.isLength(Infinity);
     * // => false
     *
     * _.isLength('3');
     * // => false
     */
    function isLength(value) {
      return typeof value == 'number' &&
        value > -1 && value % 1 == 0 && value <= MAX_SAFE_INTEGER;
    }

    /**
     * Checks if `value` is the
     * [language type](http://www.ecma-international.org/ecma-262/7.0/#sec-ecmascript-language-types)
     * of `Object`. (e.g. arrays, functions, objects, regexes, `new Number(0)`, and `new String('')`)
     *
     * @static
     * @memberOf _
     * @since 0.1.0
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is an object, else `false`.
     * @example
     *
     * _.isObject({});
     * // => true
     *
     * _.isObject([1, 2, 3]);
     * // => true
     *
     * _.isObject(_.noop);
     * // => true
     *
     * _.isObject(null);
     * // => false
     */
    function isObject(value) {
      var type = typeof value;
      return !!value && (type == 'object' || type == 'function');
    }

    /**
     * Checks if `value` is object-like. A value is object-like if it's not `null`
     * and has a `typeof` result of "object".
     *
     * @static
     * @memberOf _
     * @since 4.0.0
     * @category Lang
     * @param {*} value The value to check.
     * @returns {boolean} Returns `true` if `value` is object-like, else `false`.
     * @example
     *
     * _.isObjectLike({});
     * // => true
     *
     * _.isObjectLike([1, 2, 3]);
     * // => true
     *
     * _.isObjectLike(_.noop);
     * // => false
     *
     * _.isObjectLike(null);
     * // => false
     */
    function isObjectLike(value) {
      return !!value && typeof value == 'object';
    }

    /**
     * Assigns own enumerable string keyed properties of source objects to the
     * destination object. Source objects are applied from left to right.
     * Subsequent sources overwrite property assignments of previous sources.
     *
     * **Note:** This method mutates `object` and is loosely based on
     * [`Object.assign`](https://mdn.io/Object/assign).
     *
     * @static
     * @memberOf _
     * @since 0.10.0
     * @category Object
     * @param {Object} object The destination object.
     * @param {...Object} [sources] The source objects.
     * @returns {Object} Returns `object`.
     * @see _.assignIn
     * @example
     *
     * function Foo() {
 *   this.a = 1;
 * }
     *
     * function Bar() {
 *   this.c = 3;
 * }
     *
     * Foo.prototype.b = 2;
     * Bar.prototype.d = 4;
     *
     * _.assign({ 'a': 0 }, new Foo, new Bar);
     * // => { 'a': 1, 'c': 3 }
     */
    var assign = createAssigner(function(object, source) {
      if (nonEnumShadows || isPrototype(source) || isArrayLike(source)) {
        copyObject(source, keys(source), object);
        return;
      }
      for (var key in source) {
        if (hasOwnProperty.call(source, key)) {
          assignValue(object, key, source[ key ]);
        }
      }
    });

    /**
     * Creates an array of the own enumerable property names of `object`.
     *
     * **Note:** Non-object values are coerced to objects. See the
     * [ES spec](http://ecma-international.org/ecma-262/7.0/#sec-object.keys)
     * for more details.
     *
     * @static
     * @since 0.1.0
     * @memberOf _
     * @category Object
     * @param {Object} object The object to query.
     * @returns {Array} Returns the array of property names.
     * @example
     *
     * function Foo() {
 *   this.a = 1;
 *   this.b = 2;
 * }
     *
     * Foo.prototype.c = 3;
     *
     * _.keys(new Foo);
     * // => ['a', 'b'] (iteration order is not guaranteed)
     *
     * _.keys('hi');
     * // => ['0', '1']
     */
    function keys(object) {
      return isArrayLike(object) ? arrayLikeKeys(object) : baseKeys(object);
    }

    module.exports = assign;

    /***/
  })
  /******/ ]);