package com.unir.reservations.models;

public record Room (
  Integer idRoom,
  String name,
  String description,
  String address,
  Double pricePerHour
) {
}
