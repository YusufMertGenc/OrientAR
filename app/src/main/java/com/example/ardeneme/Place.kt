package com.example.ardeneme

import com.google.android.gms.maps.model.LatLng

data class Place(
    val name: String,
    val location: LatLng
)

// ODTÜ KKK Binaları
object CampusData {
    val places = listOf(
        Place("Kütüphane", LatLng(35.24926788926027, 33.02415738342566)),
        Place("Deniz Plaza", LatLng(35.24761844424959, 33.02315698353944)),
        Place("Yemekhane", LatLng(35.248266908314214, 33.02364179202564)),
        Place("Rektörlük", LatLng(35.24937812408306, 33.02347179341037)),
        Place("Spor Merkezi", LatLng(35.24684256939173, 33.02698110529848)),
        Place("2. Yurt", LatLng(35.24685518792056, 33.02332729606918)),
        Place("Mühendislik Binası", LatLng(35.24996933354446, 33.02121633032737)),
        Place("1. Yurt", LatLng(35.24721516445099, 33.025035838991116)),
        Place("Hazırlık Binası", LatLng(35.248096081611095, 33.0225580721141))
    )
}