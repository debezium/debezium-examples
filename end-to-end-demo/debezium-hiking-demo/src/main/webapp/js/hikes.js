angular

    .module('hikesApp', ['ngRoute', 'restangular', 'valdr'])

    .config(function(RestangularProvider) {
        RestangularProvider.setBaseUrl('hiking-manager');
    })

    .config(function($routeProvider) {
        $routeProvider
            .when('/', {
                controller:'HikesCtrl',
                templateUrl:'list.html'
            })
            .when('/new', {
                controller:'DetailCtrl',
                templateUrl:'detail.html'
            })
            .when('/edit/:hikeId', {
                controller:'EditCtrl',
                templateUrl:'detail.html'
            })
            .when('/orders', {
                controller: 'OrdersCtrl',
                templateUrl:'orders-list.html'
            })
            .when('/trips/:tripId', {
                controller: 'TripDetailCtrl',
                templateUrl: 'trips-detail.html'
            })
            .otherwise({
                redirectTo:'/'
            });
    })

    .config(function(valdrProvider) {
        valdrProvider.setConstraintUrl('webresources/validation');
    })

    .factory('PersistenceService', ['Restangular', function(Restangular) {
        var hikesResource = Restangular.all('hikes');
        var tripsResource = Restangular.all('trips');
        var ordersResource = Restangular.all('orders');

        return {
            getHikes: function(searchTerm) {
                if(searchTerm) {
                    return hikesResource.getList({ q: searchTerm });
                }
                else {
                    return hikesResource.getList();
                }
            },
            getHike: function(hike) {
                return Restangular.one('hikes', hike).get();
            },
            getTrips: function() {
                return tripsResource.getList();
            },
            getTrip: function(trip) {
                return Restangular.one('trips', trip ).get();
            },
            createHike: function(hike) {
                return hikesResource.post(hike);
            },
            updateHike: function(hike) {
                return hike.put();
            },
            deleteHike: function(hike) {
                return Restangular.one('hikes', hike.id).remove();
            },
            getOrders: function() {
                return ordersResource.getList();
            },
            createOrder: function(order) {
                return ordersResource.post(order);
            }
        }
    }])

    .controller('HikesCtrl', function($scope, PersistenceService) {
        $scope.getHikes = function() {
            PersistenceService.getHikes($scope.searchTerm).then(function (hikes) {
                $scope.hikes = hikes;
            });
        };

        $scope.remove = function(hike) {
            PersistenceService.deleteHike(hike).then(function (hike) {
                $scope.getHikes();
            });
        };

        $scope.getHikes();
    })

    .controller('DetailCtrl', function($scope,  $location, PersistenceService) {
        PersistenceService.getTrips().then(function (trips) {
            $scope.trips = trips;
        });

        $scope.hike = { sections: [] };

        $scope.save = function() {
            PersistenceService.createHike($scope.hike).then(function (hike) {
                $location.path('/');
            });
        };

        $scope.cancel = function() {
            $location.path('/');
        };
    })

   .controller('TripDetailCtrl', function($scope,  $location, $routeParams, PersistenceService) {
       $scope.trip;
       $scope.order = { tripId: $routeParams.tripId, customer: {} };

       PersistenceService.getTrips().then(function (trips) {
           $scope.trips = trips;
       });

       PersistenceService.getTrip($routeParams.tripId).then(function (trip) {
           $scope.trip = trip;
       });

       $scope.save = function() {
           PersistenceService.createOrder($scope.order).then(function (order) {
               $location.path('/orders');
           });
       };

       $scope.cancel = function() {
           $location.path('/');
       };
   })

    .controller('EditCtrl', function($scope,  $location, $routeParams, PersistenceService) {
        $scope.hike;

        PersistenceService.getTrips().then(function (trips) {
            $scope.trips = trips;
        });

        PersistenceService.getHike($routeParams.hikeId).then(function (hike) {
            $scope.hike = hike;
        });

        $scope.save = function() {
            PersistenceService.updateHike($scope.hike).then(function (hike) {
                $location.path('/');
            });
        };

        $scope.cancel = function() {
            $location.path('/');
        };
    })

    .controller('OrdersCtrl', function($scope, $location, PersistenceService) {
        $scope.getOrders = function() {
            PersistenceService.getOrders().then(function (orders) {
                $scope.orders = orders;
            });
        };
        $scope.new = function() {
            PersistenceService.createOrder().then(function (order) {
                $location.path('/orders');
            });
        };
        $scope.remove = function(hike) {
            PersistenceService.deleteHike(hike).then(function (hike) {
                $scope.getHikes();
            });
        };
        $scope.getOrders();
    })

    .run(function($rootScope) {
        $rootScope.$on('$routeChangeSuccess', function(ev, data) {
            if (data.$$route && data.$$route.controller) {
                $rootScope.controller = data.$$route.controller;
            }
       })
    });
