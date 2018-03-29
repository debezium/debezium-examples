package io.debezium.examples.hikr.rest.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.debezium.examples.hikr.model.Hike;
import io.debezium.examples.hikr.model.Section;

public class ExternalHike {

	private long id;

	@NotNull(message = "Starting point must not be null")
	@Size(min = 3, max = 20, message = "Must be between three and 20 characters long")
	private String from;

	@NotNull(message = "End point must not be null")
	@Size(min = 3, max = 20, message = "Must be between three and 20 characters long")
	private String to;
	private ExternalTrip recommendedTrip;
	private List<Section> sections = new ArrayList<>();

	public ExternalHike() {
	}

	public ExternalHike(Hike hike) {
		this.id = hike.id;
		this.from = hike.start;
		this.to = hike.destination;
		this.recommendedTrip = hike.recommendedTrip != null ? new ExternalTrip( hike.recommendedTrip ) : null;

		for ( Section section : hike.sections ) {
			if ( section != null ) {
				sections.add( section );
			}
		}
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public ExternalTrip getRecommendedTrip() {
		return recommendedTrip;
	}

	public void setRecommendedTrip(ExternalTrip recommendedTrip) {
		this.recommendedTrip = recommendedTrip;
	}

	public List<Section> getSections() {
		return sections;
	}

	public void setSections(List<Section> sections) {
		this.sections = sections;
	}

	@Override
	public String toString() {
		return "ExternalHike [id=" + id + ", from=" + from + ", to=" + to + ", recommendedTrip=" + recommendedTrip + ", sections=" + sections + "]";
	}
}
