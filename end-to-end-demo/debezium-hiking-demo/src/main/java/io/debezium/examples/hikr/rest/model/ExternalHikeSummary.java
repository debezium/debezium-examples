package io.debezium.examples.hikr.rest.model;

import io.debezium.examples.hikr.model.Hike;

public class ExternalHikeSummary {

	private long id;
	private String from;
	private String to;

	public ExternalHikeSummary() {
	}

	public ExternalHikeSummary(Hike hike) {
		this.id = hike.id;
		this.from = hike.start;
		this.to = hike.destination;
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

	@Override
	public String toString() {
		return "ExternalHikeSummary [id=" + id + ", from=" + from + ", to=" + to + "]";
	}
}
