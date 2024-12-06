package com.unir.reservations.models;

public record Client (
  Integer idClient,
  String document,
  String documentType,
  String firstName,
  String surName,
  String phoneNumber,
  String mobileNumber,
  String email
) {
}
