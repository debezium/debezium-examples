package io.debezium.examples.hikr.rest;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.debezium.examples.hikr.model.Hike;
import io.debezium.examples.hikr.model.Person;
import io.debezium.examples.hikr.model.Section;
import io.debezium.examples.hikr.model.Trip;
import io.debezium.examples.hikr.repository.HikeRepository;
import io.debezium.examples.hikr.repository.TripRepository;

@Path("/admin")
@Stateless
public class AdminResource {

	@Inject
	private HikeRepository hikeRepository;

	@Inject
	private TripRepository tripRepository;

	@PersistenceContext(unitName="hike-PU-JTA")
	private EntityManager hikeEm;

	@PersistenceContext//(unitName="business")
	private EntityManager businessEm;

	public AdminResource() {
	}

	@GET
	@Path("/populate")
	public void populate() throws Exception {
		clearDatabase(businessEm);
		clearHikeAndTrips(hikeEm);

		Trip corsica = new Trip();
		corsica.tripName = "Corsica from north to south";
		corsica.price = 2549L;
		corsica.organizer = new Person( "Emmanuel" );
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		corsica.startDate = dateFormat.parse( "2015-04-10" );
		corsica.endDate = dateFormat.parse( "2015-04-17" );
		hikeEm.persist( corsica );
		Hike gr20North = new Hike("Calenzana", "Vizzavona");
		gr20North.recommendedTrip = corsica;
		gr20North.sections.add( new Section( "Calenzana", "Carozzu" ) );
		gr20North.sections.add( new Section( "Carozzu", "Vizzavona" ) );
		Hike gr20South = new Hike("Vizzavona", "Conza");
		gr20South.recommendedTrip = corsica;
		gr20South.sections.add( new Section( "Vizzavona", "Usciulu" ) );
		gr20South.sections.add( new Section( "Usciulu", "Conza" ) );
		hikeEm.persist( gr20North );
		hikeEm.persist( gr20South );


		Trip briceCanyon = new Trip();
		briceCanyon.tripName = "Brice canyon";
		briceCanyon.price = 2549L;
		briceCanyon.organizer = new Person( "Emmanuel" );
		briceCanyon.startDate = dateFormat.parse( "2015-06-13" );
		briceCanyon.endDate = dateFormat.parse( "2015-06-20" );
		hikeEm.persist( briceCanyon );
		Hike fairyLandTrail = new Hike("Fairyland Point", "Fairyland Point");
		fairyLandTrail.recommendedTrip = briceCanyon;
		fairyLandTrail.sections.add( new Section( "Fairyland Point", "Boat Mesa" ) );
		fairyLandTrail.sections.add( new Section( "Boat Mesa", "Tower Bridge" ) );
		fairyLandTrail.sections.add( new Section( "Tower Bridge", "Fairyland Point" ) );
		Hike riggsSpringTrail = new Hike("Rainbow Point", "Yovimpa Pass");
		riggsSpringTrail.recommendedTrip = briceCanyon;
		riggsSpringTrail.sections.add( new Section( "Yovimpa Pass", "The Promontory" ) );
		riggsSpringTrail.sections.add( new Section( "The Promontory", "Yovimpa Pass" ) );
		hikeEm.persist( fairyLandTrail );
		hikeEm.persist( riggsSpringTrail );

		Trip semiMarathon = new Trip();
		semiMarathon.tripName = "Semi-Marathon Paris Versailles";
		semiMarathon.startDate = dateFormat.parse( "2015-09-27" );
		semiMarathon.endDate = dateFormat.parse( "2015-09-27" );
		semiMarathon.organizer = new Person( "Association Paris Versailles" );
		semiMarathon.price = 5;
		hikeEm.persist( semiMarathon );
	}

	private void clearHikeAndTrips(EntityManager em) {
		List<?> all = em.createQuery( "from Hike" ).getResultList();
		for ( Hike object : (List<Hike>) all ) {
			object.recommendedTrip = null;
			em.remove( object );
		}

		all = em.createQuery( "from Trip" ).getResultList();
		for ( Object object : all ) {
			em.remove( object );
		}
	}

	private void clearDatabase(EntityManager em) {
		List<Object> all = em.createQuery( "from java.lang.Object" ).getResultList();
		for ( Object object : all ) {
			em.remove( object );
		}
	}
}
