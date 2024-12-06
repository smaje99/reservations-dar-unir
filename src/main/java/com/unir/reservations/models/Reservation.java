package com.unir.reservations.models;

import java.util.ArrayList;
import java.util.Date;

public record Reservation (
  Integer idReservation,
  Client client,
  ArrayList<Service> services,
  Room room,
  Date date,
  Integer startHour,
  Integer endHour,
  Double priceTotal,
  String observations
) {
}
