# fileorganizer
Java program to store file metadata like EXIF to database

Java-ohjelma joka skannaa hakemistoja rekursiivisesti ja tallentaa tiedostojen polun, MD5-tarkistussumman sekä EXIF-tiedot MariaDB-tietokantaan.

## Uudelleenajo

Ohjelma käyttää UPSERT-logiikkaa: jos tiedostopolku löytyy jo kannasta, tietue päivitetään eikä duplikaatteja synny. Vanhentuneet EXIF-tiedot korvataan uusilla.


## EXIF-tuki

EXIF-tietoja luetaan seuraavilta tiedostotyypeiltä:

- **JPEG** / JPG
- **TIFF** / TIF
- **PNG**
- **WebP**, **HEIC**, **HEIF**
- Kameran RAW-formaatit: CR2, NEF, ARW

Kirjastona käytetään [metadata-extractor](https://github.com/drewnoakes/metadata-extractor) v2.19.


