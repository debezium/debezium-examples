package io.debezium.examples.hikr.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import io.debezium.examples.hikr.model.Person;
import io.debezium.examples.hikr.model.Trip;

@ApplicationScoped
public class TripRepository {

	@PersistenceContext(unitName="hike-PU-JTA")
	private EntityManager entityManager;

	public List<Trip> getAllTrips() {
		return entityManager.createQuery( "from Trip", Trip.class ).getResultList();
	}

	public Trip getTripById(long tripId) {
		return entityManager.find( Trip.class, tripId );
	}

	public Trip createTrip(Trip trip) {
		entityManager.persist( trip );
		return trip;
	}
}
