/// 장소 검색 결과 모델 (PLCE 서비스)
class Place {
  const Place({
    required this.placeId,
    required this.name,
    required this.category,
    required this.address,
    required this.lat,
    required this.lng,
    required this.rating,
    required this.reviewCount,
    this.imageUrl,
    this.openNow,
    this.businessHours,
    this.phoneNumber,
    this.priceLevel,
  });

  final String placeId;
  final String name;
  final String category;
  final String address;
  final double lat;
  final double lng;
  final double rating;
  final int reviewCount;
  final String? imageUrl;
  final bool? openNow;
  final List<String>? businessHours;
  final String? phoneNumber;
  final int? priceLevel;

  factory Place.fromJson(Map<String, dynamic> json) {
    return Place(
      placeId: json['place_id'] as String,
      name: json['name'] as String,
      category: json['category'] as String? ?? '',
      address: json['address'] as String? ?? '',
      lat: (json['lat'] as num? ?? 0).toDouble(),
      lng: (json['lng'] as num? ?? 0).toDouble(),
      rating: (json['rating'] as num? ?? 0).toDouble(),
      reviewCount: json['review_count'] as int? ?? 0,
      imageUrl: json['image_url'] as String?,
      openNow: json['open_now'] as bool?,
      businessHours: (json['business_hours'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      phoneNumber: json['phone_number'] as String?,
      priceLevel: json['price_level'] as int?,
    );
  }
}

/// 장소 검색 응답
class PlaceSearchResponse {
  const PlaceSearchResponse({required this.places});

  final List<Place> places;

  factory PlaceSearchResponse.fromJson(Map<String, dynamic> json) {
    final list = json['places'] as List<dynamic>? ?? [];
    return PlaceSearchResponse(
      places: list
          .map((e) => Place.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
